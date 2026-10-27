// gates: pixel shapes and colour maths shared by the leaf model and the leaf measurements.

export interface Pixels {
  size: number;
  /** RGBA, 4 bytes per pixel, row by row. */
  rgba: Uint8Array;
}

/** Hue in degrees, saturation and value 0..1, the same as Android's Color.colorToHSV. */
export function hsv(r: number, g: number, b: number): [number, number, number] {
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const d = max - min;
  let h = 0;
  if (d !== 0) {
    if (max === r) h = ((g - b) / d) % 6;
    else if (max === g) h = (b - r) / d + 2;
    else h = (r - g) / d + 4;
    h *= 60;
    if (h < 0) h += 360;
  }
  return [h, max === 0 ? 0 : d / max, max / 255];
}
