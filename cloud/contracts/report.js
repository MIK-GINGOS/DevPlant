// contracts: reports sent by phones and read by extension officers (from the CropGuard design).
// Plain shapes, the checks every incoming record goes through, and the ReportStore contract.
// Imports nothing of ours.

/** Who produced the result on the phone. */
export const SOURCES = ['SIMULATED', 'AI_MODEL', 'EXPERT'];
/** Kept apart on purpose: a report is not a confirmed case until an officer checks the field. */
export const VERIFICATIONS = ['REPORTED', 'UNDER_REVIEW', 'FIELD_VERIFIED'];
export const KINDS = ['HEALTHY', 'DISEASE', 'PEST', 'DEFICIENCY', 'UNSURE'];

/** Most records one phone may send in one request. */
export const MAX_BATCH = 50;

/**
 * One report as an officer sees it. Never a photo, never a GPS point: the area is what the
 * farmer chose on the welcome screen.
 * @typedef {object} Report
 * @property {number} id
 * @property {string} createdAt        ISO time the scan was made on the phone
 * @property {string} receivedAt       ISO time it reached the cloud (sync delay = receivedAt - createdAt)
 * @property {string|null} crop
 * @property {string|null} country
 * @property {string|null} district
 * @property {string} kind             one of KINDS
 * @property {string|null} resultKey  e.g. "maize_streak_virus"
 * @property {string|null} resultName
 * @property {number} confidence       0..1
 * @property {boolean} uncertain       a photo DevPlant could not read
 * @property {string|null} uncertainReason
 * @property {string} source           one of SOURCES
 * @property {string} verification     one of VERIFICATIONS
 * @property {string|null} note        the officer's note to the farmer
 * @property {string|null} updatedBy  the officer who last set the status
 * @property {string|null} updatedAt
 */

/**
 * A farmer's question to an extension officer, maybe about one of their reports.
 * @typedef {object} Question
 * @property {number} id
 * @property {string} askedAt
 * @property {string} text
 * @property {number|null} reportId    the cloud report it is about
 * @property {string|null} crop
 * @property {string|null} country
 * @property {string|null} district
 * @property {string|null} answer
 * @property {string|null} answeredBy
 * @property {string|null} answeredAt
 */

/**
 * What the api/ entry points need. Every method returns a promise.
 * @typedef {object} ReportStore
 * @property {(device: string, reports: object[]) => Promise<number[]>} receive    phone ids stored
 * @property {(device: string, questions: object[]) => Promise<number[]>} ask      phone ids stored
 * @property {(device: string) => Promise<object>} updatesFor   statuses and answers for one phone
 * @property {(filter?: object) => Promise<Report[]>} list
 * @property {(id: number) => Promise<Report|null>} report
 * @property {(id: number, change: {verification: string, note?: string}, officer: string) => Promise<Report|null>} setStatus
 * @property {(filter?: object) => Promise<object>} summary
 * @property {(filter?: {open?: boolean}) => Promise<Question[]>} questions
 * @property {(id: number, answer: string, officer: string) => Promise<Question|null>} answer
 */

export const STORE_METHODS = ['receive', 'ask', 'updatesFor', 'list', 'report', 'setStatus', 'summary', 'questions', 'answer'];

/** Throws if a store is missing part of the contract. */
export function checkReportStore(store) {
  for (const m of STORE_METHODS) {
    if (typeof store[m] !== 'function') throw new Error(`ReportStore is missing ${m}()`);
  }
  return store;
}

/** Thrown for a request the phone or officer must fix; the api/ files answer it with 400. */
export class Invalid extends Error {}

const text = (v, max) => {
  if (v == null || v === '') return null;
  if (typeof v !== 'string') throw new Invalid('Expected text');
  const t = v.trim();
  return t ? t.slice(0, max) : null;
};

const id = v => {
  if (!Number.isSafeInteger(v) || v <= 0) throw new Invalid('Expected a positive whole number id');
  return v;
};

const time = v => {
  const t = typeof v === 'number' ? v : Date.parse(v);
  // Phones with a wrong clock still send; anything absurd is clamped to "now".
  if (!Number.isFinite(t) || t < Date.UTC(2024, 0, 1) || t > Date.now() + 86_400_000) return new Date().toISOString();
  return new Date(t).toISOString();
};

const oneOf = (v, allowed, fallback) => (allowed.includes(v) ? v : fallback);

/** The random key a phone identifies itself with. It is never shown to officers. */
export function checkDevice(key) {
  if (typeof key !== 'string' || !/^[A-Za-z0-9-]{16,64}$/.test(key)) throw new Invalid('Missing or malformed X-Device-Key');
  return key;
}

function list(items, what) {
  if (!Array.isArray(items) || items.length === 0) throw new Invalid(`Expected a non-empty "${what}" list`);
  if (items.length > MAX_BATCH) throw new Invalid(`At most ${MAX_BATCH} ${what} per request`);
  return items;
}

/** A report as a phone sends it, checked and trimmed. Unknown fields are dropped. */
export function cleanReport(r) {
  if (!r || typeof r !== 'object') throw new Invalid('Expected a report object');
  const confidence = Number(r.confidence);
  return {
    phoneId: id(r.id),
    createdAt: time(r.createdAt),
    crop: text(r.cropName, 80),
    country: text(r.country, 60),
    district: text(r.district, 80),
    kind: oneOf(r.kind, KINDS, 'UNSURE'),
    resultKey: text(r.resultKey, 80),
    resultName: text(r.resultName, 120),
    confidence: Number.isFinite(confidence) ? Math.min(1, Math.max(0, confidence)) : 0,
    uncertain: r.uncertain === true,
    uncertainReason: text(r.uncertainReason, 300),
    source: oneOf(r.source, SOURCES, 'SIMULATED'),
  };
}

export const cleanReports = items => list(items, 'reports').map(cleanReport);

/** A question as a phone sends it. reportId is the phone's own report id, if any. */
export function cleanQuestion(q) {
  if (!q || typeof q !== 'object') throw new Invalid('Expected a question object');
  const words = text(q.text, 1000);
  if (!words) throw new Invalid('A question needs some text');
  return {
    phoneId: id(q.id),
    askedAt: time(q.askedAt),
    text: words,
    phoneReportId: q.reportId == null ? null : id(q.reportId),
    crop: text(q.cropName, 80),
    country: text(q.country, 60),
    district: text(q.district, 80),
  };
}

export const cleanQuestions = items => list(items, 'questions').map(cleanQuestion);

/** An officer's status change. */
export function cleanStatus(body) {
  if (!VERIFICATIONS.includes(body?.verification)) throw new Invalid(`verification must be one of ${VERIFICATIONS.join(', ')}`);
  return { verification: body.verification, note: text(body.note, 1000) };
}

export function cleanAnswer(body) {
  const answer = text(body?.answer, 2000);
  if (!answer) throw new Invalid('An answer needs some text');
  return answer;
}

/** Filters for the officer's report list, from the query string. */
export function cleanFilter(params) {
  const days = Number(params.get('days'));
  return {
    country: text(params.get('country'), 60),
    district: text(params.get('district'), 80),
    verification: oneOf(params.get('verification'), VERIFICATIONS, null),
    days: Number.isFinite(days) && days > 0 ? Math.min(days, 366) : 30,
  };
}
