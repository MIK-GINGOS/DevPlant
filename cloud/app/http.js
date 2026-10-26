// app: small helpers the api/ entry points share.

/** "https://<host>" of the deployment that received the request. */
export function baseUrl(request) {
  const url = new URL(request.url);
  const host = request.headers.get('x-forwarded-host') || request.headers.get('host') || url.host;
  const proto = request.headers.get('x-forwarded-proto') || url.protocol.replace(':', '');
  return `${proto}://${host}`;
}

/** JSON response; catalogue data changes only on deploy, so the CDN may keep it for an hour. */
export function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': status === 200 ? 'public, max-age=300, s-maxage=3600' : 'no-store',
      'Access-Control-Allow-Origin': '*',
    },
  });
}
