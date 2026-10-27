// bridge: picks pest or disease, severity and treatment advice from the model's top labels.
// Same rules and thresholds as the Android app's app_rules/DiagnosisRules.java.

import type { AlertLevel, Kind, Severity } from '@/contracts/shapes';
import type { Label } from '@/gates/leafModel';

export interface LeafStats {
  /** 0..1, share of the picture that is plant material. */
  leafFraction: number;
  /** 0..1, share of the plant material that is yellow or brown. */
  damage: number;
}

export interface Lookup {
  kindOf(key: string): Kind | null;
  nameOf(key: string): string | null;
}

export type Verdict =
  | { retake: true; reason: string }
  | {
      retake: false;
      kind: Kind;
      key: string;
      name: string;
      confidence: number;
      severity: Severity;
      damage: number;
      alternatives: string | null;
      advice: string;
    };

export interface Warning {
  level: AlertLevel;
  title: string;
  body: string;
}

export const HEALTHY = 'healthy';
/** Below this the model is guessing: ask for another photo. */
export const RETAKE_BELOW = 0.4;
/** Below this the answer is shown as a possible match. */
export const TENTATIVE_BELOW = 0.6;
/** Less leaf than this in the picture means the photo missed the leaf. */
export const MIN_LEAF = 0.15;
/** Same problem this many times in one field within the window raises a warning. */
export const OUTBREAK_COUNT = 3;
export const OUTBREAK_WINDOW_MS = 14 * 24 * 60 * 60 * 1000;

export function decide(labels: Label[], stats: LeafStats, lookup: Lookup): Verdict {
  if (stats.leafFraction < MIN_LEAF) {
    return { retake: true, reason: 'No leaf found. Fill the frame with one leaf, in daylight.' };
  }
  if (labels.length === 0 || labels[0].confidence < RETAKE_BELOW) {
    return { retake: true, reason: 'Not sure what this is. Move closer, hold steady and avoid shadows.' };
  }
  const top = labels[0];
  const kind: Kind = top.name === HEALTHY ? 'HEALTHY' : (lookup.kindOf(top.name) ?? 'UNSURE');
  const name = kind === 'HEALTHY' ? 'Healthy' : (lookup.nameOf(top.name) ?? pretty(top.name));
  const severity: Severity = kind === 'HEALTHY' ? 'NONE' : severityFor(stats.damage);
  return {
    retake: false,
    kind,
    key: top.name,
    name,
    confidence: top.confidence,
    severity,
    damage: stats.damage,
    alternatives: alternatives(labels, lookup),
    advice: adviceFor(kind, severity, top.confidence < TENTATIVE_BELOW),
  };
}

export function severityFor(damage: number): Severity {
  if (damage < 0.1) return 'MILD';
  if (damage < 0.3) return 'MODERATE';
  return 'SEVERE';
}

export function isProblem(kind: Kind): boolean {
  return kind === 'DISEASE' || kind === 'PEST';
}

/**
 * @param recentInField scans of this problem in this field within the outbreak window, including this one
 * @returns null when no warning is needed
 */
export function warningFor(
  v: { kind: Kind; name: string; severity: Severity },
  recentInField: number,
  fieldName: string | null,
): Warning | null {
  if (!isProblem(v.kind)) return null;
  const where = fieldName ?? 'your crops';
  if (recentInField >= OUTBREAK_COUNT) {
    return {
      level: 'WARNING',
      title: `${v.name} spreading in ${where}`,
      body: `${recentInField} scans in the last 14 days found ${v.name.toLowerCase()}. Check the whole field and start treatment now.`,
    };
  }
  if (v.severity === 'SEVERE') {
    return {
      level: 'WATCH',
      title: `Severe ${v.name.toLowerCase()} in ${where}`,
      body: 'A scan found heavy damage. Scan nearby plants to see how far it has spread.',
    };
  }
  return null;
}

export function adviceFor(kind: Kind, severity: Severity, tentative: boolean): string {
  if (kind === 'HEALTHY') return 'No sign of pest or disease. Keep scouting weekly.';
  if (kind === 'UNSURE') {
    return 'This does not match anything in the guide. Compare with the guide or ask an extension officer.';
  }
  const prefix = tentative ? 'Possible match: take another photo to confirm. ' : '';
  if (severity === 'SEVERE') return prefix + 'Serious damage. Treat now and check the rest of the field today.';
  if (severity === 'MODERATE') return prefix + 'Spreading. Start treatment this week and scan nearby plants.';
  return prefix + 'Early stage. Remove affected leaves and recheck in 3 to 5 days.';
}

function alternatives(labels: Label[], lookup: Lookup): string | null {
  const parts: string[] = [];
  for (let i = 1; i < labels.length && i < 3; i++) {
    const l = labels[i];
    if (l.confidence < 0.05) break;
    const name = l.name === HEALTHY ? 'Healthy' : (lookup.nameOf(l.name) ?? pretty(l.name));
    parts.push(`${name} ${Math.round(l.confidence * 100)}%`);
  }
  return parts.length ? parts.join(', ') : null;
}

export function pretty(label: string): string {
  const s = label.replace(/_/g, ' ').trim();
  return s ? s[0].toUpperCase() + s.slice(1) : 'Unknown';
}
