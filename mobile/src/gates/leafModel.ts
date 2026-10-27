// gates: the leaf model. Same stand-in as the Android app's ColourHeuristicModel: it only looks at
// leaf colour, so it can tell "mostly green", "yellowing" and "brown spotting" apart and nothing
// finer. Confidence is capped so the rules treat its answers as tentative.

import { hsv, type Pixels } from './colour';

export interface Label {
  name: string;
  confidence: number;
}

export const MODEL_NAME = 'Colour heuristic (no trained model bundled)';
export const MODEL_INPUT = 128;
const CAP = 0.75;

export function classify(px: Pixels): Label[] {
  let green = 0, yellow = 0, brown = 0;
  const count = px.size * px.size;
  for (let i = 0; i < count; i++) {
    const [h, s, v] = hsv(px.rgba[i * 4], px.rgba[i * 4 + 1], px.rgba[i * 4 + 2]);
    if (s < 0.2 || v < 0.12) continue;
    if (h >= 70 && h <= 170) green++;
    else if (h >= 45 && h < 70 && v > 0.35) yellow++;
    else if (h >= 10 && h < 45 && v < 0.75) brown++;
  }
  const plant = green + yellow + brown;
  // Not enough leaf in view; low confidence makes the rules ask for a retake.
  if (plant < count / 10) return [{ name: 'healthy', confidence: 0.2 }];

  const y = yellow / plant;
  const b = brown / plant;
  const healthy = Math.max(0, 1 - 4 * (y + b));
  const spot = Math.min(1, 2.5 * b);
  const chlorosis = Math.min(1, 2.5 * y);
  const sum = healthy + spot + chlorosis || 1;
  return [
    { name: 'healthy', confidence: (CAP * healthy) / sum },
    { name: 'leaf_spot', confidence: (CAP * spot) / sum },
    { name: 'chlorosis', confidence: (CAP * chlorosis) / sum },
  ].sort((a, b2) => b2.confidence - a.confidence);
}
