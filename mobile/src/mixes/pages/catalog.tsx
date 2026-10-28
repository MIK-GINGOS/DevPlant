import { router } from 'expo-router';
import { useDeferredValue, useState } from 'react';
import { FlatList, Pressable, StyleSheet, TextInput, View } from 'react-native';

import type { Plant } from '@/contracts/shapes';
import { searchPlants, useCatalog } from '@/bridge/catalog';
import { showTab, TAB_LIBRARY } from '@/mixes/nav';
import { PlantPhoto } from '@/mixes/plantPhoto';
import { useColors } from '@/mixes/theme';
import { Chip, ChipRow, Empty, ISLAND_CLEARANCE, Row, Txt } from '@/mixes/ui';

/** The plant catalogue: 600 plants with photos. Search and filter by category. */
export default function CatalogPage() {
  const c = useColors();
  const { plants, categories } = useCatalog();
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState<string | null>(null);
  const deferred = useDeferredValue(query);
  const shown = searchPlants(plants, deferred, category);

  return (
    <View style={{ flex: 1, backgroundColor: c.bg }}>
      <View style={{ paddingHorizontal: 16, paddingTop: 8 }}>
        <TextInput
          style={[styles.search, { color: c.ink, backgroundColor: c.surface, borderColor: c.line }]}
          placeholder="Search plants or problems"
          placeholderTextColor={c.inkSoft}
          value={query}
          onChangeText={setQuery}
          clearButtonMode="while-editing"
          returnKeyType="search"
          autoCorrect={false}
        />
        <ChipRow>
          <Chip text="All" selected={category == null} onPress={() => setCategory(null)} />
          {categories.map(name => (
            <Chip key={name} text={name} selected={category === name} onPress={() => setCategory(name)} />
          ))}
        </ChipRow>
        <Row style={{ paddingVertical: 4 }}>
          <Txt type="caption" style={{ flex: 1 }}>
            {shown.length === plants.length ? `${plants.length} plants` : `${shown.length} of ${plants.length} plants`}
          </Txt>
          <Pressable hitSlop={8} onPress={() => showTab(TAB_LIBRARY)}>
            <Txt type="caption" color="leaf">Pest guide →</Txt>
          </Pressable>
        </Row>
      </View>
      <FlatList
        data={shown}
        keyExtractor={p => p.key}
        numColumns={2}
        keyboardDismissMode="on-drag"
        contentContainerStyle={{ paddingHorizontal: 10, paddingBottom: ISLAND_CLEARANCE }}
        ListEmptyComponent={<Empty title="No plants match" hint="Try another word or category." />}
        renderItem={({ item }) => <PlantCard plant={item} />}
        initialNumToRender={8}
        windowSize={7}
      />
    </View>
  );
}

function PlantCard({ plant }: { plant: Plant }) {
  const c = useColors();
  return (
    <Pressable
      accessibilityLabel={plant.name}
      onPress={() => router.push(`/plant/${plant.key}`)}
      style={({ pressed }) => [styles.card, { backgroundColor: c.surface, borderColor: c.line }, pressed && { opacity: 0.7 }]}>
      <PlantPhoto
        key={plant.key}
        plant={plant}
        style={{ height: 120, backgroundColor: c.leafSoft }}
        contentFit="cover"
        transition={150}
        recyclingKey={plant.key}
      />
      <View style={{ padding: 12, paddingTop: 10 }}>
        <Txt type="heading" style={{ fontSize: 15 }} numberOfLines={1}>{plant.name}</Txt>
        <Txt type="caption" style={{ fontSize: 12 }} numberOfLines={1}>{plant.category}</Txt>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  search: { borderWidth: 1, borderRadius: 14, paddingHorizontal: 14, paddingVertical: 12, fontSize: 16 },
  card: { flex: 1, margin: 6, borderRadius: 18, borderWidth: StyleSheet.hairlineWidth * 2, overflow: 'hidden', borderCurve: 'continuous', maxWidth: '50%' },
});
