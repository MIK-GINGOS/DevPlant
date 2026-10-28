// contracts: the plain shapes every layer shares. Imports nothing of ours.

export type Kind = 'HEALTHY' | 'DISEASE' | 'PEST' | 'UNSURE';
export type Severity = 'NONE' | 'MILD' | 'MODERATE' | 'SEVERE';
export type Method = 'CULTURAL' | 'ORGANIC' | 'CHEMICAL';
export type AlertLevel = 'WATCH' | 'WARNING';

export interface Crop {
  id: number;
  name: string;
  scientificName: string;
}

export interface Field {
  id: number;
  name: string;
  cropId: number;
  cropName: string;
  areaHectares: number;
  notes: string | null;
}

export interface Diagnosis {
  kind: Kind;
  key: string;
  name: string;
  confidence: number;
  severity: Severity;
  damage: number;
  alternatives: string | null;
  advice: string;
}

export interface Scan {
  id: number;
  takenAt: number;
  photoUri: string;
  fieldId: number | null;
  fieldName: string | null;
  diagnosis: Diagnosis | null;
}

export type ScanOutcome =
  | { retake: true; reason: string }
  | { retake: false; scan: Scan };

export interface GuideEntry {
  key: string;
  kind: Kind;
  name: string;
  scientificName: string;
  crops: string;
  symptoms: string;
  spread: string;
}

export interface Treatment {
  key: string;
  method: Method;
  title: string;
  detail: string;
}

export interface Alert {
  id: number;
  createdAt: number;
  level: AlertLevel;
  title: string;
  body: string;
  key: string | null;
  fieldId: number | null;
  read: boolean;
}

export interface FieldHealth {
  fieldId: number;
  fieldName: string | null;
  scans: number;
  healthy: number;
  sick: number;
}

export interface HealthSummary {
  scans: number;
  healthy: number;
  diseases: number;
  pests: number;
  unsure: number;
  unreadAlerts: number;
  fields: FieldHealth[];
}

export interface PlantProblem {
  key: string;
  name: string;
  signs: string;
  treatment: string;
}

export interface Plant {
  key: string;
  name: string;
  scientificName: string;
  category: string;
  imageUrl: string;
  /** The same photo from the mirror, used when imageUrl can't be reached. */
  imageFallback: string;
  /** Licence and author, e.g. "CC BY-SA 3.0, Softeis". */
  imageCredit: string;
  imageSource: string;
  problems: PlantProblem[];
}

/** 0..100 share of decided scans that were healthy, or -1 with none. */
export function fieldScore(f: FieldHealth): number {
  const decided = f.healthy + f.sick;
  return decided === 0 ? -1 : Math.round((100 * f.healthy) / decided);
}
