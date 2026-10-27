import { useState } from 'react';

import type { Kind } from '@/contracts/shapes';
import { crops, guide } from '@/bridge/store';
import { EntryRow } from '@/mixes/guideViews';
import { Chip, ChipRow, Empty, MainPage } from '@/mixes/ui';

/** The pest and disease guide, filtered by type and crop. */
export default function LibraryPage() {
  const [kind, setKind] = useState<Kind | null>(null);
  const [crop, setCrop] = useState<string | null>(null);

  const shown = guide.filter(
    e => (!kind || e.kind === kind) && (!crop || e.crops.includes(crop) || e.crops.includes('All crops')),
  );

  return (
    <MainPage>
      <ChipRow>
        <Chip text="All" selected={kind == null} onPress={() => setKind(null)} />
        <Chip text="Diseases" selected={kind === 'DISEASE'} onPress={() => setKind('DISEASE')} />
        <Chip text="Pests" selected={kind === 'PEST'} onPress={() => setKind('PEST')} />
      </ChipRow>
      <ChipRow>
        <Chip text="All crops" selected={crop == null} onPress={() => setCrop(null)} />
        {crops.map(cr => <Chip key={cr.id} text={cr.name} selected={crop === cr.name} onPress={() => setCrop(cr.name)} />)}
      </ChipRow>
      {shown.length === 0 && <Empty title="Nothing here" hint="Try another crop or type." />}
      {shown.map(e => <EntryRow key={e.key} entry={e} />)}
    </MainPage>
  );
}
