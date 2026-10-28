// POST  /api/reports       a phone sends report details (header X-Device-Key, body { reports: [...] })
// GET   /api/reports       officers: ?country=&district=&verification=&days=30
// GET   /api/reports/12    officers: one report (rewritten to ?id=12 in vercel.json)
// PATCH /api/reports/12    officers: { verification, note } — the status the farmer sees
import { checkDevice, cleanFilter, cleanReports, cleanStatus } from '../contracts/report.js';
import { handle, idParam, noStore, officerOnly, readBody } from '../app/report_http.js';
import { reportStore } from '../app/report_store.js';

export { OPTIONS } from '../app/report_http.js';

export const POST = handle(async request => {
  const device = checkDevice(request.headers.get('x-device-key'));
  const reports = cleanReports((await readBody(request)).reports);
  return noStore({ accepted: await reportStore().receive(device, reports) });
});

export const GET = officerOnly(async request => {
  const id = idParam(request);
  if (id) {
    const report = await reportStore().report(id);
    return report ? noStore(report) : noStore({ error: 'No such report' }, 404);
  }
  const reports = await reportStore().list(cleanFilter(new URL(request.url).searchParams));
  return noStore({ count: reports.length, reports });
});

export const PATCH = officerOnly(async (request, officer) => {
  const id = idParam(request);
  if (!id) return noStore({ error: 'Which report? Use /api/reports/<id>' }, 400);
  const report = await reportStore().setStatus(id, cleanStatus(await readBody(request)), officer);
  return report ? noStore(report) : noStore({ error: 'No such report' }, 404);
});
