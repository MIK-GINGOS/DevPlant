// app: helpers for the report addresses. Unlike the catalogue, nothing here may be cached.
import { Invalid } from '../contracts/report.js';

import { officerOf } from './officer_auth.js';
import { NoDatabase } from './report_store.js';

const MAX_BODY = 256 * 1024;

export function noStore(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'Access-Control-Allow-Origin': '*',
    },
  });
}

export async function readBody(request) {
  const text = await request.text();
  if (text.length > MAX_BODY) throw new Invalid('Request too large');
  try {
    return JSON.parse(text || '{}');
  } catch {
    throw new Invalid('Body must be JSON');
  }
}

/** The id from /api/reports/12 (rewritten to ?id=12), or null. */
export function idParam(request) {
  const v = Number(new URL(request.url).searchParams.get('id'));
  return Number.isSafeInteger(v) && v > 0 ? v : null;
}

/** Answers 400/503/500 for the errors the store and the checks throw. */
export function handle(fn) {
  return async request => {
    try {
      return await fn(request);
    } catch (e) {
      if (e instanceof Invalid) return noStore({ error: e.message }, 400);
      if (e instanceof NoDatabase) return noStore({ error: e.message }, 503);
      console.error(e);
      return noStore({ error: 'Something went wrong on the server' }, 500);
    }
  };
}

/** Like handle, for officer-only addresses: 401 without a valid sign-in. */
export function officerOnly(fn) {
  return handle(async request => {
    const officer = officerOf(request);
    if (!officer) return noStore({ error: 'Sign in again' }, 401);
    return fn(request, officer);
  });
}

/** Phones answer CORS preflight-free requests; this covers browsers testing the API. */
export function OPTIONS() {
  return new Response(null, {
    status: 204,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PATCH, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Device-Key',
    },
  });
}
