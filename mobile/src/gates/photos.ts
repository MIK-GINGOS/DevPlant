// gates: keeps leaf photos on the phone, in the app's own documents folder.

import { Directory, File, Paths } from 'expo-file-system';

function folder(): Directory {
  const dir = new Directory(Paths.document, 'scans');
  if (!dir.exists) dir.create({ intermediates: true, idempotent: true });
  return dir;
}

/** Copies a photo into the scans folder and returns its new address. */
export function keepPhoto(uri: string): string {
  const target = new File(folder(), `scan_${Date.now()}.jpg`);
  new File(uri).copySync(target);
  return target.uri;
}

export function deletePhoto(uri: string): void {
  try {
    const f = new File(uri);
    if (f.exists) f.delete();
  } catch {
    // already gone
  }
}
