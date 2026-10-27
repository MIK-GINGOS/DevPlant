import { Image } from 'expo-image';
import { router, useLocalSearchParams } from 'expo-router';
import { Alert } from 'react-native';

import { deleteScan, guideEntry, treatmentsFor, useScan } from '@/bridge/store';
import { Treatments } from '@/mixes/guideViews';
import { showTab, TAB_SCAN } from '@/mixes/nav';
import { useColors } from '@/mixes/theme';
import {
  Button, Card, dateTime, Empty, isProblemKind, kindColor, kindLabel, Page, percent, Pill, Row, severityColor, severityLabel, Txt,
} from '@/mixes/ui';

/** Diagnosis and advice for one scan. */
export default function Result() {
  const c = useColors();
  const { id } = useLocalSearchParams<{ id: string }>();
  const scan = useScan(Number(id));

  if (!scan) return <Page><Empty title="Scan not found" hint="It may have been deleted." /></Page>;
  const d = scan.diagnosis;
  const entry = d ? guideEntry(d.key) : null;
  const treatments = d ? treatmentsFor(d.key) : [];

  function confirmDelete() {
    Alert.alert('Delete this scan?', 'The photo and diagnosis will be removed from this phone.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => {
          router.back();
          deleteScan(scan!.id);
        },
      },
    ]);
  }

  return (
    <Page>
      <Image
        source={{ uri: scan.photoUri }}
        style={{ height: 240, borderRadius: 18, backgroundColor: c.line }}
        contentFit="cover"
        accessibilityLabel="Leaf photo"
      />
      {!d ? (
        <Txt type="title" style={{ marginTop: 16 }}>Not diagnosed</Txt>
      ) : (
        <>
          <Txt type="title" style={{ marginTop: 16 }}>{d.name}</Txt>
          <Row style={{ marginTop: 8 }}>
            <Pill text={kindLabel(d.kind)} color={kindColor(d.kind)} />
            {isProblemKind(d.kind) && <Pill text={severityLabel(d.severity)} color={severityColor(d.severity)} />}
            <Pill text={`${percent(d.confidence)} sure`} color="unsure" />
          </Row>
          <Txt type="caption" style={{ marginTop: 8 }}>{(scan.fieldName ?? 'No field') + ' · ' + dateTime(scan.takenAt)}</Txt>

          <Card tint="leafSoft">
            <Txt type="label">What to do</Txt>
            <Txt style={{ marginTop: 6 }}>{d.advice}</Txt>
            {d.kind !== 'HEALTHY' && <Txt type="caption" style={{ marginTop: 8 }}>Damaged leaf area: about {percent(d.damage)}</Txt>}
            {d.alternatives && <Txt type="caption" style={{ marginTop: 4 }}>Other possibilities: {d.alternatives}</Txt>}
          </Card>

          {treatments.length > 0 && (
            <>
              <Txt type="heading" style={{ marginTop: 20 }}>Treatments</Txt>
              <Treatments treatments={treatments} />
            </>
          )}

          {entry && (
            <Card style={{ marginTop: 20 }}>
              <Txt type="heading">About {entry.name}</Txt>
              <Txt style={{ marginTop: 6 }}>{entry.symptoms}</Txt>
              <Button title="Open in guide" primary={false} style={{ marginTop: 10 }} onPress={() => router.push(`/guide/${entry.key}`)} />
            </Card>
          )}
        </>
      )}

      <Row style={{ marginTop: 24, gap: 12 }}>
        <Button title="Scan another leaf" style={{ flex: 1 }} onPress={() => showTab(TAB_SCAN)} />
        <Button title="Delete" primary={false} onPress={confirmDelete} />
      </Row>
    </Page>
  );
}
