// mixes: logo, title, and the search and alerts buttons, after the Android app's mm_shell/Header.java.

import { Image } from 'expo-image';
import { router } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useUnreadCount } from '@/bridge/store';

import { useColors } from './theme';
import { Txt } from './ui';

export function AppHeader({ subtitle }: { subtitle: string }) {
  const c = useColors();
  const top = useSafeAreaInsets().top;
  const unread = useUnreadCount();
  return (
    <View style={[styles.bar, { paddingTop: top + 8, backgroundColor: c.bg }]}>
      <Image source={require('@/assets/images/logo.png')} style={styles.logo} accessibilityIgnoresInvertColors />
      <View style={{ flex: 1, paddingLeft: 10 }}>
        <Txt type="heading">DevPlant</Txt>
        <Txt type="caption">{subtitle}</Txt>
      </View>
      <Pressable accessibilityLabel="Search" hitSlop={4} style={styles.button} onPress={() => router.push('/search')}>
        <Image source={require('@/assets/icons/search.png')} style={styles.icon} tintColor={c.ink} />
      </Pressable>
      <Pressable accessibilityLabel={`Alerts, ${unread} new`} hitSlop={4} style={styles.button} onPress={() => router.push('/alerts')}>
        <Image source={require('@/assets/icons/bell.png')} style={styles.icon} tintColor={c.ink} />
        {unread > 0 && (
          <View style={[styles.badge, { backgroundColor: c.severe }]}>
            <Text style={[styles.badgeText, { color: c.surface }]}>{unread > 99 ? '99+' : unread}</Text>
          </View>
        )}
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { flexDirection: 'row', alignItems: 'center', paddingLeft: 16, paddingRight: 8, paddingBottom: 8 },
  logo: { width: 34, height: 34 },
  button: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  icon: { width: 24, height: 24 },
  badge: { position: 'absolute', top: 6, right: 4, minWidth: 16, height: 16, borderRadius: 8, paddingHorizontal: 4, alignItems: 'center', justifyContent: 'center' },
  badgeText: { fontSize: 10 },
});
