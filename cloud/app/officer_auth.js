// app: officer sign-in. Officers and their access codes are set on Vercel, not in the code:
//   OFFICERS        "Mary Banda=long-code-1; John Phiri=long-code-2"
//   SESSION_SECRET  any long random text; signs the sign-in tokens
// On `npm run dev` with neither set, one test officer exists: "Test officer" / "devplant".
import { createHmac, timingSafeEqual } from 'node:crypto';

const TOKEN_HOURS = 12;
const DEV_SECRET = 'devplant-local-only';

const local = () => !process.env.VERCEL;

function officers() {
  const raw = process.env.OFFICERS;
  if (!raw) return local() ? new Map([['Test officer', 'devplant']]) : new Map();
  return new Map(raw.split(/[;\n]/).map(s => s.split('=')).filter(p => p.length === 2)
    .map(([n, c]) => [n.trim(), c.trim()]).filter(([n, c]) => n && c));
}

function secret() {
  const s = process.env.SESSION_SECRET;
  if (s) return s;
  if (local()) return DEV_SECRET;
  throw new Error('SESSION_SECRET is not set on this deployment');
}

const sign = text => createHmac('sha256', secret()).update(text).digest('base64url');

function same(a, b) {
  const x = Buffer.from(String(a));
  const y = Buffer.from(String(b));
  return x.length === y.length && timingSafeEqual(x, y);
}

/** Whether any officer can sign in on this deployment. */
export const officersConfigured = () => officers().size > 0;

/** A token for the officer, or null if the name and code do not match. */
export function signIn(name, code) {
  const n = String(name || '').trim();
  const expected = [...officers()].find(([k]) => k.toLowerCase() === n.toLowerCase());
  if (!expected || !same(expected[1], String(code || '').trim())) return null;
  const body = Buffer.from(JSON.stringify({ n: expected[0], exp: Date.now() + TOKEN_HOURS * 3_600_000 })).toString('base64url');
  return { token: `${body}.${sign(body)}`, name: expected[0], expiresInHours: TOKEN_HOURS };
}

/** The signed-in officer's name from "Authorization: Bearer <token>", or null. */
export function officerOf(request) {
  const header = request.headers.get('authorization') || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : '';
  const [body, mac] = token.split('.');
  if (!body || !mac || !same(mac, sign(body))) return null;
  try {
    const { n, exp } = JSON.parse(Buffer.from(body, 'base64url').toString());
    // An officer removed from OFFICERS loses access at once, not when the token runs out.
    return exp > Date.now() && officers().has(n) ? n : null;
  } catch {
    return null;
  }
}
