// Copies the catalogue built in ../plant_catalog into this site:
//   catalog.json -> gates/gm_data/catalog.json   (read by the API)
//   images/*.jpg -> public/images/               (served by Vercel's CDN)
// Run after the catalogue changes: npm run import
import { copyFileSync, mkdirSync, readdirSync, existsSync, readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(dirname(fileURLToPath(import.meta.url)));
const from = join(here, '..', 'plant_catalog');
if (!existsSync(join(from, 'catalog.json'))) throw new Error(`No catalogue at ${from}`);

const catalog = JSON.parse(readFileSync(join(from, 'catalog.json'), 'utf8'));
mkdirSync(join(here, 'gates', 'gm_data'), { recursive: true });
copyFileSync(join(from, 'catalog.json'), join(here, 'gates', 'gm_data', 'catalog.json'));

const out = join(here, 'public', 'images');
mkdirSync(out, { recursive: true });
let copied = 0;
for (const f of readdirSync(join(from, 'images'))) {
  if (!f.endsWith('.jpg')) continue;
  copyFileSync(join(from, 'images', f), join(out, f));
  copied++;
}
const missing = catalog.plants.filter(p => !existsSync(join(here, 'public', p.image))).map(p => p.key);
console.log(`catalog: ${catalog.plants.length} plants, images copied: ${copied}, missing: ${missing.length}`);
if (missing.length) console.log('missing photos:', missing.slice(0, 20).join(', '), missing.length > 20 ? '…' : '');
