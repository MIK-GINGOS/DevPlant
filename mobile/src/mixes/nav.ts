// mixes: how screens move between each other, after the Android app's mm_shell/Nav.java.
// The shell's pages are swiped or picked from the control menu; details open on top of them.

import { router } from 'expo-router';

export const TAB_SCAN = 0;
export const TAB_HISTORY = 1;
export const TAB_FIELDS = 2;
export const TAB_LIBRARY = 3;
export const TAB_DASHBOARD = 4;
export const TAB_CATALOG = 5;

export const SUBTITLES = ['Scan a leaf', 'Past scans', 'My crops and fields', 'Pest & disease guide', 'Field health', 'Plant catalogue'];

let shell: ((tab: number) => void) | null = null;

/** The shell registers here so any screen can switch its page. */
export function registerShell(show: ((tab: number) => void) | null) {
  shell = show;
}

/** Closes any detail screens and shows one of the main pages. */
export function showTab(tab: number) {
  if (router.canDismiss()) router.dismissAll();
  shell?.(tab);
}
