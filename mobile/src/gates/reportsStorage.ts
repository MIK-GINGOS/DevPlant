// gates: the saved reports and sharing choice. Their own JSON document, so the scan rows in
// storage.ts stay as they are (the Android app keeps them in devplant_reports.db).

import AsyncStorage from '@react-native-async-storage/async-storage';

import type { Consent, Question, Report } from '@/contracts/reports';

export interface ReportsDoc {
  nextId: number;
  reports: Report[];
  consent: Consent;
  nextQuestionId: number;
  questions: Question[];
  /** Random; tells the officer service which reports are this phone's. Made on first send. */
  deviceKey: string | null;
}

const KEY = 'devplant.reports.v1';

export const emptyReports = (): ReportsDoc => ({
  nextId: 1,
  reports: [],
  consent: { decided: false, share: false, country: null, district: null },
  nextQuestionId: 1,
  questions: [],
  deviceKey: null,
});

export async function readReports(): Promise<ReportsDoc> {
  try {
    const json = await AsyncStorage.getItem(KEY);
    return json ? { ...emptyReports(), ...JSON.parse(json) } : emptyReports();
  } catch {
    return emptyReports();
  }
}

export async function writeReports(doc: ReportsDoc): Promise<void> {
  await AsyncStorage.setItem(KEY, JSON.stringify(doc));
}
