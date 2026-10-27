// gates: turns a leaf photo upright, crops it square, resizes it and hands back its pixels.
// Pure JavaScript decoding, so it runs inside Expo Go with no custom build.

import { ImageManipulator, SaveFormat } from 'expo-image-manipulator';
import jpeg from 'jpeg-js';

import type { Pixels } from './colour';

export type { Pixels };

export async function squarePixels(uri: string, size: number): Promise<Pixels> {
  // Decoding at a bounded width keeps big camera photos from using lots of memory.
  const upright = await ImageManipulator.manipulate(uri, { maxWidth: 1024 }).renderAsync();
  const side = Math.min(upright.width, upright.height);
  if (side <= 0) throw new Error('Not a readable image');
  const square = await ImageManipulator.manipulate(upright)
    .crop({
      originX: Math.floor((upright.width - side) / 2),
      originY: Math.floor((upright.height - side) / 2),
      width: side,
      height: side,
    })
    .resize({ width: size, height: size })
    .renderAsync();
  const saved = await square.saveAsync({ format: SaveFormat.JPEG, compress: 1, base64: true });
  if (!saved.base64) throw new Error('Could not read the photo');
  const decoded = jpeg.decode(base64ToBytes(saved.base64), { useTArray: true, formatAsRGBA: true });
  return { size: decoded.width, rgba: decoded.data };
}

function base64ToBytes(b64: string): Uint8Array {
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes;
}
