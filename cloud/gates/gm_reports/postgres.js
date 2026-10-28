// gates: reports in Postgres (Neon, added from the Vercel Marketplace, which sets DATABASE_URL).
// Same rows as file.js. The tables are created on first use. Imports nothing of ours.
import { neon } from '@neondatabase/serverless';

const SCHEMA = [
  `CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    device TEXT NOT NULL,
    phone_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    crop TEXT, country TEXT, district TEXT,
    kind TEXT NOT NULL,
    result_key TEXT, result_name TEXT,
    confidence REAL NOT NULL DEFAULT 0,
    uncertain BOOLEAN NOT NULL DEFAULT false,
    uncertain_reason TEXT,
    source TEXT NOT NULL,
    verification TEXT NOT NULL DEFAULT 'REPORTED',
    note TEXT, updated_by TEXT, updated_at TIMESTAMPTZ,
    UNIQUE (device, phone_id)
  )`,
  'CREATE INDEX IF NOT EXISTS reports_created ON reports (created_at)',
  `CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    device TEXT NOT NULL,
    phone_id BIGINT NOT NULL,
    asked_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    text TEXT NOT NULL,
    report_id BIGINT REFERENCES reports (id) ON DELETE SET NULL,
    crop TEXT, country TEXT, district TEXT,
    answer TEXT, answered_by TEXT, answered_at TIMESTAMPTZ,
    UNIQUE (device, phone_id)
  )`,
];

const REPORT_COLUMNS = `id, created_at AS "createdAt", received_at AS "receivedAt", crop, country, district,
  kind, result_key AS "resultKey", result_name AS "resultName", confidence, uncertain,
  uncertain_reason AS "uncertainReason", source, verification, note, updated_by AS "updatedBy",
  updated_at AS "updatedAt"`;

const QUESTION_COLUMNS = `id, asked_at AS "askedAt", received_at AS "receivedAt", text, report_id AS "reportId",
  crop, country, district, answer, answered_by AS "answeredBy", answered_at AS "answeredAt"`;

const iso = v => (v instanceof Date ? v.toISOString() : v);
/** Postgres returns BIGINT as text and times as Date; the contract wants numbers and ISO text. */
const shape = row => {
  if (!row) return null;
  const out = {};
  for (const [k, v] of Object.entries(row)) out[k] = iso(v);
  for (const k of ['id', 'reportId', 'phoneId']) if (out[k] != null) out[k] = Number(out[k]);
  return out;
};

/** @param {string} url  the DATABASE_URL connection string */
export function postgresRows(url) {
  const sql = neon(url);
  const q = (text, params = []) => sql.query(text, params);
  let ready;
  const schema = () => (ready ??= (async () => { for (const s of SCHEMA) await q(s); })().catch(e => {
    ready = null;
    throw e;
  }));
  const run = async (text, params) => { await schema(); return (await q(text, params)).map(shape); };

  return {
    async upsertReport(device, r) {
      await run(`INSERT INTO reports (device, phone_id, created_at, crop, country, district, kind, result_key,
          result_name, confidence, uncertain, uncertain_reason, source)
        VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)
        ON CONFLICT (device, phone_id) DO UPDATE SET created_at = EXCLUDED.created_at, crop = EXCLUDED.crop,
          country = EXCLUDED.country, district = EXCLUDED.district, kind = EXCLUDED.kind,
          result_key = EXCLUDED.result_key, result_name = EXCLUDED.result_name,
          confidence = EXCLUDED.confidence, uncertain = EXCLUDED.uncertain,
          uncertain_reason = EXCLUDED.uncertain_reason, source = EXCLUDED.source`,
      [device, r.phoneId, r.createdAt, r.crop, r.country, r.district, r.kind, r.resultKey, r.resultName,
        r.confidence, r.uncertain, r.uncertainReason, r.source]);
    },

    async reportIdFor(device, phoneId) {
      const [row] = await run('SELECT id FROM reports WHERE device = $1 AND phone_id = $2', [device, phoneId]);
      return row?.id ?? null;
    },

    async upsertQuestion(device, x, reportId) {
      await run(`INSERT INTO questions (device, phone_id, asked_at, text, report_id, crop, country, district)
        VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
        ON CONFLICT (device, phone_id) DO UPDATE SET text = EXCLUDED.text, report_id = EXCLUDED.report_id,
          crop = EXCLUDED.crop, country = EXCLUDED.country, district = EXCLUDED.district`,
      [device, x.phoneId, x.askedAt, x.text, reportId, x.crop, x.country, x.district]);
    },

    reportsSince: since =>
      run(`SELECT ${REPORT_COLUMNS} FROM reports WHERE created_at >= $1 ORDER BY created_at DESC LIMIT 5000`, [since]),

    deviceReports: device =>
      run(`SELECT phone_id AS "phoneId", verification, note, updated_at AS "updatedAt"
        FROM reports WHERE device = $1`, [device]),

    deviceQuestions: device =>
      run(`SELECT phone_id AS "phoneId", answer, answered_by AS "answeredBy", answered_at AS "answeredAt"
        FROM questions WHERE device = $1`, [device]),

    async report(id) {
      const [row] = await run(`SELECT ${REPORT_COLUMNS} FROM reports WHERE id = $1`, [id]);
      return row ?? null;
    },

    async updateReport(id, c) {
      const [row] = await run(`UPDATE reports SET verification = $2, note = $3, updated_by = $4, updated_at = $5
        WHERE id = $1 RETURNING ${REPORT_COLUMNS}`, [id, c.verification, c.note, c.updatedBy, c.updatedAt]);
      return row ?? null;
    },

    questions: open =>
      run(`SELECT ${QUESTION_COLUMNS} FROM questions ${open ? 'WHERE answer IS NULL' : ''}
        ORDER BY asked_at DESC LIMIT 1000`),

    async answerQuestion(id, c) {
      const [row] = await run(`UPDATE questions SET answer = $2, answered_by = $3, answered_at = $4
        WHERE id = $1 RETURNING ${QUESTION_COLUMNS}`, [id, c.answer, c.answeredBy, c.answeredAt]);
      return row ?? null;
    },
  };
}
