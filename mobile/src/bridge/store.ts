// bridge: the app's data, built from the gates. One scan goes leaf -> pixels -> model -> rules ->
// saved rows, the same path as the Android app's RoomScanStore. Screens read through the hooks
// at the bottom and re-render when anything is saved.

import { useSyncExternalStore } from 'react';

import type {
  Alert,
  Crop,
  Field,
  FieldHealth,
  GuideEntry,
  HealthSummary,
  Scan,
  ScanOutcome,
  Treatment,
} from '@/contracts/shapes';
import { CROPS, GUIDE, TREATMENTS } from '@/gates/guide';
import { classify, MODEL_INPUT } from '@/gates/leafModel';
import { hsv, type Pixels } from '@/gates/colour';
import { squarePixels } from '@/gates/leafPixels';
import { deletePhoto, keepPhoto } from '@/gates/photos';
import { emptyRows, readRows, writeRows, type Rows, type ScanRow } from '@/gates/storage';

import { decide, isProblem, OUTBREAK_WINDOW_MS, warningFor, type LeafStats, type Lookup } from './rules';

const ALERT_REPEAT_MS = 7 * 24 * 60 * 60 * 1000;

// ---- saved rows, shared by every screen ----

let rows: Rows = emptyRows();
let loaded = false;
const listeners = new Set<() => void>();

const loading = readRows().then(r => {
  rows = r;
  loaded = true;
  listeners.forEach(l => l());
});

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

async function save(change: (r: Rows) => Rows) {
  await loading;
  rows = change(rows);
  listeners.forEach(l => l());
  await writeRows(rows);
}

// ---- the guide (bundled, read-only) ----

const guideByKey = new Map(GUIDE.map(e => [e.key, e]));

const lookup: Lookup = {
  kindOf: key => guideByKey.get(key)?.kind ?? null,
  nameOf: key => guideByKey.get(key)?.name ?? null,
};

export const crops: Crop[] = CROPS;
export const guide: GuideEntry[] = GUIDE;

export function guideEntry(key: string): GuideEntry | null {
  return guideByKey.get(key) ?? null;
}

export function treatmentsFor(key: string): Treatment[] {
  return TREATMENTS.filter(t => t.key === key);
}

export function searchGuide(query: string): GuideEntry[] {
  const q = query.trim().toLowerCase();
  if (!q) return [];
  return GUIDE.filter(e =>
    [e.name, e.scientificName, e.crops, e.symptoms].join(' ').toLowerCase().includes(q),
  );
}

export function searchCrops(query: string): Crop[] {
  const q = query.trim().toLowerCase();
  if (!q) return [];
  return CROPS.filter(c => (c.name + ' ' + c.scientificName).toLowerCase().includes(q));
}

// ---- shaping rows for the screens ----

function fieldOf(r: Rows, id: number | null) {
  return id == null ? undefined : r.fields.find(f => f.id === id);
}

function toScan(r: Rows, s: ScanRow): Scan {
  return {
    id: s.id,
    takenAt: s.takenAt,
    photoUri: s.photoUri,
    fieldId: s.fieldId,
    fieldName: fieldOf(r, s.fieldId)?.name ?? null,
    diagnosis: s.diagnosis,
  };
}

function fields(r: Rows): Field[] {
  return r.fields.map(f => ({
    ...f,
    cropName: CROPS.find(c => c.id === f.cropId)?.name ?? 'Unknown crop',
  }));
}

function recent(r: Rows): Scan[] {
  return [...r.scans].sort((a, b) => b.takenAt - a.takenAt).map(s => toScan(r, s));
}

function alerts(r: Rows): Alert[] {
  return [...r.alerts].sort((a, b) => b.createdAt - a.createdAt);
}

function health(r: Rows, days: number): HealthSummary {
  const since = Date.now() - days * 24 * 60 * 60 * 1000;
  const sum: HealthSummary = { scans: 0, healthy: 0, diseases: 0, pests: 0, unsure: 0, unreadAlerts: 0, fields: [] };
  const byField = new Map<number, FieldHealth>();
  for (const s of r.scans) {
    if (s.takenAt < since) continue;
    const id = fieldOf(r, s.fieldId) ? s.fieldId! : 0;
    let f = byField.get(id);
    if (!f) {
      f = { fieldId: id, fieldName: id === 0 ? null : fieldOf(r, id)!.name, scans: 0, healthy: 0, sick: 0 };
      byField.set(id, f);
    }
    sum.scans++;
    f.scans++;
    const k = s.diagnosis.kind;
    if (k === 'HEALTHY') { sum.healthy++; f.healthy++; }
    else if (k === 'DISEASE') { sum.diseases++; f.sick++; }
    else if (k === 'PEST') { sum.pests++; f.sick++; }
    else sum.unsure++;
  }
  sum.fields = [...byField.values()];
  for (const f of r.fields) {
    if (!byField.has(f.id)) sum.fields.push({ fieldId: f.id, fieldName: f.name, scans: 0, healthy: 0, sick: 0 });
  }
  sum.unreadAlerts = r.alerts.filter(a => !a.read).length;
  return sum;
}

// ---- one scan, from leaf to diagnosis ----

/** Measures how much of the picture is leaf and how much of the leaf looks damaged. */
function measure(px: Pixels): LeafStats {
  let green = 0, damaged = 0;
  const count = px.size * px.size;
  for (let i = 0; i < count; i++) {
    const [h, s, v] = hsv(px.rgba[i * 4], px.rgba[i * 4 + 1], px.rgba[i * 4 + 2]);
    if (s < 0.2 || v < 0.12) continue;
    if (h >= 70 && h <= 170) green++;
    else if (h >= 10 && h < 70) damaged++;
  }
  const plant = green + damaged;
  return { leafFraction: plant / count, damage: plant === 0 ? 0 : damaged / plant };
}

export async function scanLeaf(photoUri: string, fieldId: number | null): Promise<ScanOutcome> {
  await loading;
  const px = await squarePixels(photoUri, MODEL_INPUT);
  const verdict = decide(classify(px), measure(px), lookup);
  if (verdict.retake) return { retake: true, reason: verdict.reason };

  const kept = keepPhoto(photoUri);
  const now = Date.now();
  const field = fieldOf(rows, fieldId);
  const fid = field ? field.id : null;
  const { retake: _, ...diagnosis } = verdict;
  let scanId = 0;
  try {
    await save(r => {
      scanId = r.nextId;
      const scan: ScanRow = { id: scanId, takenAt: now, photoUri: kept, fieldId: fid, diagnosis };
      let next: Rows = { ...r, nextId: r.nextId + 1, scans: [...r.scans, scan] };

      if (isProblem(verdict.kind)) {
        const recentInField = next.scans.filter(
          s => s.diagnosis.key === verdict.key && s.fieldId === fid && s.takenAt >= now - OUTBREAK_WINDOW_MS,
        ).length;
        const warning = warningFor(verdict, recentInField, field?.name ?? null);
        const repeated = next.alerts.some(
          a => a.key === verdict.key && a.fieldId === fid && a.createdAt >= now - ALERT_REPEAT_MS,
        );
        if (warning && !repeated) {
          next = {
            ...next,
            nextId: next.nextId + 1,
            alerts: [...next.alerts, { id: next.nextId, createdAt: now, ...warning, key: verdict.key, fieldId: fid, read: false }],
          };
        }
      }
      return next;
    });
  } catch (e) {
    deletePhoto(kept);
    throw e;
  }
  return { retake: false, scan: toScan(rows, rows.scans.find(s => s.id === scanId)!) };
}

// ---- writes ----

export function deleteScan(id: number) {
  const row = rows.scans.find(s => s.id === id);
  if (!row) return;
  deletePhoto(row.photoUri);
  return save(r => ({ ...r, scans: r.scans.filter(s => s.id !== id) }));
}

export function addField(name: string, cropId: number, areaHectares: number, notes: string | null) {
  return save(r => ({
    ...r,
    nextId: r.nextId + 1,
    fields: [...r.fields, { id: r.nextId, name, cropId, areaHectares, notes }],
  }));
}

/** Its scans are kept but no longer tied to a field. Its alerts are removed. */
export function deleteField(id: number) {
  return save(r => ({
    ...r,
    fields: r.fields.filter(f => f.id !== id),
    scans: r.scans.map(s => (s.fieldId === id ? { ...s, fieldId: null } : s)),
    alerts: r.alerts.filter(a => a.fieldId !== id),
  }));
}

export function markAlertRead(id: number) {
  return save(r => ({ ...r, alerts: r.alerts.map(a => (a.id === id ? { ...a, read: true } : a)) }));
}

export function markAllAlertsRead() {
  return save(r => ({ ...r, alerts: r.alerts.map(a => ({ ...a, read: true })) }));
}

// ---- hooks for the screens ----

function useRows(): Rows {
  return useSyncExternalStore(subscribe, () => rows);
}

export function useLoaded(): boolean {
  return useSyncExternalStore(subscribe, () => loaded);
}

export const useFields = () => fields(useRows());
export const useRecentScans = () => recent(useRows());
export const useAlerts = () => alerts(useRows());
export const useUnreadCount = () => useRows().alerts.filter(a => !a.read).length;
/** Health over the last `days` days. */
export const useHealth = (days: number) => health(useRows(), days);

export function useScan(id: number): Scan | null {
  const r = useRows();
  const row = r.scans.find(s => s.id === id);
  return row ? toScan(r, row) : null;
}
