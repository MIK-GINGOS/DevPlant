// GET /api/health: quick check that the deployment and its data are there.
import { catalogStore } from '../app/catalog_store.js';
import { baseUrl, json } from '../app/http.js';

export function GET(request) {
  const { count, categories } = catalogStore(baseUrl(request)).all();
  return json({ ok: true, plants: count, categories: categories.length });
}
