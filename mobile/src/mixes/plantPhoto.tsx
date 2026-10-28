// mixes: a catalogue photo. Loads from the cloud and switches to the mirror if that fails.

import { Image, type ImageProps } from 'expo-image';
import { useState } from 'react';

import type { Plant } from '@/contracts/shapes';

export function PlantPhoto({ plant, ...props }: { plant: Plant } & Omit<ImageProps, 'source'>) {
  const [failed, setFailed] = useState(false);
  return (
    <Image
      {...props}
      source={{ uri: failed ? plant.imageFallback : plant.imageUrl }}
      cachePolicy="disk"
      onError={() => setFailed(true)}
    />
  );
}
