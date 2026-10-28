// POST /api/officer   sign in: { name, code } -> { token, name }
// GET  /api/officer   the overview for the dashboard (signed in): ?days=7&country=&district=
import { cleanFilter } from '../contracts/report.js';
import { officersConfigured, signIn } from '../app/officer_auth.js';
import { handle, noStore, officerOnly, readBody } from '../app/report_http.js';
import { reportStore } from '../app/report_store.js';

export { OPTIONS } from '../app/report_http.js';

export const POST = handle(async request => {
  if (!officersConfigured()) {
    return noStore({ error: 'No officers are set up on this site yet (OFFICERS on Vercel).' }, 503);
  }
  const { name, code } = await readBody(request);
  const session = signIn(name, code);
  if (!session) {
    await new Promise(r => setTimeout(r, 600)); // slows down guessing
    return noStore({ error: 'Name or access code is wrong' }, 401);
  }
  return noStore(session);
});

export const GET = officerOnly(async (request, officer) => {
  const params = new URL(request.url).searchParams;
  const filter = cleanFilter(params);
  if (!params.get('days')) filter.days = 7;
  return noStore({ officer, ...(await reportStore().summary(filter)) });
});
