import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useEffect } from 'react';
import { Alert, View } from 'react-native';

import {
  area, deleteReport, sourceLabel, syncLabel, syncSoon, useConsent, useReports, verificationLabel,
} from '@/bridge/reports';
import type { Report } from '@/contracts/reports';
import { showTab, TAB_SCAN } from '@/mixes/nav';
import { useColors, type ColorName } from '@/mixes/theme';
import { Button, Card, dateTime, Empty, kindColor, kindLabel, Page, Pill, Row, Txt } from '@/mixes/ui';

const verificationColor = (v: Report['verification']): ColorName =>
  v === 'UNDER_REVIEW' ? 'moderate' : v === 'FIELD_VERIFIED' ? 'healthy' : 'unsure';

/** My Reports: every scan as a report, with where it stands and whether it has left the phone. */
export default function Reports() {
  const c = useColors();
  const consent = useConsent();
  const reports = useReports();
  const where = area(consent.country, consent.district);

  // Send what waits and fetch officers' statuses; anything new shows up in the list.
  useEffect(() => syncSoon(), []);

  function openReport(r: Report) {
    if (r.scanId != null) return router.push(`/result/${r.scanId}`);
    Alert.alert(
      "We couldn't tell yet",
      (r.uncertainReason ? r.uncertainReason + '\n\n' : '')
        + 'Try another clear photo, check more than one plant, or ask your extension officer.',
      [
        { text: 'Delete', style: 'destructive', onPress: () => deleteReport(r.id) },
        { text: 'Ask an officer', onPress: () => router.push(`/help?report=${r.id}`) },
        { text: 'Scan again', onPress: () => showTab(TAB_SCAN) },
        { text: 'Close', style: 'cancel' },
      ],
    );
  }

  return (
    <Page>
      <Card style={{ marginTop: 0 }}>
        <Txt type="label">Sharing with extension officers</Txt>
        <Txt style={{ marginTop: 4 }}>
          {consent.share
            ? 'On. Report details are sent when the phone has signal. Photos are never sent.'
            : 'Off. Everything stays on this phone, unless you ask an officer about a report.'}
        </Txt>
        {where && <Txt type="caption" style={{ marginTop: 4 }}>Area: {where}</Txt>}
        <Button title="Change" primary={false} style={{ marginTop: 10 }} onPress={() => router.push('/welcome')} />
      </Card>
      <Button title="Ask an extension officer" style={{ marginTop: 12 }} onPress={() => router.push('/help')} />

      {reports.length === 0 && (
        <Empty title="No reports yet" hint="Each scan becomes a report here, including photos DevPlant could not read." />
      )}
      {reports.map(r => {
        const place = area(r.country, r.district);
        const caption = [r.cropName, place, dateTime(r.createdAt)].filter(Boolean).join(' · ');
        const title = r.scanId == null ? 'Not recognised yet' : (r.resultName ?? 'Not diagnosed');
        return (
          <Card key={r.id} onPress={() => openReport(r)}>
            <Row style={{ alignItems: 'flex-start' }}>
              <Image
                source={{ uri: r.photoUri }}
                style={{ width: 60, height: 60, borderRadius: 12, backgroundColor: c.line }}
                contentFit="cover"
              />
              <View style={{ flex: 1, paddingLeft: 12 }}>
                <Txt type="heading">{title}</Txt>
                <Txt type="caption">{caption}</Txt>
                <Row style={{ marginTop: 4 }}>
                  <Pill text={kindLabel(r.kind)} color={kindColor(r.kind)} />
                  <Pill text={sourceLabel(r.source)} color="unsure" />
                </Row>
                <Row style={{ marginTop: 4 }}>
                  <Pill text={verificationLabel(r.verification)} color={verificationColor(r.verification)} />
                  <Pill text={syncLabel(r.sync)} color="leaf" />
                </Row>
              </View>
            </Row>
            {r.scanId == null && r.uncertainReason && (
              <Txt type="caption" style={{ marginTop: 8 }}>{r.uncertainReason}</Txt>
            )}
            {r.officerNote && <Txt style={{ marginTop: 8 }}>Officer: {r.officerNote}</Txt>}
          </Card>
        );
      })}
      {reports.length > 0 && (
        <Txt type="caption" style={{ marginTop: 16 }}>
          &ldquo;Reported&rdquo; means only that a scan was made. A case is confirmed only after an officer checks the field.
        </Txt>
      )}
    </Page>
  );
}
