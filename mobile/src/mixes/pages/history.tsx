import { Image } from 'expo-image';
import { router } from 'expo-router';
import { SectionList, View } from 'react-native';

import type { Scan } from '@/contracts/shapes';
import { useRecentScans } from '@/bridge/store';
import { useColors } from '@/mixes/theme';
import { Card, day, ISLAND_CLEARANCE, Empty, isProblemKind, kindColor, kindLabel, Pill, Row, severityColor, severityLabel, time, Txt } from '@/mixes/ui';

/** Past scans, grouped by date. */
export default function HistoryPage() {
  const c = useColors();
  const scans = useRecentScans();

  const sections: { title: string; data: Scan[] }[] = [];
  for (const s of scans) {
    const title = day(s.takenAt);
    const last = sections[sections.length - 1];
    if (last && last.title === title) last.data.push(s);
    else sections.push({ title, data: [s] });
  }

  return (
    <View style={{ flex: 1, backgroundColor: c.bg }}>
      <SectionList
        sections={sections}
        keyExtractor={s => String(s.id)}
        contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: ISLAND_CLEARANCE }}
        stickySectionHeadersEnabled={false}
        ListEmptyComponent={<Empty title="No scans yet" hint="Scanned leaves appear here by date." />}
        renderSectionHeader={({ section }) => (
          <Txt type="label" style={{ paddingLeft: 4, paddingTop: 16 }}>{section.title}</Txt>
        )}
        renderItem={({ item }) => <ScanRow scan={item} />}
      />
    </View>
  );
}

function ScanRow({ scan }: { scan: Scan }) {
  const c = useColors();
  const d = scan.diagnosis;
  return (
    <Card style={{ padding: 10, marginTop: 8 }} onPress={() => router.push(`/result/${scan.id}`)}>
      <Row>
        <Image source={{ uri: scan.photoUri }} style={{ width: 60, height: 60, borderRadius: 12, backgroundColor: c.line }} contentFit="cover" />
        <View style={{ flex: 1, paddingLeft: 12 }}>
          <Txt type="heading">{d ? d.name : 'Not diagnosed'}</Txt>
          <Txt type="caption">{(scan.fieldName ?? 'No field') + ' · ' + time(scan.takenAt)}</Txt>
          {d && (
            <Row style={{ marginTop: 4 }}>
              <Pill text={kindLabel(d.kind)} color={kindColor(d.kind)} />
              {isProblemKind(d.kind) && <Pill text={severityLabel(d.severity)} color={severityColor(d.severity)} />}
            </Row>
          )}
        </View>
      </Row>
    </Card>
  );
}
