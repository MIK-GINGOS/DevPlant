// mixes: guide rows and treatment cards, shared by the guide, result and search screens.

import { router } from 'expo-router';
import { View } from 'react-native';

import type { GuideEntry, Treatment } from '@/contracts/shapes';

import { Card, kindColor, kindLabel, methodColor, methodLabel, Pill, Row, Txt } from './ui';

export function EntryRow({ entry }: { entry: GuideEntry }) {
  return (
    <Card onPress={() => router.push(`/guide/${entry.key}`)}>
      <Row>
        <Txt type="heading" style={{ flex: 1 }}>{entry.name}</Txt>
        <Pill text={kindLabel(entry.kind)} color={kindColor(entry.kind)} />
      </Row>
      <Txt type="caption" style={{ marginTop: 2, fontStyle: 'italic' }}>{entry.scientificName}</Txt>
      <Txt type="caption" style={{ marginTop: 2 }}>{entry.crops}</Txt>
    </Card>
  );
}

export function Treatments({ treatments }: { treatments: Treatment[] }) {
  return (
    <View>
      {treatments.map((t, i) => (
        <Card key={i}>
          <Row>
            <Txt type="heading" style={{ flex: 1 }}>{t.title}</Txt>
            <Pill text={methodLabel(t.method)} color={methodColor(t.method)} />
          </Row>
          <Txt style={{ marginTop: 6 }}>{t.detail}</Txt>
        </Card>
      ))}
      <Txt type="caption" style={{ marginTop: 8 }}>
        Always follow the product label and local extension advice for chemical use.
      </Txt>
    </View>
  );
}
