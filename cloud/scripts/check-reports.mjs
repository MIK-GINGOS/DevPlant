// npm run check:reports — the whole farmer-to-officer path against the api/ files, using a
// throwaway report file (never the real database). Exits 1 on the first failure.
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const dir = mkdtempSync(join(tmpdir(), 'devplant-reports-'));
process.env.REPORTS_FILE = join(dir, 'reports.json');
delete process.env.DATABASE_URL;
delete process.env.POSTGRES_URL;
delete process.env.VERCEL;
delete process.env.OFFICERS;

const reports = await import('../api/reports.js');
const questions = await import('../api/questions.js');
const updates = await import('../api/updates.js');
const officer = await import('../api/officer.js');

const BASE = 'http://localhost';
const PHONE = 'phone-aaaaaaaaaaaaaaaaaaaaaaaa';
const OTHER = 'phone-bbbbbbbbbbbbbbbbbbbbbbbb';
let failures = 0;

function req(path, { method = 'GET', body, device, token } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (device) headers['X-Device-Key'] = device;
  if (token) headers.Authorization = `Bearer ${token}`;
  const url = new URL(path, BASE);
  const byId = url.pathname.match(/^\/api\/(reports|questions)\/(\d+)$/);
  if (byId) { url.pathname = `/api/${byId[1]}`; url.searchParams.set('id', byId[2]); }
  return new Request(url, { method, headers, body: body == null ? undefined : JSON.stringify(body) });
}

async function call(handler, request) {
  const res = await handler(request);
  return { status: res.status, body: await res.json(), cache: res.headers.get('cache-control') };
}

function expect(what, ok, detail = '') {
  console.log(`${ok ? 'ok  ' : 'FAIL'} ${what}${ok ? '' : ' ' + detail}`);
  if (!ok) failures++;
}

const now = Date.now();
const report = (id, extra = {}) => ({
  id, createdAt: now - id * 60_000, cropName: 'Maize', country: 'Zambia', district: 'Chipata',
  kind: 'DISEASE', resultKey: 'maize_streak_virus', resultName: 'Maize streak virus', confidence: 0.71,
  source: 'SIMULATED', photoPath: '/private/should/not/be/stored.jpg', ...extra,
});

try {
  // phones send
  let r = await call(reports.POST, req('/api/reports', { method: 'POST', device: PHONE, body: { reports: [report(1), report(2), report(3, { uncertain: true, kind: 'UNSURE', resultKey: null, resultName: null })] } }));
  expect('phone sends 3 reports', r.status === 200 && r.body.accepted.join() === '1,2,3', JSON.stringify(r.body));
  expect('report replies are never cached', r.cache === 'no-store', r.cache);
  r = await call(reports.POST, req('/api/reports', { method: 'POST', device: OTHER, body: { reports: [report(1, { district: 'chipata' })] } }));
  expect('second phone, same district (other spelling)', r.status === 200);
  r = await call(reports.POST, req('/api/reports', { method: 'POST', device: PHONE, body: { reports: [report(1, { confidence: 0.8 })] } }));
  expect('sending the same report again updates it', r.status === 200);
  r = await call(reports.POST, req('/api/reports', { method: 'POST', body: { reports: [report(9)] } }));
  expect('no device key is refused', r.status === 400, JSON.stringify(r.body));
  r = await call(reports.POST, req('/api/reports', { method: 'POST', device: PHONE, body: { reports: [] } }));
  expect('empty list is refused', r.status === 400);
  r = await call(reports.POST, req('/api/reports', { method: 'POST', device: PHONE, body: { reports: [report(-1)] } }));
  expect('bad id is refused', r.status === 400);

  // a question about report 2
  r = await call(questions.POST, req('/api/questions', { method: 'POST', device: PHONE, body: { questions: [{ id: 1, askedAt: now, text: 'Yellow streaks on young leaves. What should I do?', reportId: 2, cropName: 'Maize', country: 'Zambia', district: 'Chipata' }] } }));
  expect('phone asks a question', r.status === 200 && r.body.accepted[0] === 1, JSON.stringify(r.body));
  r = await call(questions.POST, req('/api/questions', { method: 'POST', device: PHONE, body: { questions: [{ id: 2, text: '   ' }] } }));
  expect('empty question is refused', r.status === 400);

  // officers
  r = await call(reports.GET, req('/api/reports'));
  expect('report list needs sign-in', r.status === 401);
  r = await call(officer.POST, req('/api/officer', { method: 'POST', body: { name: 'Test officer', code: 'wrong' } }));
  expect('wrong code is refused', r.status === 401);
  r = await call(officer.POST, req('/api/officer', { method: 'POST', body: { name: 'test officer', code: 'devplant' } }));
  expect('test officer signs in', r.status === 200 && r.body.token && r.body.name === 'Test officer', JSON.stringify(r.body));
  const token = r.body.token;
  r = await call(reports.GET, req('/api/reports', { token: token.slice(0, -2) + 'xx' }));
  expect('tampered token is refused', r.status === 401);

  r = await call(officer.GET, req('/api/officer', { token }));
  const s = r.body;
  expect('overview counts 4 reports', s.reports === 4, JSON.stringify(s));
  expect('overview: 4 need review, 1 uncertain', s.needsReview === 4 && s.uncertain === 1);
  expect('possible hotspot: 3 MSV reports in Chipata', s.hotspots.length === 1 && s.hotspots[0].reports === 3, JSON.stringify(s.hotspots));
  expect('one open question', s.openQuestions === 1);

  r = await call(reports.GET, req('/api/reports?district=Chipata&days=30', { token }));
  expect('list filters by district', r.body.count === 4, JSON.stringify(r.body).slice(0, 200));
  expect('no device key or photo path in the list', !JSON.stringify(r.body).includes('phone-') && !JSON.stringify(r.body).includes('photo'));
  const first = r.body.reports.find(x => x.confidence === 0.8);
  expect('resent report was updated, not doubled', !!first);

  r = await call(reports.PATCH, req(`/api/reports/${first.id}`, { method: 'PATCH', token, body: { verification: 'CONFIRMED' } }));
  expect('unknown status is refused', r.status === 400);
  r = await call(reports.PATCH, req(`/api/reports/${first.id}`, { method: 'PATCH', token, body: { verification: 'FIELD_VERIFIED', note: 'Seen in the field. Pull out and burn infected plants.' } }));
  expect('officer verifies a report', r.status === 200 && r.body.verification === 'FIELD_VERIFIED' && r.body.updatedBy === 'Test officer', JSON.stringify(r.body));
  r = await call(reports.GET, req('/api/reports/999', { token }));
  expect('missing report is 404', r.status === 404);

  r = await call(questions.GET, req('/api/questions?open=1', { token }));
  const q = r.body.questions[0];
  expect('question is linked to its report', q && q.reportId != null, JSON.stringify(r.body));
  r = await call(questions.PATCH, req(`/api/questions/${q.id}`, { method: 'PATCH', token, body: { answer: 'Remove infected plants and control leafhoppers.' } }));
  expect('officer answers', r.status === 200 && r.body.answeredBy === 'Test officer');

  // the farmer's phone hears back, the other phone does not
  r = await call(updates.GET, req('/api/updates', { device: PHONE }));
  const mine = r.body.reports.find(x => x.phoneId === 1);
  expect('phone gets the new status and note', mine?.verification === 'FIELD_VERIFIED' && mine.note?.startsWith('Seen'), JSON.stringify(r.body));
  expect('phone gets the answer', r.body.questions[0]?.answer?.startsWith('Remove'));
  r = await call(updates.GET, req('/api/updates', { device: OTHER }));
  expect('other phone sees only its own report', r.body.reports.length === 1 && r.body.reports[0].verification === 'REPORTED' && r.body.questions.length === 0, JSON.stringify(r.body));
} catch (e) {
  console.error(e);
  failures++;
} finally {
  rmSync(dir, { recursive: true, force: true });
}

console.log(failures ? `\n${failures} failed` : '\nall report checks passed');
process.exit(failures ? 1 : 0);
