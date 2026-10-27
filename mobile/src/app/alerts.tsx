import { router, Stack } from 'expo-router';
import { Pressable } from 'react-native';

import { markAlertRead, markAllAlertsRead, useAlerts } from '@/bridge/store';
import { ago, Card, Empty, Page, Pill, Row, Txt } from '@/mixes/ui';

/** Outbreak warnings raised from the farmer's own scans. */
export default function Alerts() {
  const alerts = useAlerts();
  const unread = alerts.some(a => !a.read);

  return (
    <Page>
      <Stack.Screen
        options={{
          headerRight: () =>
            unread ? (
              <Pressable hitSlop={8} onPress={markAllAlertsRead}>
                <Txt color="leaf">Mark all read</Txt>
              </Pressable>
            ) : null,
        }}
      />
      {alerts.length === 0 && (
        <Empty title="No warnings" hint="DevPlant warns you when a problem keeps showing up in a field or a scan finds severe damage." />
      )}
      {alerts.map(a => (
        <Card
          key={a.id}
          tint={a.read ? undefined : 'leafSoft'}
          onPress={() => {
            if (!a.read) markAlertRead(a.id);
            if (a.key) router.push(`/guide/${a.key}`);
          }}>
          <Row>
            <Pill text={a.level === 'WARNING' ? 'Warning' : 'Watch'} color={a.level === 'WARNING' ? 'severe' : 'moderate'} />
            {!a.read && <Pill text="New" color="leaf" />}
          </Row>
          <Txt type="heading" style={{ marginTop: 8 }}>{a.title}</Txt>
          <Txt style={{ marginTop: 4 }}>{a.body}</Txt>
          <Txt type="caption" style={{ marginTop: 6 }}>{ago(a.createdAt)}</Txt>
        </Card>
      ))}
      {alerts.length > 0 && (
        <Txt type="caption" style={{ marginTop: 12 }}>Regional outbreak alerts from the internet are planned.</Txt>
      )}
    </Page>
  );
}
