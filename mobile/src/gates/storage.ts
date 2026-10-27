// gates: the saved rows. A small JSON document in the phone's app storage, standing in for the
// Android app's Room database.

import AsyncStorage from '@react-native-async-storage/async-storage';

import type { AlertLevel, Diagnosis } from '@/contracts/shapes';

export interface FieldRow {
  id: number;
  name: string;
  cropId: number;
  areaHectares: number;
  notes: string | null;
}

export interface ScanRow {
  id: number;
  takenAt: number;
  photoUri: string;
  fieldId: number | null;
  diagnosis: Diagnosis;
}

export interface AlertRow {
  id: number;
  createdAt: number;
  level: AlertLevel;
  title: string;
  body: string;
  key: string | null;
  fieldId: number | null;
  read: boolean;
}

export interface Rows {
  nextId: number;
  fields: FieldRow[];
  scans: ScanRow[];
  alerts: AlertRow[];
}

const KEY = 'devplant.rows.v1';

export const emptyRows = (): Rows => ({ nextId: 1, fields: [], scans: [], alerts: [] });

export async function readRows(): Promise<Rows> {
  try {
    const json = await AsyncStorage.getItem(KEY);
    return json ? { ...emptyRows(), ...JSON.parse(json) } : emptyRows();
  } catch {
    return emptyRows();
  }
}

export async function writeRows(rows: Rows): Promise<void> {
  await AsyncStorage.setItem(KEY, JSON.stringify(rows));
}

export async function readText(key: string): Promise<string | null> {
  try {
    return await AsyncStorage.getItem(key);
  } catch {
    return null;
  }
}

export async function writeText(key: string, value: string): Promise<void> {
  await AsyncStorage.setItem(key, value);
}
