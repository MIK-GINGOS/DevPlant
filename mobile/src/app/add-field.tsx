import { router } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, TextInput } from 'react-native';

import { addField, crops } from '@/bridge/store';
import { useColors } from '@/mixes/theme';
import { Button, Chip, Row, Txt } from '@/mixes/ui';

/** Name, crop, size and notes for a new plot. */
export default function AddField() {
  const c = useColors();
  const [name, setName] = useState('');
  const [cropId, setCropId] = useState(crops[0].id);
  const [area, setArea] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);

  const input = [styles.input, { color: c.ink, backgroundColor: c.surface, borderColor: c.line }];

  async function save() {
    const n = name.trim();
    if (!n) return setError('Give the field a name');
    const a = area.trim().replace(',', '.');
    const hectares = a ? Number(a) : 0;
    if (Number.isNaN(hectares) || hectares < 0) return setError('Enter the size as a number');
    await addField(n, cropId, hectares, notes.trim() || null);
    router.back();
  }

  return (
    <ScrollView style={{ backgroundColor: c.bg }} contentContainerStyle={{ padding: 16, paddingBottom: 40 }} keyboardShouldPersistTaps="handled">
      <Txt type="label">Name</Txt>
      <TextInput
        style={input}
        placeholder="Field name, e.g. North plot"
        placeholderTextColor={c.inkSoft}
        value={name}
        onChangeText={t => { setName(t); setError(null); }}
        autoCapitalize="sentences"
        autoFocus
      />
      <Txt type="label" style={{ marginTop: 16 }}>Crop</Txt>
      <Row style={{ flexWrap: 'wrap', gap: 8, marginTop: 8 }}>
        {crops.map(cr => <Chip key={cr.id} text={cr.name} selected={cr.id === cropId} onPress={() => setCropId(cr.id)} />)}
      </Row>
      <Txt type="label" style={{ marginTop: 16 }}>Size in hectares (optional)</Txt>
      <TextInput style={input} placeholder="0.5" placeholderTextColor={c.inkSoft} value={area} onChangeText={setArea} keyboardType="decimal-pad" />
      <Txt type="label" style={{ marginTop: 16 }}>Notes (optional)</Txt>
      <TextInput style={input} placeholder="Notes" placeholderTextColor={c.inkSoft} value={notes} onChangeText={setNotes} />
      {error && <Txt color="severe" style={{ marginTop: 12 }}>{error}</Txt>}
      <Button title="Add field" style={{ marginTop: 20 }} onPress={save} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  input: { borderWidth: 1, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 12, fontSize: 16, marginTop: 8 },
});
