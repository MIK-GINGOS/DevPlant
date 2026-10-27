// Copies the plant catalogue from ../cloud into the app, shaped like /api/catalog, so the
// Catalog page works offline from the first launch. Run after the cloud catalogue changes:
//   npm run sync-catalog
/* global __dirname */
const fs = require('fs');
const path = require('path');

const source = path.join(__dirname, '..', '..', 'cloud', 'gates', 'gm_data', 'catalog.json');
const target = path.join(__dirname, '..', 'src', 'gates', 'catalog.json');

const raw = JSON.parse(fs.readFileSync(source, 'utf8'));
const problems = new Map(raw.diseases.map(d => [d.key, d]));
const plants = raw.plants.map(p => ({
  key: p.key,
  name: p.name,
  scientific_name: p.scientific_name,
  category: p.category,
  image: p.image,
  image_source: p.image_source,
  image_license: p.image_license,
  image_author: p.image_author,
  problems: p.diseases.map(k => {
    const d = problems.get(k);
    return { key: k, name: d.name, signs: d.signs, treatment: d.treatment };
  }),
}));
const categories = [...new Set(plants.map(p => p.category))];
fs.writeFileSync(target, JSON.stringify({ count: plants.length, categories, plants }));
console.log(`Wrote ${plants.length} plants to ${path.relative(process.cwd(), target)}`);
