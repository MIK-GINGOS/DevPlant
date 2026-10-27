import { Stack } from 'expo-router';
import { useState } from 'react';

import { searchCrops, searchGuide } from '@/bridge/store';
import { EntryRow } from '@/mixes/guideViews';
import { Card, Empty, Page, Txt } from '@/mixes/ui';

/** Search crops, diseases and pests. */
export default function Search() {
  const [query, setQuery] = useState('');
  const crops = searchCrops(query);
  const entries = searchGuide(query);

  return (
    <Page>
      <Stack.Screen
        options={{
          headerSearchBarOptions: {
            placeholder: 'Crops, diseases, pests, symptoms',
            autoFocus: true,
            hideWhenScrolling: false,
            onChangeText: e => setQuery(e.nativeEvent.text),
          },
        }}
      />
      {!query.trim() && <Txt type="caption">Try “blight”, “maize” or “yellow”.</Txt>}
      {!!query.trim() && crops.length === 0 && entries.length === 0 && (
        <Empty title="No matches" hint="Check the spelling or try a symptom." />
      )}
      {crops.length > 0 && <Txt type="label">Crops</Txt>}
      {crops.map(crop => (
        <Card key={crop.id}>
          <Txt type="heading">{crop.name}</Txt>
          <Txt type="caption">{crop.scientificName}</Txt>
        </Card>
      ))}
      {entries.length > 0 && <Txt type="label" style={{ marginTop: crops.length ? 16 : 0 }}>Diseases and pests</Txt>}
      {entries.map(e => <EntryRow key={e.key} entry={e} />)}
    </Page>
  );
}
