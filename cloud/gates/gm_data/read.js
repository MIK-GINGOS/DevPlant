// gates: the tools. The catalogue data file shipped with the deployment. Imports nothing of ours.
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);

/** The raw catalogue: { version, count, diseases: [...], plants: [...] }. */
export function readCatalog() {
  return require('./catalog.json');
}
