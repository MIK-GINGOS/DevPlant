// Checks the leaf model and diagnosis rules against synthetic leaves and catalogue photos.
// Run with: npm test
/// <reference types="node" />
import fs from 'fs';
import jpeg from 'jpeg-js';
import { classify } from '../src/gates/leafModel';
import type { Pixels } from '../src/gates/colour';
import { decide, warningFor, severityFor, adviceFor, type Lookup } from '../src/bridge/rules';
import { GUIDE } from '../src/gates/guide';
import { hsv } from '../src/gates/colour';

const byKey = new Map(GUIDE.map(e => [e.key, e]));
const lookup: Lookup = { kindOf: k => byKey.get(k)?.kind ?? null, nameOf: k => byKey.get(k)?.name ?? null };
let failed = 0;
const check = (name: string, ok: boolean, extra = '') => { console.log(`${ok ? 'PASS' : 'FAIL'} ${name} ${extra}`); if (!ok) failed++; };

// Same measure as the store.
function measure(px: Pixels) {
  let g = 0, d = 0; const n = px.size * px.size;
  for (let i = 0; i < n; i++) { const [h, s, v] = hsv(px.rgba[i*4], px.rgba[i*4+1], px.rgba[i*4+2]); if (s < 0.2 || v < 0.12) continue; if (h >= 70 && h <= 170) g++; else if (h >= 10 && h < 70) d++; }
  const p = g + d; return { leafFraction: p / n, damage: p ? d / p : 0 };
}
function square(file: string, size = 128): Pixels {
  const img = jpeg.decode(fs.readFileSync(file), { useTArray: true, formatAsRGBA: true });
  const side = Math.min(img.width, img.height), ox = (img.width - side) >> 1, oy = (img.height - side) >> 1;
  const out = new Uint8Array(size * size * 4);
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
    const sx = ox + Math.floor((x * side) / size), sy = oy + Math.floor((y * side) / size);
    for (let c = 0; c < 4; c++) out[(y*size+x)*4+c] = img.data[(sy*img.width+sx)*4+c];
  }
  return { size, rgba: out };
}
function solid(parts: [number, [number, number, number]][]): Pixels {
  const size = 128, out = new Uint8Array(size*size*4); let i = 0;
  for (const [share, rgb] of parts) { const n = Math.round(share*size*size); for (let k = 0; k < n && i < size*size; k++, i++) { out.set([...rgb, 255], i*4); } }
  for (; i < size*size; i++) out.set([255,255,255,255], i*4);
  return { size, rgba: out };
}
const run = (px: Pixels) => decide(classify(px), measure(px), lookup);

// Synthetic leaves
const green = run(solid([[1, [40, 140, 50]]]));
check('all-green leaf is Healthy', !green.retake && green.kind === 'HEALTHY', JSON.stringify(green));
const brown = run(solid([[0.75, [40, 140, 50]], [0.25, [120, 70, 30]]]));
check('green with brown spots is Leaf spot (disease)', !brown.retake && brown.key === 'leaf_spot' && brown.kind === 'DISEASE', JSON.stringify(brown));
const yellow = run(solid([[0.7, [40, 140, 50]], [0.3, [210, 200, 40]]]));
check('green with yellow is Chlorosis', !yellow.retake && yellow.key === 'chlorosis', JSON.stringify(yellow));
const blank = run(solid([[1, [255, 255, 255]]]));
check('white wall asks for a retake', blank.retake === true, JSON.stringify(blank));

// Rules parity with DiagnosisRulesTest
check('severity thresholds', severityFor(0.05) === 'MILD' && severityFor(0.2) === 'MODERATE' && severityFor(0.5) === 'SEVERE');
check('tentative advice prefix', adviceFor('DISEASE', 'MILD', true).startsWith('Possible match'));
const w3 = warningFor({ kind: 'PEST', name: 'Fall armyworm', severity: 'MILD' }, 3, 'North plot');
check('3 scans in a field raise a WARNING', w3?.level === 'WARNING' && w3.title === 'Fall armyworm spreading in North plot');
const ws = warningFor({ kind: 'DISEASE', name: 'Late blight', severity: 'SEVERE' }, 1, null);
check('severe single scan raises WATCH', ws?.level === 'WATCH' && ws.title === 'Severe late blight in your crops');
check('healthy never warns', warningFor({ kind: 'HEALTHY', name: 'Healthy', severity: 'NONE' }, 9, null) === null);

// Real photos from the catalogue
for (const k of ['spinach', 'basil', 'tomato', 'lemon']) {
  const f = `../cloud/public/images/${k}.jpg`;
  if (!fs.existsSync(f)) continue;
  const v = run(square(f));
  console.log(`INFO ${k}.jpg ->`, v.retake ? `retake: ${v.reason}` : `${v.name} (${v.kind}) ${Math.round(v.confidence*100)}% sure, damage ${Math.round(v.damage*100)}%`);
}
process.exit(failed ? 1 : 0);
