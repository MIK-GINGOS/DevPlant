// bridge: files a report beside every scan, like the Android app's app_reports/ReportingScanStore.
// scanLeaf and deleteScan wrap the ones in store.ts, which stay as they are. A photo the rules
// ask to retake is kept as an uncertain report instead of being thrown away; if the farmer then
// retakes it within a couple of minutes, that try is dropped.
// Shared reports and questions go to the officer service on the same cloud/ site as the
// catalogue (gates/reportSender.ts), and officers' statuses, notes and answers come back.

import { useSyncExternalStore } from 'react';

import type { Consent, Question, Report, ReportSource } from '@/contracts/reports';
import type { ScanOutcome } from '@/contracts/shapes';
import { CROPS } from '@/gates/guide';
import { deletePhoto, keepPhoto } from '@/gates/photos';
import { fetchUpdates, sendQuestions, sendReports, type OfficerUpdates } from '@/gates/reportSender';
import { emptyReports, readReports, writeReports, type ReportsDoc } from '@/gates/reportsStorage';
import { readRows } from '@/gates/storage';

import { catalogUrl } from './catalog';
import { deleteScan as deleteScanOnly, scanLeaf as scanOnly } from './store';

/** Uncertain photos younger than this are treated as tries at the same leaf. */
const RETRY_WINDOW_MS = 2 * 60 * 1000;
/** This app bundles only the colour stand-in (gates/leafModel.ts), so every result is simulated. */
const SOURCE: ReportSource = 'SIMULATED';
/** Most records sent in one request; the server takes up to 50. */
const BATCH = 50;

let doc: ReportsDoc = emptyReports();
let loaded = false;
const listeners = new Set<() => void>();

const loading = readReports().then(d => {
  doc = d;
  loaded = true;
  listeners.forEach(l => l());
  // Whatever waited since last time goes out now, if there is signal.
  syncSoon();
});

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

async function save(change: (d: ReportsDoc) => ReportsDoc) {
  await loading;
  doc = change(doc);
  listeners.forEach(l => l());
  await writeReports(doc);
}

async function cropOf(fieldId: number | null): Promise<string | null> {
  if (fieldId == null) return null;
  const field = (await readRows()).fields.find(f => f.id === fieldId);
  return CROPS.find(c => c.id === field?.cropId)?.name ?? null;
}

function newReport(d: ReportsDoc, base: Pick<Report, 'scanId' | 'createdAt' | 'photoUri' | 'cropName' | 'kind' | 'resultKey' | 'resultName' | 'confidence' | 'uncertainReason'>): Report {
  return {
    ...base,
    id: d.nextId,
    country: d.consent.country,
    district: d.consent.district,
    source: SOURCE,
    verification: 'REPORTED',
    sync: d.consent.share ? 'QUEUED' : 'LOCAL',
  };
}

/** Same as store.scanLeaf, and files the report. */
export async function scanLeaf(photoUri: string, fieldId: number | null): Promise<ScanOutcome> {
  const outcome = await scanOnly(photoUri, fieldId);
  // A report that fails to save must never cost the farmer their result.
  try {
    await loading;
    const now = Date.now();
    const cropName = await cropOf(fieldId);
    const retries = doc.reports.filter(r => r.scanId == null && r.createdAt >= now - RETRY_WINDOW_MS);
    retries.forEach(r => deletePhoto(r.photoUri));
    const kept = outcome.retake ? keepPhoto(photoUri) : null;

    await save(d => {
      const reports = d.reports.filter(r => !retries.some(x => x.id === r.id));
      let report: Report;
      if (outcome.retake) {
        report = newReport(d, {
          scanId: null, createdAt: now, photoUri: kept!, cropName, kind: 'UNSURE',
          resultKey: null, resultName: null, confidence: 0, uncertainReason: outcome.reason,
        });
      } else {
        const s = outcome.scan;
        const dg = s.diagnosis;
        report = newReport(d, {
          scanId: s.id, createdAt: s.takenAt, photoUri: s.photoUri, cropName,
          kind: dg?.kind ?? 'UNSURE', resultKey: dg?.key ?? null, resultName: dg?.name ?? null,
          confidence: dg?.confidence ?? 0, uncertainReason: dg?.kind === 'UNSURE' ? dg.advice : null,
        });
      }
      return { ...d, nextId: d.nextId + 1, reports: [...reports, report] };
    });
    if (doc.consent.share) syncSoon();
  } catch {
    // the scan itself is saved; only the report is missing
  }
  return outcome;
}

/** Same as store.deleteScan, and removes its report. */
export async function deleteScan(id: number) {
  await deleteScanOnly(id);
  await save(d => ({ ...d, reports: d.reports.filter(r => r.scanId !== id) }));
}

/** Removes an uncertain report and its photo. Saved scans are deleted with deleteScan. */
export function deleteReport(id: number) {
  const r = doc.reports.find(x => x.id === id);
  if (!r || r.scanId != null) return;
  deletePhoto(r.photoUri);
  return save(d => ({ ...d, reports: d.reports.filter(x => x.id !== id) }));
}

/** Saves the choice and moves waiting reports between LOCAL and QUEUED to match it. */
export async function saveConsent(c: Omit<Consent, 'decided'>) {
  await save(d => ({
    ...d,
    consent: { ...c, decided: true },
    reports: d.reports.map(r => {
      if (c.share && r.sync === 'LOCAL') return { ...r, sync: 'QUEUED' };
      if (!c.share && r.sync === 'QUEUED') return { ...r, sync: 'LOCAL' };
      return r;
    }),
  }));
  if (c.share) syncSoon();
}

/**
 * Saves a question for an extension officer, about one report or none, and sends it as soon as
 * there is signal. Asking sends that report's details too, even with sharing off, because the
 * farmer chose to; never its photo.
 */
export async function ask(text: string, reportId: number | null): Promise<void> {
  const words = text.trim();
  if (!words) throw new Error('A question needs some text');
  await save(d => {
    const report = reportId == null ? undefined : d.reports.find(r => r.id === reportId);
    const q: Question = {
      id: d.nextQuestionId, reportId: report?.id ?? null, askedAt: Date.now(), text: words,
      cropName: report?.cropName ?? null, answer: null, answeredBy: null, answeredAt: null, sync: 'QUEUED',
    };
    return {
      ...d,
      nextQuestionId: d.nextQuestionId + 1,
      questions: [...d.questions, q],
      reports: d.reports.map(r => (r.id === report?.id && r.sync === 'LOCAL' ? { ...r, sync: 'QUEUED' } : r)),
    };
  });
  syncSoon();
}

// ---- sending to the officer service ----

let syncing: Promise<boolean> | null = null;
let again = false;

/**
 * Sends whatever waits and fetches officers' statuses, notes and answers, in the background.
 * With no signal everything stays queued for the next try (next start, scan, question, or
 * opening My reports).
 */
export function syncSoon(): void {
  if (syncing) {
    again = true;
    return;
  }
  syncing = (async () => {
    let ok: boolean;
    do {
      again = false;
      ok = await syncOnce();
    } while (again && ok);
    return ok;
  })().finally(() => {
    syncing = null;
  });
}

async function deviceKey(): Promise<string> {
  await loading;
  if (doc.deviceKey) return doc.deviceKey;
  let key = 'ios-';
  for (let i = 0; i < 32; i++) key += Math.floor(Math.random() * 16).toString(16);
  await save(d => ({ ...d, deviceKey: d.deviceKey ?? key }));
  return doc.deviceKey!;
}

/** Report details as the server takes them. The photo is never part of it. */
const reportJson = (r: Report) => ({
  id: r.id, createdAt: r.createdAt, cropName: r.cropName, country: r.country, district: r.district,
  kind: r.kind, resultKey: r.resultKey, resultName: r.resultName, confidence: r.confidence,
  uncertain: r.scanId == null, uncertainReason: r.uncertainReason, source: r.source,
});

async function syncOnce(): Promise<boolean> {
  try {
    await loading;
    for (;;) {
      const queued = doc.reports.filter(r => r.sync === 'QUEUED').sort((a, b) => a.createdAt - b.createdAt).slice(0, BATCH);
      if (!queued.length) break;
      const accepted = await sendReports(catalogUrl, await deviceKey(), queued.map(reportJson));
      if (!accepted.length) break;
      await save(d => ({
        ...d,
        reports: d.reports.map(r => (r.sync === 'QUEUED' && accepted.includes(r.id) ? { ...r, sync: 'SENT' } : r)),
      }));
    }
    for (;;) {
      const queued = doc.questions.filter(q => q.sync === 'QUEUED').sort((a, b) => a.askedAt - b.askedAt).slice(0, BATCH);
      if (!queued.length) break;
      const { country, district } = doc.consent;
      const accepted = await sendQuestions(catalogUrl, await deviceKey(), queued.map(q => ({
        id: q.id, askedAt: q.askedAt, text: q.text, reportId: q.reportId, cropName: q.cropName, country, district,
      })));
      if (!accepted.length) break;
      await save(d => ({
        ...d,
        questions: d.questions.map(q => (accepted.includes(q.id) ? { ...q, sync: 'SENT' } : q)),
      }));
    }
    if (doc.reports.some(r => r.sync === 'SENT') || doc.questions.some(q => q.sync === 'SENT')) {
      const updates = await fetchUpdates(catalogUrl, await deviceKey());
      if (changes(doc, updates)) await save(d => applyUpdates(d, updates));
    }
    return true;
  } catch {
    return false; // no signal or the service is down: everything stays queued
  }
}

const VERIFICATIONS: Report['verification'][] = ['REPORTED', 'UNDER_REVIEW', 'FIELD_VERIFIED'];

function applyUpdates(d: ReportsDoc, u: OfficerUpdates): ReportsDoc {
  const reports = new Map(u.reports.map(x => [x.phoneId, x]));
  const answers = new Map(u.questions.filter(x => x.answer).map(x => [x.phoneId, x]));
  return {
    ...d,
    reports: d.reports.map(r => {
      const x = reports.get(r.id);
      if (!x || !VERIFICATIONS.includes(x.verification as Report['verification'])) return r;
      return { ...r, verification: x.verification as Report['verification'], officerNote: x.note ?? null };
    }),
    questions: d.questions.map(q => {
      const x = answers.get(q.id);
      if (!x) return q;
      const at = x.answeredAt ? Date.parse(x.answeredAt) : NaN;
      return { ...q, answer: x.answer, answeredBy: x.answeredBy, answeredAt: Number.isFinite(at) ? at : Date.now() };
    }),
  };
}

/** Whether the officers changed anything this phone does not have yet. */
function changes(d: ReportsDoc, u: OfficerUpdates): boolean {
  const next = applyUpdates(d, u);
  return next.reports.some((r, i) => r.verification !== d.reports[i].verification
    || (r.officerNote ?? null) !== (d.reports[i].officerNote ?? null))
    || next.questions.some((q, i) => q.answer !== d.questions[i].answer);
}

// ---- hooks for the screens ----

const useDoc = () => useSyncExternalStore(subscribe, () => doc);

export const useReportsLoaded = () => useSyncExternalStore(subscribe, () => loaded);
export const useConsent = () => useDoc().consent;
/** Newest first. */
export const useReports = () => [...useDoc().reports].sort((a, b) => b.createdAt - a.createdAt);
export const useReportForScan = (scanId: number) => useDoc().reports.find(r => r.scanId === scanId) ?? null;
export const useReport = (id: number | null) => useDoc().reports.find(r => r.id === id) ?? null;
/** Newest first. */
export const useQuestions = () => [...useDoc().questions].sort((a, b) => b.askedAt - a.askedAt);

// ---- words for the screens ----

export const sourceLabel = (s: ReportSource) => (s === 'AI_MODEL' ? 'AI result' : s === 'EXPERT' ? 'Expert checked' : 'Simulated');

export const sourceSentence = (s: ReportSource) =>
  s === 'AI_MODEL' ? 'This result comes from the AI model on this phone.'
    : s === 'EXPERT' ? 'An expert has checked this result.'
      : 'This result comes from a simple colour check, not a trained AI model.';

export const verificationLabel = (v: Report['verification']) =>
  v === 'UNDER_REVIEW' ? 'Under review' : v === 'FIELD_VERIFIED' ? 'Field verified' : 'Reported';

export const syncLabel = (s: Report['sync']) => (s === 'QUEUED' ? 'Waiting to send' : s === 'SENT' ? 'Sent' : 'On this phone');

export function area(country: string | null, district: string | null): string | null {
  if (!country && !district) return null;
  if (!district) return country;
  if (!country) return district;
  return `${district}, ${country}`;
}
