// GET /api/plants?q=tomato&category=Herbs  search the catalogue
// GET /api/plants/tomato                   one plant (rewritten to ?key=tomato in vercel.json)
import { catalogStore } from '../app/catalog_store.js';
import { baseUrl, json } from '../app/http.js';

export function GET(request) {
  const params = new URL(request.url).searchParams;
  const store = catalogStore(baseUrl(request));
  const key = params.get('key');
  if (key) {
    const plant = store.plant(key);
    return plant ? json(plant, 200, request) : json({ error: `No plant with key "${key}"` }, 404);
  }
  const plants = store.search({ q: params.get('q') || '', category: params.get('category') || '' });
  return json({ count: plants.length, plants }, 200, request);
}
