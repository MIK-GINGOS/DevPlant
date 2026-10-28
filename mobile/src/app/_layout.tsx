import { DarkTheme, DefaultTheme, Stack, ThemeProvider } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { useColorScheme } from 'react-native';

import { useLoaded } from '@/bridge/store';
import { loadThemeMode, useColors } from '@/mixes/theme';

SplashScreen.preventAutoHideAsync();
loadThemeMode();

/** The shell underneath; diagnosis, guide entries, plants, alerts, search, settings, reports, welcome and help open on top. */
export default function RootLayout() {
  const scheme = useColorScheme();
  const c = useColors();
  const loaded = useLoaded();

  useEffect(() => {
    if (loaded) SplashScreen.hideAsync();
  }, [loaded]);

  const base = scheme === 'dark' ? DarkTheme : DefaultTheme;
  const theme = {
    ...base,
    colors: { ...base.colors, primary: c.leaf, background: c.bg, card: c.bg, text: c.ink, border: c.line },
  };

  return (
    <ThemeProvider value={theme}>
      <StatusBar style="auto" />
      <Stack screenOptions={{ headerBackButtonDisplayMode: 'minimal', headerTintColor: c.leaf, headerTitleStyle: { color: c.ink } }}>
        <Stack.Screen name="index" options={{ headerShown: false }} />
        <Stack.Screen name="result/[id]" options={{ title: 'Diagnosis' }} />
        <Stack.Screen name="guide/[key]" options={{ title: 'Guide' }} />
        <Stack.Screen name="plant/[key]" options={{ title: 'Plant catalogue' }} />
        <Stack.Screen name="alerts" options={{ title: 'Alerts' }} />
        <Stack.Screen name="search" options={{ title: 'Search' }} />
        <Stack.Screen name="settings" options={{ title: 'Settings' }} />
        <Stack.Screen name="reports" options={{ title: 'My reports' }} />
        <Stack.Screen name="welcome" options={{ title: 'Welcome' }} />
        <Stack.Screen name="help" options={{ title: 'Ask an officer' }} />
        <Stack.Screen name="add-field" options={{ title: 'Add a field', presentation: 'formSheet', sheetAllowedDetents: [0.75, 1], sheetGrabberVisible: true }} />
      </Stack>
    </ThemeProvider>
  );
}
