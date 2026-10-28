// gates: reports in one JSON file on this computer, for `npm run dev` and the checks.
// Same rows as postgres.js. Not for Vercel, whose disk is read-only. Imports nothing of ours.
import { mkdirSync, readFileSync, renameSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';

const REPORT_FIELDS = ['createdAt', 'crop', 'country', 'district', 'kind', 'resultKey', 'resultName',
  'confidence', 'uncertain', 'uncertainReason', 'source'];

/** @param {string} path  the JSON file; created on first write */
export function fileRows(path) {
  let doc;
  const load = () => {
    if (doc) return doc;
    try {
      doc = JSON.parse(readFileSync(path, 'utf8'));
    } catch {
      doc = { nextReport: 1, nextQuestion: 1, reports: [], questions: [] };
    }
    return doc;
  };
  const save = () => {
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path + '.tmp', JSON.stringify(doc, null, 1));
    renameSync(path + '.tmp', path);
  };
  const now = () => new Date().toISOString();
  const pub = ({ device, phoneId, ...row }) => row;
  const pubQ = ({ device, phoneId, phoneReportId, ...row }) => row;

  return {
    async upsertReport(device, r) {
      const d = load();
      const old = d.reports.find(x => x.device === device && x.phoneId === r.phoneId);
      if (old) {
        for (const f of REPORT_FIELDS) old[f] = r[f];
      } else {
        d.reports.push({
          id: d.nextReport++, device, phoneId: r.phoneId, receivedAt: now(),
          ...Object.fromEntries(REPORT_FIELDS.map(f => [f, r[f]])),
          verification: 'REPORTED', note: null, updatedBy: null, updatedAt: null,
        });
      }
      save();
    },

    async reportIdFor(device, phoneId) {
      return load().reports.find(x => x.device === device && x.phoneId === phoneId)?.id ?? null;
    },

    async upsertQuestion(device, q, reportId) {
      const d = load();
      const old = d.questions.find(x => x.device === device && x.phoneId === q.phoneId);
      if (old) {
        Object.assign(old, { text: q.text, reportId, crop: q.crop, country: q.country, district: q.district });
      } else {
        d.questions.push({
          id: d.nextQuestion++, device, phoneId: q.phoneId, askedAt: q.askedAt, receivedAt: now(),
          text: q.text, reportId, crop: q.crop, country: q.country, district: q.district,
          answer: null, answeredBy: null, answeredAt: null,
        });
      }
      save();
    },

    async reportsSince(iso) {
      return load().reports.filter(r => r.createdAt >= iso).map(pub)
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
    },

    async deviceReports(device) {
      return load().reports.filter(r => r.device === device)
        .map(r => ({ phoneId: r.phoneId, verification: r.verification, note: r.note, updatedAt: r.updatedAt }));
    },

    async deviceQuestions(device) {
      return load().questions.filter(q => q.device === device)
        .map(q => ({ phoneId: q.phoneId, answer: q.answer, answeredBy: q.answeredBy, answeredAt: q.answeredAt }));
    },

    async report(id) {
      const r = load().reports.find(x => x.id === id);
      return r ? pub(r) : null;
    },

    async updateReport(id, change) {
      const r = load().reports.find(x => x.id === id);
      if (!r) return null;
      Object.assign(r, change);
      save();
      return pub(r);
    },

    async questions(open) {
      return load().questions.filter(q => !open || q.answer == null).map(pubQ)
        .sort((a, b) => b.askedAt.localeCompare(a.askedAt));
    },

    async answerQuestion(id, change) {
      const q = load().questions.find(x => x.id === id);
      if (!q) return null;
      Object.assign(q, change);
      save();
      return pubQ(q);
    },
  };
}
