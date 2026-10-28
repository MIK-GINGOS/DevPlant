// GET /api/updates   a phone asks what officers did with its reports and questions
//                    (header X-Device-Key). Returns { reports: [{phoneId, verification, note}],
//                    questions: [{phoneId, answer, answeredBy, answeredAt}] }.
import { checkDevice } from '../contracts/report.js';
import { handle, noStore } from '../app/report_http.js';
import { reportStore } from '../app/report_store.js';

export { OPTIONS } from '../app/report_http.js';

export const GET = handle(async request => {
  const device = checkDevice(request.headers.get('x-device-key'));
  return noStore(await reportStore().updatesFor(device));
});
