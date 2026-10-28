// app: the bridge for reports. Picks the gate (Postgres on Vercel, a local file for npm run dev)
// and implements the ReportStore contract on top of it, including the officer's overview.
import { createHash } from 'node:crypto';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

import { checkReportStore } from '../contracts/report.js';
import { fileRows } from '../gates/gm_reports/file.js';

/** Reports of one threat in one district within this many days make a possible hotspot. */
export const HOTSPOT_DAYS = 14;
export const HOTSPOT_MIN = 3;

const DAY_MS = 86_400_000;
const LOCAL_FILE = join(fileURLToPath(new URL('..', import.meta.url)), '.data', 'reports.json');

/** Thrown when no database is set up on Vercel; the api/ files answer it with 503. */
export class NoDatabase extends Error {}

let rowsPromise;
async function rows() {
  return (rowsPromise ??= (async () => {
    const url = process.env.DATABASE_URL || process.env.POSTGRES_URL;
    if (url) {
      // Loaded only here, so npm run dev works without installing anything.
      const { postgresRows } = await import('../gates/gm_reports/postgres.js');
      return postgresRows(url);
    }
    if (process.env.VERCEL) {
      throw new NoDatabase('No report database yet: add Neon Postgres to this Vercel project (it sets DATABASE_URL).');
    }
    return fileRows(process.env.REPORTS_FILE || LOCAL_FILE);
  })().catch(e => {
    rowsPromise = null;
    throw e;
  }));
}

/** Phones' keys are stored only as a hash, so the database alone cannot pose as a phone. */
const deviceHash = key => createHash('sha256').update(key).digest('hex');

const since = days => new Date(Date.now() - days * DAY_MS).toISOString();
const isThreat = r => !r.uncertain && r.kind !== 'HEALTHY' && r.kind !== 'UNSURE' && r.resultKey;
const areaOf = r => [r.district, r.country].filter(Boolean).join(', ') || 'Area not given';

function count(items, keyOf) {
  const m = new Map();
  for (const x of items) {
    const k = keyOf(x);
    if (k != null) m.set(k, (m.get(k) || 0) + 1);
  }
  return [...m].map(([name, n]) => ({ name, count: n })).sort((a, b) => b.count - a.count);
}

const median = xs => {
  if (!xs.length) return null;
  const s = [...xs].sort((a, b) => a - b);
  return s[Math.floor(s.length / 2)];
};

/**
 * Same threat, same district, at least HOTSPOT_MIN reports in HOTSPOT_DAYS days. Only reports
 * that name a district count: a country alone is too wide to send anyone to.
 * Always called a possible hotspot: report volume is not how much of the crop is affected.
 */
function hotspots(recent) {
  const cut = since(HOTSPOT_DAYS);
  const groups = new Map();
  for (const r of recent) {
    if (r.createdAt < cut || !isThreat(r) || !r.district) continue;
    const key = [r.country || '', r.district.toLowerCase(), r.resultKey].join('|');
    const g = groups.get(key) || { area: areaOf(r), country: r.country, district: r.district, threat: r.resultName || r.resultKey, reports: 0, verified: 0, last: r.createdAt };
    g.reports++;
    if (r.verification === 'FIELD_VERIFIED') g.verified++;
    if (r.createdAt > g.last) g.last = r.createdAt;
    groups.set(key, g);
  }
  return [...groups.values()].filter(g => g.reports >= HOTSPOT_MIN).sort((a, b) => b.reports - a.reports);
}

function matches(r, f) {
  if (f.country && (r.country || '').toLowerCase() !== f.country.toLowerCase()) return false;
  if (f.district && (r.district || '').toLowerCase() !== f.district.toLowerCase()) return false;
  if (f.verification && r.verification !== f.verification) return false;
  return true;
}

/** @returns {import('../contracts/report.js').ReportStore} */
export function reportStore() {
  return checkReportStore({
    async receive(device, reports) {
      const db = await rows();
      const d = deviceHash(device);
      for (const r of reports) await db.upsertReport(d, r);
      return reports.map(r => r.phoneId);
    },

    async ask(device, questions) {
      const db = await rows();
      const d = deviceHash(device);
      for (const q of questions) {
        const reportId = q.phoneReportId == null ? null : await db.reportIdFor(d, q.phoneReportId);
        await db.upsertQuestion(d, q, reportId);
      }
      return questions.map(q => q.phoneId);
    },

    async updatesFor(device) {
      const db = await rows();
      const d = deviceHash(device);
      return { reports: await db.deviceReports(d), questions: await db.deviceQuestions(d) };
    },

    async list(filter = {}) {
      const db = await rows();
      return (await db.reportsSince(since(filter.days || 30))).filter(r => matches(r, filter));
    },

    async report(id) {
      return (await rows()).report(id);
    },

    async setStatus(id, change, officer) {
      return (await rows()).updateReport(id, {
        verification: change.verification, note: change.note ?? null,
        updatedBy: officer, updatedAt: new Date().toISOString(),
      });
    },

    async summary(filter = {}) {
      const db = await rows();
      const days = filter.days || 7;
      const all = (await db.reportsSince(since(Math.max(days, HOTSPOT_DAYS)))).filter(r => matches(r, filter));
      const cut = since(days);
      const recent = all.filter(r => r.createdAt >= cut);
      const delays = recent.map(r => (Date.parse(r.receivedAt) - Date.parse(r.createdAt)) / 3_600_000).filter(h => h >= 0);
      return {
        days,
        reports: recent.length,
        needsReview: recent.filter(r => r.verification === 'REPORTED').length,
        underReview: recent.filter(r => r.verification === 'UNDER_REVIEW').length,
        fieldVerified: recent.filter(r => r.verification === 'FIELD_VERIFIED').length,
        uncertain: recent.filter(r => r.uncertain || r.kind === 'UNSURE').length,
        byThreat: count(recent.filter(isThreat), r => r.resultName || r.resultKey).slice(0, 8),
        byArea: count(recent, areaOf).slice(0, 8),
        hotspots: hotspots(all),
        hotspotRule: { days: HOTSPOT_DAYS, min: HOTSPOT_MIN },
        medianSyncDelayHours: median(delays),
        openQuestions: (await db.questions(true)).length,
      };
    },

    async questions(filter = {}) {
      return (await rows()).questions(filter.open === true);
    },

    async answer(id, answer, officer) {
      return (await rows()).answerQuestion(id, { answer, answeredBy: officer, answeredAt: new Date().toISOString() });
    },
  });
}
