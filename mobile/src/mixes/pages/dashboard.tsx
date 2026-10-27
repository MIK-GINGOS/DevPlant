import { router } from 'expo-router';
import { Image, View } from 'react-native';

import { fieldScore, type FieldHealth } from '@/contracts/shapes';
import { useHealth } from '@/bridge/store';
import { useColors, type ColorName } from '@/mixes/theme';
import { showTab, TAB_SCAN } from '@/mixes/nav';
import { Button, Card, MainPage, Row, Txt } from '@/mixes/ui';

/** Field health numbers over the last 30 days. */
export default function DashboardPage() {
  const c = useColors();
  const h = useHealth(30);
  const decided = h.healthy + h.diseases + h.pests;

  return (
    <MainPage>
      <Txt type="label">Last 30 days</Txt>
      <Row style={{ gap: 12, marginTop: -4 }}>
        <Tile number={String(h.scans)} label="Scans" color="ink" />
        <Tile number={decided === 0 ? '–' : `${Math.round((100 * h.healthy) / decided)}%`} label="Healthy" color="healthy" />
      </Row>
      <Row style={{ gap: 12 }}>
        <Tile number={String(h.diseases)} label="Diseases" color="disease" />
        <Tile number={String(h.pests)} label="Pests" color="pest" />
      </Row>
      {h.unsure > 0 && (
        <Txt type="caption" style={{ marginTop: 8 }}>{h.unsure} scans could not be matched to the guide.</Txt>
      )}

      <Card onPress={() => router.push('/alerts')}>
        <Row>
          <Txt type="heading" style={{ flex: 1 }}>
            {h.unreadAlerts === 0 ? 'No new warnings' : `${h.unreadAlerts} new warning${h.unreadAlerts === 1 ? '' : 's'}`}
          </Txt>
          <Image source={require('@/assets/icons/bell.png')} style={{ width: 22, height: 22, tintColor: h.unreadAlerts === 0 ? c.inkSoft : c.severe }} />
        </Row>
      </Card>

      <Txt type="label" style={{ marginTop: 20 }}>By field</Txt>
      {h.fields.length === 0 && (
        <Card>
          <Txt type="heading">Nothing to show yet</Txt>
          <Txt style={{ marginTop: 4 }}>Scan a few leaves to see how your fields are doing.</Txt>
          <Button title="Scan a leaf" style={{ marginTop: 12 }} onPress={() => showTab(TAB_SCAN)} />
        </Card>
      )}
      {h.fields.map(f => <FieldRow key={f.fieldId} f={f} />)}
    </MainPage>
  );
}

function Tile({ number, label, color }: { number: string; label: string; color: ColorName }) {
  return (
    <Card style={{ flex: 1 }}>
      <Txt type="number" color={color}>{number}</Txt>
      <Txt type="caption">{label}</Txt>
    </Card>
  );
}

function FieldRow({ f }: { f: FieldHealth }) {
  const c = useColors();
  const score = fieldScore(f);
  return (
    <Card>
      <Row>
        <Txt type="heading" style={{ flex: 1 }}>{f.fieldName ?? 'No field'}</Txt>
        <Txt type="caption">{score < 0 ? '–' : `${score}% healthy`}</Txt>
      </Row>
      <View style={{ flexDirection: 'row', height: 10, borderRadius: 5, overflow: 'hidden', backgroundColor: c.line, marginTop: 10 }}>
        {f.healthy + f.sick > 0 && (
          <>
            <View style={{ flex: f.healthy, backgroundColor: c.healthy }} />
            <View style={{ flex: f.sick, backgroundColor: c.severe }} />
          </>
        )}
      </View>
      <Txt type="caption" style={{ marginTop: 6 }}>
        {f.scans === 0 ? 'No scans in 30 days' : `${f.scans} scan${f.scans === 1 ? '' : 's'} · ${f.sick} with problems`}
      </Txt>
    </Card>
  );
}
