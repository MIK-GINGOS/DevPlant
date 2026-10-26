// contracts: the abstraction layer. Plain shapes and the store every screen talks to.
// Imports nothing of ours.

/**
 * One common problem of a plant.
 * @typedef {object} Problem
 * @property {string} key        e.g. "late_blight"
 * @property {string} name       e.g. "Late blight"
 * @property {string} signs      what the farmer sees
 * @property {string} treatment  what to do
 */

/**
 * One plant in the catalogue.
 * @typedef {object} Plant
 * @property {number} id
 * @property {string} key              matches the label the scan model outputs, e.g. "tomato"
 * @property {string} name
 * @property {string} scientific_name
 * @property {string} category
 * @property {string} image            path under the site root, e.g. "images/tomato.jpg"
 * @property {string} image_url        absolute URL of the photo
 * @property {string} image_source     where the photo comes from (Wikimedia Commons page)
 * @property {string} image_license
 * @property {string} image_author
 * @property {Problem[]} problems      always 4
 */

/**
 * What the api/ entry points need. The app/ folder decides what sits behind it.
 * @typedef {object} CatalogStore
 * @property {() => {count: number, categories: string[], plants: Plant[]}} all
 * @property {(query?: {q?: string, category?: string}) => Plant[]} search
 * @property {(key: string) => Plant | null} plant
 * @property {() => string[]} categories
 */

export const STORE_METHODS = ['all', 'search', 'plant', 'categories'];

/** Throws if a store is missing part of the contract. */
export function checkStore(store) {
  for (const m of STORE_METHODS) {
    if (typeof store[m] !== 'function') throw new Error(`CatalogStore is missing ${m}()`);
  }
  return store;
}
