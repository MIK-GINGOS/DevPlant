// mixes: text styles and the small building blocks every screen is made from, after the Android
// app's mm_theme/Ui.java.

import { SymbolView } from 'expo-symbols';
import type { ReactNode } from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  type StyleProp,
  type TextStyle,
  type ViewStyle,
} from 'react-native';

import type { Kind, Method, Severity } from '@/contracts/shapes';

import { useColors, type ColorName } from './theme';

export type TextType = 'title' | 'heading' | 'body' | 'caption' | 'number' | 'label';

export function Txt({
  type = 'body',
  color,
  style,
  children,
  numberOfLines,
  selectable,
}: {
  type?: TextType;
  color?: ColorName;
  style?: StyleProp<TextStyle>;
  children: ReactNode;
  numberOfLines?: number;
  selectable?: boolean;
}) {
  const c = useColors();
  const soft = type === 'caption' || type === 'label';
  return (
    <Text
      numberOfLines={numberOfLines}
      selectable={selectable}
      style={[styles[type], { color: c[color ?? (soft ? 'inkSoft' : 'ink')] }, style]}>
      {children}
    </Text>
  );
}

export function Card({
  children,
  style,
  onPress,
  onLongPress,
  tint,
}: {
  children: ReactNode;
  style?: StyleProp<ViewStyle>;
  onPress?: () => void;
  onLongPress?: () => void;
  /** Fill with this colour and drop the border. */
  tint?: ColorName;
}) {
  const c = useColors();
  const look: ViewStyle = tint
    ? { backgroundColor: c[tint], borderColor: c[tint] }
    : { backgroundColor: c.surface, borderColor: c.line };
  if (!onPress && !onLongPress) return <View style={[styles.card, look, style]}>{children}</View>;
  return (
    <Pressable
      onPress={onPress}
      onLongPress={onLongPress}
      style={({ pressed }) => [styles.card, look, pressed && { opacity: 0.7 }, style]}>
      {children}
    </Pressable>
  );
}

export function Pill({ text, color }: { text: string; color: ColorName }) {
  const c = useColors();
  return (
    <View style={[styles.pill, { backgroundColor: c[color] + '28' }]}>
      <Text style={[styles.pillText, { color: c[color] }]}>{text}</Text>
    </View>
  );
}

export function Row({ children, style }: { children: ReactNode; style?: StyleProp<ViewStyle> }) {
  return <View style={[styles.row, style]}>{children}</View>;
}

export function Button({
  title,
  onPress,
  primary = true,
  style,
  disabled,
}: {
  title: string;
  onPress: () => void;
  primary?: boolean;
  style?: StyleProp<ViewStyle>;
  disabled?: boolean;
}) {
  const c = useColors();
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [
        styles.button,
        primary ? { backgroundColor: c.leaf } : { borderColor: c.line, borderWidth: 1 },
        (pressed || disabled) && { opacity: 0.6 },
        style,
      ]}>
      <Text style={[styles.buttonText, { color: primary ? '#FFFFFF' : c.leaf }]}>{title}</Text>
    </Pressable>
  );
}

export function Chip({ text, selected, onPress }: { text: string; selected: boolean; onPress: () => void }) {
  const c = useColors();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ selected }}
      onPress={onPress}
      style={[
        styles.chip,
        selected ? { backgroundColor: c.leafSoft, borderColor: c.leaf } : { backgroundColor: c.surface, borderColor: c.line },
      ]}>
      <Text style={[styles.chipText, { color: selected ? c.leaf : c.ink }]}>{text}</Text>
    </Pressable>
  );
}

export function ChipRow({ children }: { children: ReactNode }) {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{ gap: 8, paddingVertical: 6 }}>
      {children}
    </ScrollView>
  );
}

/** A scrolling page. Pages inside a stack let the native header handle the safe area. */
export function Page({ children }: { children: ReactNode }) {
  const c = useColors();
  return (
    <ScrollView
      style={{ backgroundColor: c.bg }}
      contentInsetAdjustmentBehavior="automatic"
      contentContainerStyle={styles.page}
      keyboardShouldPersistTaps="handled">
      {children}
    </ScrollView>
  );
}

export function Empty({ title, hint }: { title: string; hint: string }) {
  const c = useColors();
  return (
    <View style={styles.empty}>
      <SymbolView name={{ ios: 'leaf', android: 'eco', web: 'eco' }} size={52} tintColor={c.line} />
      <Txt type="heading" style={{ textAlign: 'center', marginTop: 12 }}>{title}</Txt>
      <Txt type="caption" style={{ textAlign: 'center', marginTop: 6 }}>{hint}</Txt>
    </View>
  );
}

// ---- words and colours for the plain shapes ----

export const kindColor = (k: Kind): ColorName =>
  k === 'HEALTHY' ? 'healthy' : k === 'DISEASE' ? 'disease' : k === 'PEST' ? 'pest' : 'unsure';

export const kindLabel = (k: Kind) =>
  k === 'HEALTHY' ? 'Healthy' : k === 'DISEASE' ? 'Disease' : k === 'PEST' ? 'Pest' : 'Unsure';

export const severityColor = (s: Severity): ColorName =>
  s === 'SEVERE' ? 'severe' : s === 'MODERATE' ? 'moderate' : s === 'MILD' ? 'mild' : 'healthy';

export const severityLabel = (s: Severity) =>
  s === 'SEVERE' ? 'Severe' : s === 'MODERATE' ? 'Moderate' : s === 'MILD' ? 'Mild' : 'None';

export const methodLabel = (m: Method) => (m === 'CHEMICAL' ? 'Chemical' : m === 'ORGANIC' ? 'Organic' : 'Cultural');

export const methodColor = (m: Method): ColorName => (m === 'CHEMICAL' ? 'disease' : m === 'ORGANIC' ? 'pest' : 'healthy');

export const isProblemKind = (k: Kind) => k === 'DISEASE' || k === 'PEST';

export const percent = (f: number) => `${Math.round(f * 100)}%`;

export const time = (ms: number) =>
  new Date(ms).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });

export const dateTime = (ms: number) =>
  new Date(ms).toLocaleString(undefined, { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });

export function day(ms: number): string {
  const d = new Date(ms);
  const today = new Date();
  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1);
  if (d.toDateString() === today.toDateString()) return 'Today';
  if (d.toDateString() === yesterday.toDateString()) return 'Yesterday';
  return d.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
}

export function ago(ms: number): string {
  const mins = Math.round((Date.now() - ms) / 60000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins} min ago`;
  const hours = Math.round(mins / 60);
  if (hours < 24) return `${hours} h ago`;
  const days = Math.round(hours / 24);
  return days === 1 ? 'Yesterday' : `${days} days ago`;
}

const styles = StyleSheet.create({
  title: { fontSize: 24, fontWeight: '700' },
  heading: { fontSize: 17, fontWeight: '700' },
  body: { fontSize: 15, lineHeight: 21 },
  caption: { fontSize: 13 },
  number: { fontSize: 32, fontWeight: '700', fontVariant: ['tabular-nums'] },
  label: { fontSize: 12, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1 },
  card: { borderRadius: 18, borderWidth: StyleSheet.hairlineWidth * 2, padding: 16, marginTop: 12, borderCurve: 'continuous' },
  pill: { borderRadius: 99, paddingHorizontal: 10, paddingVertical: 4, marginRight: 6, alignSelf: 'flex-start' },
  pillText: { fontSize: 12, fontWeight: '700' },
  row: { flexDirection: 'row', alignItems: 'center' },
  button: { borderRadius: 14, paddingVertical: 13, paddingHorizontal: 18, alignItems: 'center', justifyContent: 'center', borderCurve: 'continuous' },
  buttonText: { fontSize: 16, fontWeight: '600' },
  chip: { borderRadius: 99, borderWidth: 1, paddingHorizontal: 14, paddingVertical: 7 },
  chipText: { fontSize: 14, fontWeight: '500' },
  page: { paddingHorizontal: 16, paddingTop: 8, paddingBottom: 40 },
  empty: { alignItems: 'center', paddingHorizontal: 24, paddingTop: 48, paddingBottom: 24 },
});

/** Bottom padding for main pages so content can scroll clear of the control menu. */
export const ISLAND_CLEARANCE = 160;

/** A main page in the shell's pager: scrolling content clear of the control menu. */
export function MainPage({ children }: { children: ReactNode }) {
  const c = useColors();
  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: c.bg }}
      contentContainerStyle={[styles.page, { paddingBottom: ISLAND_CLEARANCE }]}
      keyboardShouldPersistTaps="handled">
      {children}
    </ScrollView>
  );
}
