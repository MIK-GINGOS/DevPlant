// mixes: colours from the Android app's mm_colors.xml, light and dark, and the theme setting.

import { Appearance, useColorScheme } from 'react-native';

import { readText, writeText } from '@/gates/storage';

const light = {
  bg: '#F4F7F2',
  surface: '#FFFFFF',
  ink: '#17251A',
  inkSoft: '#5D6C60',
  line: '#DDE5DA',
  leaf: '#2F7A3E',
  leafSoft: '#E2F0E1',
  healthy: '#2F7A3E',
  disease: '#B5432F',
  pest: '#8A5A12',
  unsure: '#6B6F7A',
  mild: '#5E8F3A',
  moderate: '#C98A12',
  severe: '#B5432F',
  scrim: 'rgba(0,0,0,0.6)',
  // Glass menu, from crystal_io's mm_theme.
  glassBg: 'rgba(255,255,255,0.13)',
  glassBorder: 'rgba(0,0,0,0.12)',
  glassAccent: '#09090B',
  glassOnAccent: '#F5F6F8',
  glassIdle: 'rgba(0,0,0,0.08)',
  glassIdleIcon: 'rgba(9,9,11,0.7)',
  glassLabel: 'rgba(9,9,11,0.8)',
};

const dark: typeof light = {
  bg: '#0F1611',
  surface: '#18221B',
  ink: '#E6EFE6',
  inkSoft: '#9DB0A0',
  line: '#2A382D',
  leaf: '#6BC27A',
  leafSoft: '#1F3324',
  healthy: '#6BC27A',
  disease: '#E07A63',
  pest: '#D9A54A',
  unsure: '#A3A8B3',
  mild: '#94C46B',
  moderate: '#E3B04B',
  severe: '#E07A63',
  scrim: 'rgba(0,0,0,0.6)',
  glassBg: 'rgba(24,24,28,0.13)',
  glassBorder: 'rgba(255,255,255,0.18)',
  glassAccent: '#FFFFFF',
  glassOnAccent: '#16171A',
  glassIdle: 'rgba(255,255,255,0.15)',
  glassIdleIcon: 'rgba(255,255,255,0.7)',
  glassLabel: 'rgba(255,255,255,0.8)',
};

export type Colors = typeof light;
export type ColorName = keyof Colors;

export function useColors(): Colors {
  return useColorScheme() === 'dark' ? dark : light;
}

// ---- theme setting: follow the phone, light or dark ----

export type ThemeMode = 'system' | 'light' | 'dark';
const KEY = 'devplant.theme';

function apply(mode: ThemeMode) {
  Appearance.setColorScheme(mode === 'system' ? 'unspecified' : mode);
}

export async function loadThemeMode(): Promise<ThemeMode> {
  const saved = await readText(KEY);
  const mode: ThemeMode = saved === 'light' || saved === 'dark' ? saved : 'system';
  apply(mode);
  return mode;
}

export async function setThemeMode(mode: ThemeMode) {
  apply(mode);
  await writeText(KEY, mode);
}
