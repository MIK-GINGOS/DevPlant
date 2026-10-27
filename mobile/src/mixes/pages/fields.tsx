import { router } from 'expo-router';
import { Alert } from 'react-native';

import type { Field } from '@/contracts/shapes';
import { crops, deleteField, useFields } from '@/bridge/store';
import { Button, Card, Empty, MainPage, Pill, Row, Txt } from '@/mixes/ui';

/** My crops and plots. */
export default function FieldsPage() {
  const fields = useFields();

  function confirmDelete(f: Field) {
    Alert.alert(`Delete ${f.name}?`, 'Its scans are kept but no longer tied to a field. Its alerts are removed.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => deleteField(f.id) },
    ]);
  }

  return (
    <MainPage>
      <Button title="Add a field" onPress={() => router.push('/add-field')} />
      {fields.length === 0 && (
        <Empty title="No fields yet" hint="Add your plots so scans and outbreak warnings can be tracked per field." />
      )}
      {fields.map(f => (
        <Card key={f.id} onLongPress={() => confirmDelete(f)}>
          <Row>
            <Txt type="heading" style={{ flex: 1 }}>{f.name}</Txt>
            <Pill text={f.cropName} color="leaf" />
          </Row>
          <Txt type="caption" style={{ marginTop: 2 }}>
            {f.areaHectares > 0 ? `${f.areaHectares.toFixed(2)} ha` : 'Size not set'}
          </Txt>
          {!!f.notes && <Txt style={{ marginTop: 6 }}>{f.notes}</Txt>}
        </Card>
      ))}

      <Txt type="label" style={{ marginTop: 24 }}>Crops in the guide</Txt>
      {crops.map(crop => (
        <Txt key={crop.id} style={{ marginTop: 6 }}>{crop.name}  ·  {crop.scientificName}</Txt>
      ))}
      <Txt type="caption" style={{ marginTop: 16 }}>
        Long-press a field to delete it. GPS field mapping is planned.
      </Txt>
    </MainPage>
  );
}
