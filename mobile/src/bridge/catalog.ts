// bridge: the plant catalogue from the DevPlant cloud (the cloud/ folder, hosted on Vercel).
// A copy ships inside the app, so it works offline from the first launch. On start it looks for
// a newer one online and keeps it. Photos load from the cloud and expo-image keeps them on disk,
// so each one works offline after it has been shown once. Some networks can't reach Vercel, so
// the catalogue and photos also come from the GitHub copy on jsDelivr (catalogMirror).

import Constants from 'expo-constants';
import { useSyncExternalStore } from 'react';

import type { Plant } from '@/contracts/shapes';
import { readText, writeText } from '@/gates/storage';

interface RawPlant {
  key: string;
  name: string;
  scientific_name?: string;
  category?: string;
  image: string;
  image_url?: string;
  image_source?: string;
  image_license?: string;
  image_author?: string;
  problems: { key: string; name: string; signs: string; treatment: string }[];
}

interface RawCatalog {
  categories: string[];
  plants: RawPlant[];
}

/** The data file as stored in the repo (cloud/gates/gm_data/catalog.json), read from the mirror. */
interface FileCatalog {
  diseases: { key: string; name: string; signs: string; treatment: string }[];
  plants: (Omit<RawPlant, 'problems'> & { diseases: string[] })[];
}

const SAVED = 'devplant.catalog.v1';
/** Where the saved catalogue came from and its ETag, so an unchanged one isn't downloaded again. */
const VERSION = 'devplant.catalog.version';

export const catalogUrl: string = String(
  Constants.expoConfig?.extra?.catalogUrl ?? 'https://devplant-cloud.vercel.app',
).replace(/\/+$/, '');

const catalogMirror: string = String(
  Constants.expoConfig?.extra?.catalogMirror ?? 'https://cdn.jsdelivr.net/gh/MIK-GINGOS/DevPlant@main/cloud',
).replace(/\/+$/, '');

interface Loaded {
  plants: Plant[];
  categories: string[];
  byKey: Map<string, Plant>;
}

// Set when the cloud could not be reached this launch: photos then load from the mirror first.
let cloudDown = false;

function shape(raw: RawCatalog): Loaded {
  const plants = raw.plants.map<Plant>(p => {
    const author = p.image_author ?? '';
    return {
      key: p.key,
      name: p.name,
      scientificName: p.scientific_name ?? '',
      category: p.category ?? '',
      imageUrl: cloudDown ? `${catalogMirror}/public/${p.image}` : `${catalogUrl}/${p.image}`,
      imageFallback: cloudDown ? `${catalogUrl}/${p.image}` : `${catalogMirror}/public/${p.image}`,
      imageCredit: (p.image_license ?? '') + (author ? `, ${author}` : ''),
      imageSource: p.image_source ?? '',
      problems: p.problems,
    };
  });
  return { plants, categories: raw.categories, byKey: new Map(plants.map(p => [p.key, p])) };
}

let current: Loaded = shape(require('@/gates/catalog.json') as RawCatalog);
const listeners = new Set<() => void>();

function replace(raw: RawCatalog) {
  if (!Array.isArray(raw?.plants) || raw.plants.length === 0) return;
  current = shape(raw);
  listeners.forEach(l => l());
}

interface Fetched {
  url: string;
  etag: string;
  /** Null when the server still has the version we saved (304 Not Modified). */
  text: string | null;
}

async function fetchText(url: string, ms: number, saved: { url: string; etag: string } | null): Promise<Fetched> {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), ms);
  const etag = saved?.url === url ? saved.etag : '';
  try {
    const res = await fetch(url, { signal: ctrl.signal, headers: etag ? { 'If-None-Match': etag } : {} });
    if (res.status === 304 && etag) return { url, etag, text: null };
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return { url, etag: res.headers.get('etag') ?? '', text: await res.text() };
  } finally {
    clearTimeout(timer);
  }
}

/** The newest catalogue as JSON text in API shape: from the cloud, else from the mirror. */
async function download(saved: { url: string; etag: string } | null): Promise<Fetched> {
  try {
    return await fetchText(`${catalogUrl}/api/catalog`, 10000, saved);
  } catch {
    cloudDown = true;
    const got = await fetchText(`${catalogMirror}/gates/gm_data/catalog.json`, 20000, saved);
    if (got.text === null) return got;
    const file = JSON.parse(got.text) as FileCatalog;
    const problems = new Map(file.diseases.map(d => [d.key, d]));
    const plants: RawPlant[] = file.plants.map(({ diseases, ...p }) => ({
      ...p,
      problems: diseases.flatMap(k => {
        const d = problems.get(k);
        return d ? [d] : [];
      }),
    }));
    const categories = [...new Set(plants.map(p => p.category ?? ''))];
    return { ...got, text: JSON.stringify({ categories, plants } satisfies RawCatalog) };
  }
}

// Saved download from an earlier launch first, then a fresh one if there is signal.
(async () => {
  const saved = await readText(SAVED);
  let version: { url: string; etag: string } | null = null;
  if (saved) {
    try {
      replace(JSON.parse(saved));
      version = JSON.parse((await readText(VERSION)) ?? 'null');
    } catch {
      // unreadable copy: keep the bundled one
    }
  }
  try {
    const got = await download(version);
    if (got.text === null) {
      // Unchanged, but if it came from the mirror, photos should still prefer the mirror.
      if (cloudDown && saved) replace(JSON.parse(saved));
      return;
    }
    replace(JSON.parse(got.text) as RawCatalog);
    await writeText(SAVED, got.text);
    await writeText(VERSION, JSON.stringify({ url: got.url, etag: got.etag }));
  } catch {
    // no signal: the saved or bundled copy stays
  }
})();

function useLoaded(): Loaded {
  return useSyncExternalStore(
    l => {
      listeners.add(l);
      return () => listeners.delete(l);
    },
    () => current,
  );
}

export function useCatalog() {
  const c = useLoaded();
  return { plants: c.plants, categories: c.categories };
}

export function usePlant(key: string): Plant | null {
  return useLoaded().byKey.get(String(key).toLowerCase()) ?? null;
}

export function searchPlants(plants: Plant[], query: string, category: string | null): Plant[] {
  const words = query.toLowerCase().split(/\s+/).filter(Boolean);
  return plants.filter(p => {
    if (category && p.category !== category) return false;
    if (!words.length) return true;
    const text = [p.name, p.scientificName, p.category, ...p.problems.map(x => x.name)].join(' ').toLowerCase();
    return words.every(w => text.includes(w));
  });
}
