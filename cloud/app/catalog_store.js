// app: the bridge. Implements the contracts with the gates. The only folder that knows both.
import { checkStore } from '../contracts/catalog.js';
import { readCatalog } from '../gates/gm_data/read.js';

/**
 * Builds the CatalogStore for one request.
 * @param {string} baseUrl  e.g. "https://devplant.vercel.app", used to make absolute photo URLs
 * @returns {import('../contracts/catalog.js').CatalogStore}
 */
export function catalogStore(baseUrl) {
  const raw = readCatalog();
  const problems = new Map(raw.diseases.map(d => [d.key, d]));
  const categories = [...new Set(raw.plants.map(p => p.category))];

  const shape = p => ({
    id: p.id,
    key: p.key,
    name: p.name,
    scientific_name: p.scientific_name,
    category: p.category,
    image: p.image,
    image_url: `${baseUrl}/${p.image}`,
    image_source: p.image_source,
    image_license: p.image_license,
    image_author: p.image_author,
    problems: p.diseases.map(k => {
      const d = problems.get(k);
      return { key: k, name: d.name, signs: d.signs, treatment: d.treatment };
    }),
  });

  const plants = raw.plants.map(shape);
  const byKey = new Map(plants.map(p => [p.key, p]));

  return checkStore({
    all: () => ({ count: plants.length, categories, plants }),
    categories: () => categories,
    plant: key => byKey.get(String(key || '').toLowerCase()) || null,
    search: ({ q = '', category = '' } = {}) => {
      const words = q.toLowerCase().split(/\s+/).filter(Boolean);
      return plants.filter(p => {
        if (category && p.category !== category) return false;
        if (!words.length) return true;
        const text = [p.name, p.scientific_name, p.category, ...p.problems.map(x => x.name)]
          .join(' ').toLowerCase();
        return words.every(w => text.includes(w));
      });
    },
  });
}
