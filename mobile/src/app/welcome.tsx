import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, TextInput } from 'react-native';

import { saveConsent, useConsent } from '@/bridge/reports';
import { useColors } from '@/mixes/theme';
import { Button, Card, Chip, Page, Row, Txt } from '@/mixes/ui';

const COUNTRIES = ['Zambia', 'Malawi', 'Zimbabwe', 'Botswana', 'Other'];

/**
 * Shown once on first launch, and again from My Reports: what DevPlant is, what stays on the
 * phone, and whether report details may be shared with extension officers.
 */
export default function Welcome() {
  const c = useColors();
  const consent = useConsent();
  const [country, setCountry] = useState<string | null>(consent.country);
  const [district, setDistrict] = useState(consent.district ?? '');

  async function save(share: boolean) {
    await saveConsent({ share, country, district: district.trim() || null });
    router.back();
  }

  return (
    <Page>
      <Txt type="title">Check a crop from one leaf</Txt>
      <Txt style={{ marginTop: 8 }}>
        Photograph one leaf and DevPlant suggests what may be wrong and what to do next. A result is a first
        assessment, not a confirmed diagnosis.
      </Txt>

      <Card tint="leafSoft">
        <Txt type="label">Stays on this phone</Txt>
        <Txt style={{ marginTop: 4 }}>Your photos, always. DevPlant works with no signal.</Txt>
      </Card>

      <Card>
        <Txt type="heading">Share reports with extension officers?</Txt>
        <Txt style={{ marginTop: 6 }}>
          If you agree, each scan is shared as a short text record: crop, result, how sure DevPlant was, the date
          and the area you choose below. No photos, no exact location. Records are sent when the phone has
          signal, and officers can reply with what they found.
        </Txt>
        <Txt type="label" style={{ marginTop: 12 }}>Country</Txt>
        <Row style={{ flexWrap: 'wrap', gap: 8, marginTop: 8 }}>
          {COUNTRIES.map(n => <Chip key={n} text={n} selected={n === country} onPress={() => setCountry(n)} />)}
        </Row>
        <Txt type="label" style={{ marginTop: 12 }}>District (optional)</Txt>
        <TextInput
          style={[styles.input, { color: c.ink, backgroundColor: c.surface, borderColor: c.line }]}
          placeholder="For example Chipata"
          placeholderTextColor={c.inkSoft}
          value={district}
          onChangeText={setDistrict}
          autoCapitalize="words"
        />
      </Card>

      <Button title="Share reports" style={{ marginTop: 20 }} onPress={() => save(true)} />
      <Button title="Keep everything on this phone" primary={false} style={{ marginTop: 8 }} onPress={() => save(false)} />
      <Txt type="caption" style={{ marginTop: 12 }}>You can change this any time in My reports.</Txt>
    </Page>
  );
}

const styles = StyleSheet.create({
  input: { borderWidth: 1, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 12, fontSize: 16, marginTop: 8 },
});
