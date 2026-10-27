import Constants from 'expo-constants';
import { useEffect, useState } from 'react';

import { catalogUrl } from '@/bridge/catalog';
import { MODEL_NAME } from '@/gates/leafModel';
import { loadThemeMode, setThemeMode, type ThemeMode } from '@/mixes/theme';
import { Card, Chip, Page, Row, Txt } from '@/mixes/ui';

const MODES: [ThemeMode, string][] = [['system', 'Follow the phone'], ['light', 'Light'], ['dark', 'Dark']];

/** Theme, model and about. */
export default function Settings() {
  const [mode, setMode] = useState<ThemeMode>('system');
  useEffect(() => {
    loadThemeMode().then(setMode);
  }, []);

  return (
    <Page>
      <Card style={{ marginTop: 0 }}>
        <Txt type="label">Theme</Txt>
        <Row style={{ gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
          {MODES.map(([m, label]) => (
            <Chip key={m} text={label} selected={mode === m} onPress={() => { setMode(m); setThemeMode(m); }} />
          ))}
        </Row>
      </Card>

      <Card>
        <Txt type="label">Diagnosis model</Txt>
        <Txt type="heading" style={{ marginTop: 4 }}>{MODEL_NAME}</Txt>
        <Txt style={{ marginTop: 4 }}>
          Runs on this phone with no signal needed. Sending unsure scans to an expert is planned.
        </Txt>
      </Card>

      <Card>
        <Txt type="label">Plant catalogue</Txt>
        <Txt style={{ marginTop: 4 }}>600 plants ship with the app. A newer list and the photos come from:</Txt>
        <Txt type="caption" selectable style={{ marginTop: 4 }}>{catalogUrl}</Txt>
      </Card>

      <Card>
        <Txt type="label">About</Txt>
        <Txt type="heading" style={{ marginTop: 4 }}>DevPlant {Constants.expoConfig?.version ?? ''}</Txt>
        <Txt style={{ marginTop: 4 }}>
          Early detection of crop pests and diseases from a photo of one leaf. Photos and results stay on this phone.
        </Txt>
        <Txt type="caption" style={{ marginTop: 8 }}>
          Advice is general. Confirm serious problems with your local extension officer and always follow product labels.
        </Txt>
      </Card>
    </Page>
  );
}
