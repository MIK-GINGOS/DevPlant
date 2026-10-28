// app: small helpers the api/ entry points share.
import { createHash } from 'node:crypto';

/** "https://<host>" of the deployment that received the request. */
export function baseUrl(request) {
  const url = new URL(request.url);
  const host = request.headers.get('x-forwarded-host') || request.headers.get('host') || url.host;
  const proto = request.headers.get('x-forwarded-proto') || url.protocol.replace(':', '');
  return `${proto}://${host}`;
}

/**
 * JSON response; catalogue data changes only on deploy, so the CDN may keep it for an hour.
 * It carries an ETag, so the apps can ask "still the same?" and get an empty 304 instead of the
 * whole catalogue again.
 */
export function json(data, status = 200, request = null) {
  const body = JSON.stringify(data);
  const headers = {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': status === 200 ? 'public, max-age=300, s-maxage=3600' : 'no-store',
    'Access-Control-Allow-Origin': '*',
  };
  if (status === 200) {
    const etag = `"${createHash('sha1').update(body).digest('hex').slice(0, 20)}"`;
    headers.ETag = etag;
    if (request?.headers.get('if-none-match') === etag) return new Response(null, { status: 304, headers });
  }
  return new Response(body, { status, headers });
}
