// mixes: the control-centre menu at the bottom, ported from the Android app's
// mm_shell/ControlMenu.java. Three tiles plus More, and More opens a grid of eight. The More
// circle glides to the close button's corner while spinning, then the close button spins in
// there; grid tiles fan out from that corner and fold back into it. Same sizes, timings and
// curves as the Android version.

import { BlurView } from 'expo-blur';
import { useEffect, useState } from 'react';
import { Image, Pressable, StyleSheet, Text, useColorScheme, View, type ImageSourcePropType } from 'react-native';
import Animated, {
  Easing,
  interpolateColor,
  makeMutable,
  useAnimatedStyle,
  withDelay,
  withSequence,
  withTiming,
  type SharedValue,
} from 'react-native-reanimated';

import { useColors } from './theme';

/** One button. A tile with a tab lights up while that page shows; one with no action is not built yet. */
export interface Tile {
  label: string;
  icon: ImageSourcePropType;
  tab: number;
  action: (() => void) | null;
}

export const pageTile = (label: string, icon: ImageSourcePropType, tab: number, action: () => void): Tile => ({ label, icon, tab, action });
export const actionTile = (label: string, icon: ImageSourcePropType, action: () => void): Tile => ({ label, icon, tab: -1, action });
export const plannedTile = (label: string, icon: ImageSourcePropType): Tile => ({ label, icon, tab: -1, action: null });

const MAX_WIDTH = 380;
const TILE = 52;
const ICON = 20;
const CLOSE = 28;
const LABEL_H = 14;
const CELL_H = TILE + 6 + LABEL_H;
const ROW_GAP = 20;
const COL_GAP = 12;
const PAD_X = 24;
const PAD_Y = 20;
const GRID_TOP = 22;
const RADIUS = 36;

const EASE_IN = Easing.bezier(0.4, 0, 1, 1);
const GLIDE = Easing.bezier(0.32, 0.72, 0, 1);
const SPIN = Easing.bezier(0.4, 0, 0.2, 1);
const SPRING = Easing.bezier(0.34, 1.45, 0.64, 1);
type Curve = typeof EASE_IN;

// ---- poses: opacity, offset, scale and spin of a view at one end of a motion ----

interface Pose {
  a: number;
  x: number;
  y: number;
  s: number;
  r: number;
}

const REST: Pose = { a: 1, x: 0, y: 0, s: 1, r: 0 };
const OUT: Pose = { a: 0, x: 0, y: 6, s: 0.8, r: 0 };
const CLOSED_X: Pose = { a: 0, x: 0, y: 0, s: 0.4, r: -90 };

type Motion = { [K in keyof Pose]: SharedValue<number> };

const makeMotion = (p: Pose): Motion => ({
  a: makeMutable(p.a), x: makeMutable(p.x), y: makeMutable(p.y), s: makeMutable(p.s), r: makeMutable(p.r),
});

/** Moves a view to `to`, optionally starting from `from`, like ControlMenu.run(). */
function run(m: Motion, from: Pose | null, to: Pose, duration: number, delay: number, curve: Curve) {
  for (const k of ['a', 'x', 'y', 's', 'r'] as const) {
    const go = withDelay(delay, withTiming(to[k], { duration, easing: curve }));
    m[k].set(from ? withSequence(withTiming(from[k], { duration: 0 }), go) : go);
  }
}

function useMotionStyle(m: Motion) {
  return useAnimatedStyle(() => ({
    opacity: Math.max(0, Math.min(1, m.a.get())),
    transform: [{ translateX: m.x.get() }, { translateY: m.y.get() }, { scale: m.s.get() }, { rotate: `${m.r.get()}deg` }],
  }));
}

/** Where grid tile `index` folds to: toward the top-right corner. */
const fanPose = (index: number): Pose => ({ a: 0, x: (3 - (index % 4)) * 20, y: -Math.floor(index / 4) * 24 - 8, s: 0.6, r: 0 });

// ---- the menu ----

interface Cells {
  root: Motion[];
  lit: SharedValue<number>[];
}

const makeCells = (n: number): Cells => ({
  root: Array.from({ length: n }, () => makeMotion(REST)),
  lit: Array.from({ length: n }, () => makeMutable(0)),
});

export function ControlMenu({
  mainTiles,
  gridTiles,
  tab,
  onPlanned,
  bottom,
}: {
  mainTiles: Tile[];
  gridTiles: Tile[];
  /** The page now showing; its tile lights up. */
  tab: number;
  /** A tile that is not built yet was tapped. */
  onPlanned: (label: string) => void;
  bottom: number;
}) {
  const c = useColors();
  const dark = useColorScheme() === 'dark';
  const moreTile: Tile = { label: 'More', icon: require('@/assets/icons/cc_more.png'), tab: -1, action: null };
  const main = [...mainTiles, moreTile];

  // Animated values live as long as the menu, outside React state.
  const [m] = useState(() => ({
    main: makeCells(main.length),
    grid: makeCells(gridTiles.length),
    moreCircle: makeMotion(REST),
    moreIcon: makeMotion(REST),
    moreLabel: makeMutable(1),
    close: makeMotion(CLOSED_X),
    height: makeMutable(CELL_H),
    // Read only in handlers, like the Android fields of the same names.
    expanded: makeMutable(false),
    animating: makeMutable(false),
    timer: makeMutable<ReturnType<typeof setTimeout> | null>(null),
  }));
  const [showing, setShowing] = useState<'main' | 'full'>('main');
  const [width, setWidth] = useState(0);
  useEffect(() => () => {
    const t = m.timer.get();
    if (t) clearTimeout(t);
  }, [m]);

  // Light the tile for the page now showing.
  useEffect(() => {
    main.forEach((t, i) => m.main.lit[i].set(withTiming(t.tab >= 0 && t.tab === tab ? 1 : 0, { duration: 300 })));
    gridTiles.forEach((t, i) => m.grid.lit[i].set(withTiming(t.tab >= 0 && t.tab === tab ? 1 : 0, { duration: 200 })));
  });

  /** The More circle shrunk onto the close button's spot. */
  function cornerPose(alpha: number): Pose {
    const colW = (width - 2 * PAD_X - 3 * COL_GAP) / 4;
    const atX = PAD_X + 3 * (colW + COL_GAP) + colW / 2;
    const atY = PAD_Y + TILE / 2;
    const cornerX = width - 12 - CLOSE / 2;
    const cornerY = 12 + CLOSE / 2;
    return { a: alpha, x: cornerX - atX, y: cornerY - atY, s: CLOSE / TILE, r: 0 };
  }

  function phase(ms: number) {
    const t = m.timer.get();
    if (t) clearTimeout(t);
    m.timer.set(setTimeout(finishPhase, ms));
  }

  function expand() {
    if (m.animating.get() || m.expanded.get()) return;
    m.animating.set(true);
    for (let i = 0; i < main.length - 1; i++) run(m.main.root[i], REST, OUT, 260, (2 - i) * 40, EASE_IN);
    run(m.moreCircle, REST, cornerPose(0.9), 450, 0, GLIDE);
    m.moreIcon.r.set(withTiming(270, { duration: 450, easing: SPIN }));
    m.moreIcon.a.set(withSequence(withTiming(1, { duration: 315 }), withTiming(0, { duration: 135, easing: SPIN })));
    m.moreLabel.set(withTiming(0, { duration: 200 }));
    phase(450);
  }

  function collapse() {
    if (m.animating.get() || !m.expanded.get()) return;
    m.animating.set(true);
    const n = gridTiles.length;
    for (let i = 0; i < n; i++) run(m.grid.root[i], REST, fanPose(i), 240, (n - 1 - i) * 20, EASE_IN);
    run(m.close, REST, CLOSED_X, 220, 0, EASE_IN);
    phase(400);
  }

  /** Swaps the grids once the outgoing tiles are gone, and plays the incoming ones. */
  function finishPhase() {
    if (!m.expanded.get()) {
      m.expanded.set(true);
      gridTiles.forEach((_, i) => {
        const col = i % 4;
        const row = Math.floor(i / 4);
        run(m.grid.root[i], fanPose(i), REST, 520, 60 + (3 - col) * 35 + row * 60, SPRING);
      });
      run(m.close, CLOSED_X, REST, 420, 0, SPRING);
      const rows = Math.ceil(gridTiles.length / 4);
      m.height.set(withTiming(GRID_TOP + rows * CELL_H + (rows - 1) * ROW_GAP, { duration: 500, easing: GLIDE }));
      setShowing('full');
    } else {
      m.expanded.set(false);
      for (let i = 0; i < main.length - 1; i++) run(m.main.root[i], OUT, REST, 460, 120 + (2 - i) * 45, SPRING);
      m.moreLabel.set(1);
      run(m.moreCircle, cornerPose(0), REST, 500, 0, GLIDE);
      run(m.moreIcon, { a: 0, x: 0, y: 0, s: 1, r: 270 }, REST, 500, 0, GLIDE);
      m.height.set(withTiming(CELL_H, { duration: 500, easing: GLIDE }));
      setShowing('main');
    }
    m.animating.set(false);
  }

  function onTap(t: Tile) {
    if (m.animating.get()) return;
    if (t.label === 'More' && !t.action) return expand();
    if (!t.action) return onPlanned(t.label);
    t.action();
    if (m.expanded.get()) collapse();
  }

  const heightStyle = useAnimatedStyle(() => ({ height: m.height.get() }));
  const closeStyle = useMotionStyle(m.close);

  const grid = (tiles: Tile[], cells: Cells, isMain: boolean) => {
    const rows: Tile[][] = [];
    for (let i = 0; i < tiles.length; i += 4) rows.push(tiles.slice(i, i + 4));
    return (
      <View style={{ paddingTop: isMain ? 0 : GRID_TOP, gap: ROW_GAP }}>
        {rows.map((row, r) => (
          <View key={r} style={{ flexDirection: 'row', gap: COL_GAP }}>
            {row.map((t, j) => {
              const i = r * 4 + j;
              const more = isMain && i === tiles.length - 1;
              return (
                <Cell
                  key={t.label}
                  tile={t}
                  root={cells.root[i]}
                  lit={cells.lit[i]}
                  circle={more ? m.moreCircle : undefined}
                  icon={more ? m.moreIcon : undefined}
                  label={more ? m.moreLabel : undefined}
                  onPress={() => onTap(t)}
                />
              );
            })}
            {/* Keep partial rows in four columns. */}
            {Array.from({ length: 4 - row.length }, (_, k) => <View key={`gap${k}`} style={{ flex: 1 }} />)}
          </View>
        ))}
      </View>
    );
  };

  return (
    <View pointerEvents="box-none" style={[styles.dock, { bottom }]}>
      <View style={[styles.shadow, { maxWidth: MAX_WIDTH }]} onLayout={e => setWidth(e.nativeEvent.layout.width)}>
        <View style={styles.card}>
          {/* Frosted glass: the page blurred behind the card, then a faint tint and hairline. */}
          <BlurView intensity={dark ? 50 : 60} tint={dark ? 'dark' : 'light'} style={StyleSheet.absoluteFill} />
          <View style={[StyleSheet.absoluteFill, styles.tint, { backgroundColor: c.glassBg, borderColor: c.glassBorder }]} />
          <View style={{ paddingHorizontal: PAD_X, paddingVertical: PAD_Y }}>
            <Animated.View style={heightStyle}>
              {showing === 'main' ? grid(main, m.main, true) : grid(gridTiles, m.grid, false)}
            </Animated.View>
          </View>
          {showing === 'full' && (
            <Animated.View style={[styles.close, closeStyle]}>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Close menu"
                onPress={collapse}
                hitSlop={8}
                style={({ pressed }) => [styles.closeButton, { backgroundColor: c.glassIdle }, pressed && { transform: [{ scale: 0.9 }] }]}>
                <Image source={require('@/assets/icons/cc_close_more.png')} style={{ width: 14, height: 14, tintColor: c.glassAccent }} />
              </Pressable>
            </Animated.View>
          )}
        </View>
      </View>
    </View>
  );
}

/** One tile: a round icon and a label underneath. */
function Cell({
  tile,
  root,
  lit,
  circle,
  icon,
  label,
  onPress,
}: {
  tile: Tile;
  root: Motion;
  lit: SharedValue<number>;
  circle?: Motion;
  icon?: Motion;
  label?: SharedValue<number>;
  onPress: () => void;
}) {
  const c = useColors();
  const [pressed] = useState(() => makeMutable(1));
  const [still] = useState(() => makeMotion(REST));
  const rootStyle = useMotionStyle(root);
  const pressStyle = useAnimatedStyle(() => ({ transform: [{ scale: pressed.get() }] }));
  const circleMotion = useMotionStyle(circle ?? still);
  const iconMotion = useMotionStyle(icon ?? still);
  const discStyle = useAnimatedStyle(() => ({
    backgroundColor: interpolateColor(lit.get(), [0, 1], [c.glassIdle, c.glassAccent]),
    shadowOpacity: lit.get() * 0.35,
  }));
  const tintStyle = useAnimatedStyle(() => ({
    tintColor: interpolateColor(lit.get(), [0, 1], [c.glassIdleIcon, c.glassOnAccent]),
  }));
  const labelStyle = useAnimatedStyle(() => ({ opacity: label ? label.get() : 1 }));

  return (
    <Animated.View style={[{ flex: 1, alignItems: 'center' }, rootStyle]}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={tile.label}
        onPress={onPress}
        onPressIn={() => pressed.set(withTiming(0.9, { duration: 150 }))}
        onPressOut={() => pressed.set(withTiming(1, { duration: 150 }))}
        style={{ alignItems: 'center' }}>
        <Animated.View style={[{ alignItems: 'center' }, pressStyle]}>
          <Animated.View style={[styles.disc, { shadowColor: c.glassAccent }, discStyle, circleMotion]}>
            <Animated.Image source={tile.icon} style={[{ width: ICON, height: ICON }, tintStyle, iconMotion]} />
          </Animated.View>
          <Animated.Text numberOfLines={1} style={[styles.label, { color: c.glassLabel }, labelStyle]}>
            {tile.label}
          </Animated.Text>
        </Animated.View>
      </Pressable>
    </Animated.View>
  );
}

/** A short message above the menu, like Android's Toast. */
export function Toast({ text, bottom }: { text: string | null; bottom: number }) {
  const c = useColors();
  if (!text) return null;
  return (
    <View pointerEvents="none" style={[styles.toastWrap, { bottom }]}>
      <View style={[styles.toast, { backgroundColor: c.ink }]}>
        <Text style={{ color: c.bg, fontSize: 14, fontWeight: '500' }}>{text}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  dock: { position: 'absolute', left: 16, right: 16, alignItems: 'center' },
  shadow: {
    width: '100%',
    borderRadius: RADIUS,
    shadowColor: '#000000',
    shadowOpacity: 0.18,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 6 },
    elevation: 8,
  },
  card: { borderRadius: RADIUS, overflow: 'hidden', borderCurve: 'continuous' },
  tint: { borderRadius: RADIUS, borderWidth: 1, borderCurve: 'continuous' },
  disc: {
    width: TILE,
    height: TILE,
    borderRadius: TILE / 2,
    alignItems: 'center',
    justifyContent: 'center',
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 3 },
  },
  label: { marginTop: 6, fontSize: 11, lineHeight: LABEL_H, fontWeight: '500', maxWidth: 70, textAlign: 'center' },
  close: { position: 'absolute', top: 12, right: 12 },
  closeButton: { width: CLOSE, height: CLOSE, borderRadius: CLOSE / 2, alignItems: 'center', justifyContent: 'center' },
  toastWrap: { position: 'absolute', left: 0, right: 0, alignItems: 'center' },
  toast: { borderRadius: 99, paddingHorizontal: 16, paddingVertical: 10 },
});
