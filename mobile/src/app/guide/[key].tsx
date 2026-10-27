import { useLocalSearchParams } from 'expo-router';

import { guideEntry, treatmentsFor } from '@/bridge/store';
import { Treatments } from '@/mixes/guideViews';
import { Card, Empty, kindColor, kindLabel, Page, Pill, Row, Txt } from '@/mixes/ui';

/** One disease or pest: symptoms, spread and treatments. */
export default function GuideEntryScreen() {
  const { key } = useLocalSearchParams<{ key: string }>();
  const e = guideEntry(key);
  if (!e) return <Page><Empty title="Not in the guide" hint="This entry could not be found." /></Page>;
  const treatments = treatmentsFor(e.key);

  return (
    <Page>
      <Txt type="title">{e.name}</Txt>
      <Txt type="caption" style={{ marginTop: 2, fontStyle: 'italic' }}>{e.scientificName}</Txt>
      <Row style={{ marginTop: 8 }}><Pill text={kindLabel(e.kind)} color={kindColor(e.kind)} /></Row>
      <Section label="Crops" text={e.crops} />
      <Section label="Symptoms" text={e.symptoms} />
      <Section label={e.kind === 'PEST' ? 'How it spreads and feeds' : 'How it spreads'} text={e.spread} />
      {treatments.length > 0 && (
        <>
          <Txt type="heading" style={{ marginTop: 20 }}>Treatments</Txt>
          <Treatments treatments={treatments} />
        </>
      )}
    </Page>
  );
}

function Section({ label, text }: { label: string; text: string }) {
  return (
    <Card>
      <Txt type="label">{label}</Txt>
      <Txt style={{ marginTop: 4 }}>{text}</Txt>
    </Card>
  );
}
