import { router, useFocusEffect } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import { View } from 'react-native';
import PagerView from 'react-native-pager-view';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useConsent, useReportsLoaded } from '@/bridge/reports';
import { actionTile, ControlMenu, pageTile, plannedTile, Toast } from '@/mixes/controlMenu';
import { AppHeader } from '@/mixes/header';
import {
  registerShell, showTab, SUBTITLES, TAB_CATALOG, TAB_DASHBOARD, TAB_FIELDS, TAB_SCAN,
} from '@/mixes/nav';
import CatalogPage from '@/mixes/pages/catalog';
import DashboardPage from '@/mixes/pages/dashboard';
import FieldsPage from '@/mixes/pages/fields';
import HistoryPage from '@/mixes/pages/history';
import LibraryPage from '@/mixes/pages/library';
import ScanPage from '@/mixes/pages/scan';
import { useColors } from '@/mixes/theme';

/** The one main screen, like ShellActivity: header on top, pager of main screens, the menu at the bottom. */
export default function Shell() {
  const c = useColors();
  const insets = useSafeAreaInsets();
  const pager = useRef<PagerView>(null);
  const [tab, setTab] = useState(TAB_DASHBOARD);
  const [focused, setFocused] = useState(true);
  const [toast, setToast] = useState<string | null>(null);
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // First launch: the welcome screen asks about sharing before anything else.
  const consent = useConsent();
  const reportsLoaded = useReportsLoaded();
  useEffect(() => {
    if (reportsLoaded && !consent.decided) router.push('/welcome');
    // Only when the saved choice has been read, not each time it changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reportsLoaded]);

  // Other screens and the menu tiles switch pages through nav.showTab.
  useEffect(() => {
    registerShell(t => {
      pager.current?.setPageWithoutAnimation(t);
      setTab(t);
    });
    return () => registerShell(null);
  }, []);

  // The camera page only runs while the shell is on screen, not under a detail screen.
  useFocusEffect(
    useCallback(() => {
      setFocused(true);
      return () => setFocused(false);
    }, []),
  );

  function planned(label: string) {
    if (toastTimer.current) clearTimeout(toastTimer.current);
    setToast(`${label} is coming soon`);
    toastTimer.current = setTimeout(() => setToast(null), 2000);
  }

  const menuBottom = insets.bottom + 16;

  return (
    <View style={{ flex: 1, backgroundColor: c.bg }}>
      <AppHeader subtitle={SUBTITLES[tab]} />
      <PagerView
        ref={pager}
        style={{ flex: 1 }}
        initialPage={TAB_DASHBOARD}
        offscreenPageLimit={1}
        onPageSelected={e => setTab(e.nativeEvent.position)}>
        <View key="scan" style={{ flex: 1 }}><ScanPage active={focused && tab === TAB_SCAN} /></View>
        <View key="history" style={{ flex: 1 }}><HistoryPage /></View>
        <View key="fields" style={{ flex: 1 }}><FieldsPage /></View>
        <View key="library" style={{ flex: 1 }}><LibraryPage /></View>
        <View key="dashboard" style={{ flex: 1 }}><DashboardPage /></View>
        <View key="catalog" style={{ flex: 1 }}><CatalogPage /></View>
      </PagerView>

      {/* Tiles without an action are not built yet; they say so when tapped. History and the guide
          have no tile, as on Android: swipe to reach them. */}
      <ControlMenu
        tab={tab}
        bottom={menuBottom}
        onPlanned={planned}
        mainTiles={[
          pageTile('Home', require('@/assets/icons/cc_home.png'), TAB_DASHBOARD, () => showTab(TAB_DASHBOARD)),
          plannedTile('Security', require('@/assets/icons/cc_shield.png')),
          pageTile('Fields', require('@/assets/icons/cc_space.png'), TAB_FIELDS, () => showTab(TAB_FIELDS)),
        ]}
        gridTiles={[
          actionTile('Alerts', require('@/assets/icons/cc_bell_line.png'), () => router.push('/alerts')),
          plannedTile('Location', require('@/assets/icons/cc_location.png')),
          actionTile('Reports', require('@/assets/icons/cc_share.png'), () => router.push('/reports')),
          actionTile('Settings', require('@/assets/icons/cc_sliders.png'), () => router.push('/settings')),
          pageTile('Camera', require('@/assets/icons/cc_camera.png'), TAB_SCAN, () => showTab(TAB_SCAN)),
          plannedTile('Add', require('@/assets/icons/cc_add.png')),
          pageTile('Catalog', require('@/assets/icons/cc_catalog.png'), TAB_CATALOG, () => showTab(TAB_CATALOG)),
          plannedTile('Active Profile', require('@/assets/icons/cc_profile.png')),
        ]}
      />
      <Toast text={toast} bottom={menuBottom + 130} />
    </View>
  );
}
