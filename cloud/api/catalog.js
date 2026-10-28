// GET /api/catalog: the whole catalogue in one response. The phone app downloads this once and
// keeps a copy, so the catalogue works offline afterwards.
import { catalogStore } from '../app/catalog_store.js';
import { baseUrl, json } from '../app/http.js';

export function GET(request) {
  return json(catalogStore(baseUrl(request)).all(), 200, request);
}
