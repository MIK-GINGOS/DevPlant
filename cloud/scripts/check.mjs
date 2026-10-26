// Checks the engine without a server: every api/ entry point answers and every photo exists.
// npm run check
import { existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { GET as catalog } from '../api/catalog.js';
import { GET as plants } from '../api/plants.js';
import { GET as health } from '../api/health.js';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const call = async (fn, path) => {
  const r = await fn(new Request(`https://example.vercel.app${path}`));
  return { status: r.status, body: await r.json() };
};
const fail = msg => { console.error('FAIL', msg); process.exitCode = 1; };

const h = await call(health, '/api/health');
console.log('health', h.status, h.body);

const c = await call(catalog, '/api/catalog');
if (c.body.plants.length !== c.body.count) fail('count mismatch');
for (const p of c.body.plants) {
  if (p.problems.length !== 4) fail(`${p.key} has ${p.problems.length} problems`);
  if (!p.image_url.startsWith('https://example.vercel.app/images/')) fail(`${p.key} image_url ${p.image_url}`);
}
const missing = c.body.plants.filter(p => !existsSync(join(root, 'public', p.image)));
console.log(`catalog ${c.status}: ${c.body.count} plants, ${c.body.categories.length} categories, ${missing.length} photos missing`);
if (missing.length) fail(`missing photos: ${missing.slice(0, 10).map(p => p.key).join(', ')}`);

const one = await call(plants, '/api/plants?key=tomato');
console.log('plant tomato', one.status, one.body.name, one.body.problems.map(p => p.name).join(', '));
const none = await call(plants, '/api/plants?key=nope');
if (none.status !== 404) fail('unknown key should be 404');
const s = await call(plants, '/api/plants?q=blight');
console.log('search "blight"', s.status, s.body.count, 'plants');
