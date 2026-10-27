// bridge: the plant catalogue from the DevPlant cloud (the cloud/ folder, hosted on Vercel).
// A copy ships inside the app, so it works offline from the first launch. On start it looks for
// a newer one online and keeps it. Photos load from the cloud and expo-image keeps them on disk,
// so each one works offline after it has been shown once.

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

const SAVED = 'devplant.catalog.v1';

export const catalogUrl: string = String(
  Constants.expoConfig?.extra?.catalogUrl ?? 'https://devplant-cloud.vercel.app',
).replace(/\/+$/, '');

interface Loaded {
  plants: Plant[];
  categories: string[];
  byKey: Map<string, Plant>;
}

function shape(raw: RawCatalog): Loaded {
  const plants = raw.plants.map<Plant>(p => {
    const author = p.image_author ?? '';
    return {
      key: p.key,
      name: p.name,
      scientificName: p.scientific_name ?? '',
      category: p.category ?? '',
      imageUrl: `${catalogUrl}/${p.image}`,
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

// Saved download from an earlier launch first, then a fresh one if there is signal.
(async () => {
  const saved = await readText(SAVED);
  if (saved) {
    try {
      replace(JSON.parse(saved));
    } catch {
      // unreadable copy: keep the bundled one
    }
  }
  try {
    const res = await fetch(`${catalogUrl}/api/catalog`);
    if (!res.ok) return;
    const json = await res.text();
    const raw = JSON.parse(json) as RawCatalog;
    replace(raw);
    await writeText(SAVED, json);
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
