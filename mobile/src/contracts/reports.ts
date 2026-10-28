// contracts: reports kept beside the scans (from CropGuard), after the Android app's
// contracts/Report.java and Consent.java. Imports nothing of ours outside contracts.

import type { Kind } from './shapes';

/** Who produced the result. */
export type ReportSource = 'SIMULATED' | 'AI_MODEL' | 'EXPERT';
/** Kept apart on purpose: a report is not a confirmed case until someone checks the field. */
export type Verification = 'REPORTED' | 'UNDER_REVIEW' | 'FIELD_VERIFIED';
/** LOCAL stays on the phone; QUEUED waits for a connection; SENT has reached the officers. */
export type SyncState = 'LOCAL' | 'QUEUED' | 'SENT';

export interface Report {
  id: number;
  /** The saved scan, or null for an uncertain photo the rules asked to retake. */
  scanId: number | null;
  createdAt: number;
  photoUri: string;
  cropName: string | null;
  country: string | null;
  district: string | null;
  kind: Kind;
  resultKey: string | null;
  resultName: string | null;
  confidence: number;
  /** Why the result is uncertain, or null. */
  uncertainReason: string | null;
  source: ReportSource;
  verification: Verification;
  sync: SyncState;
  /** What the extension officer wrote back, or null. */
  officerNote?: string | null;
}

/** A farmer's question to an extension officer, maybe about one of their reports. */
export interface Question {
  id: number;
  /** The report it is about, or null. */
  reportId: number | null;
  askedAt: number;
  text: string;
  cropName: string | null;
  /** The officer's answer, or null while there is none. */
  answer: string | null;
  answeredBy: string | null;
  answeredAt: number | null;
  /** QUEUED until it has reached the officers. Never LOCAL: asking is sharing. */
  sync: Exclude<SyncState, 'LOCAL'>;
}

/** What the farmer agreed to share, set on the welcome screen. Photos are never part of it. */
export interface Consent {
  /** False until the farmer has answered the welcome screen once. */
  decided: boolean;
  /** Send report details (no photos) when the phone is online. */
  share: boolean;
  country: string | null;
  district: string | null;
}
