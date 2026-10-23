package com.mixesgates.devplant.mixes.mm_settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Light, dark or follow the phone. */
public final class ThemePrefs {

    public static final String SYSTEM = "system";
    public static final String LIGHT = "light";
    public static final String DARK = "dark";

    private static final String FILE = "mm_settings";
    private static final String KEY = "theme";

    private ThemePrefs() {
    }

    public static String get(Context c) {
        return prefs(c).getString(KEY, SYSTEM);
    }

    public static void set(Context c, String mode) {
        prefs(c).edit().putString(KEY, mode).apply();
        apply(c);
    }

    public static void apply(Context c) {
        int mode;
        switch (get(c)) {
            case LIGHT: mode = AppCompatDelegate.MODE_NIGHT_NO; break;
            case DARK: mode = AppCompatDelegate.MODE_NIGHT_YES; break;
            default: mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; break;
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) AppCompatDelegate.setDefaultNightMode(mode);
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
