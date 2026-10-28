import { useLocalSearchParams } from 'expo-router';
import * as WebBrowser from 'expo-web-browser';
import { Pressable } from 'react-native';

import type { PlantProblem } from '@/contracts/shapes';
import { usePlant } from '@/bridge/catalog';
import { PlantPhoto } from '@/mixes/plantPhoto';
import { useColors } from '@/mixes/theme';
import { Card, Empty, Page, Pill, Row, Txt } from '@/mixes/ui';

/** One catalogue plant: its photo, its 4 common problems and what to do about each. */
export default function PlantScreen() {
  const c = useColors();
  const { key } = useLocalSearchParams<{ key: string }>();
  const p = usePlant(key);
  if (!p) return <Page><Empty title="Not in the catalogue" hint="This plant could not be found." /></Page>;

  return (
    <Page>
      <PlantPhoto
        key={p.key}
        plant={p}
        style={{ height: 240, borderRadius: 18, backgroundColor: c.leafSoft }}
        contentFit="contain"
        accessibilityLabel={`Photo of ${p.name}`}
      />
      <Txt type="title" style={{ marginTop: 16 }}>{p.name}</Txt>
      <Txt type="caption" style={{ marginTop: 2, fontStyle: 'italic' }}>{p.scientificName}</Txt>
      <Row style={{ marginTop: 8 }}><Pill text={p.category} color="leaf" /></Row>

      <Txt type="heading" style={{ marginTop: 20 }}>Common problems</Txt>
      {p.problems.map((x, i) => <Problem key={x.key} number={i + 1} problem={x} />)}

      {!!p.imageSource && (
        <Pressable onPress={() => WebBrowser.openBrowserAsync(p.imageSource)} style={{ marginTop: 16 }}>
          <Txt type="caption" style={{ fontSize: 12 }}>Photo: {p.imageCredit} · tap to see the source</Txt>
        </Pressable>
      )}
      <Txt type="caption" style={{ marginTop: 8 }}>
        Advice is general. Check with a local extension officer and follow product labels.
      </Txt>
    </Page>
  );
}

function Problem({ number, problem }: { number: number; problem: PlantProblem }) {
  return (
    <Card>
      <Row>
        <Pill text={String(number)} color="disease" />
        <Txt type="heading" style={{ flex: 1 }}>{problem.name}</Txt>
      </Row>
      <Txt type="label" style={{ marginTop: 10 }}>Signs</Txt>
      <Txt style={{ marginTop: 2 }}>{problem.signs}</Txt>
      <Txt type="label" style={{ marginTop: 10 }}>What to do</Txt>
      <Txt style={{ marginTop: 2 }}>{problem.treatment}</Txt>
    </Card>
  );
}
