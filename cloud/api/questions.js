// POST  /api/questions       a phone asks (header X-Device-Key, body { questions: [...] })
// GET   /api/questions       officers: ?open=1 for unanswered only
// PATCH /api/questions/7     officers: { answer } (rewritten to ?id=7 in vercel.json)
import { checkDevice, cleanAnswer, cleanQuestions } from '../contracts/report.js';
import { handle, idParam, noStore, officerOnly, readBody } from '../app/report_http.js';
import { reportStore } from '../app/report_store.js';

export { OPTIONS } from '../app/report_http.js';

export const POST = handle(async request => {
  const device = checkDevice(request.headers.get('x-device-key'));
  const questions = cleanQuestions((await readBody(request)).questions);
  return noStore({ accepted: await reportStore().ask(device, questions) });
});

export const GET = officerOnly(async request => {
  const open = new URL(request.url).searchParams.get('open') === '1';
  const questions = await reportStore().questions({ open });
  return noStore({ count: questions.length, questions });
});

export const PATCH = officerOnly(async (request, officer) => {
  const id = idParam(request);
  if (!id) return noStore({ error: 'Which question? Use /api/questions/<id>' }, 400);
  const question = await reportStore().answer(id, cleanAnswer(await readBody(request)), officer);
  return question ? noStore(question) : noStore({ error: 'No such question' }, 404);
});
