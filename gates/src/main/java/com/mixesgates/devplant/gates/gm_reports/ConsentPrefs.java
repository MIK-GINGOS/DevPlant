package com.mixesgates.devplant.gates.gm_reports;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/** The farmer's sharing choice, in a small preferences file. */
public final class ConsentPrefs {

    private final SharedPreferences prefs;

    public ConsentPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("devplant_consent", Context.MODE_PRIVATE);
    }

    public boolean decided() {
        return prefs.getBoolean("decided", false);
    }

    public boolean share() {
        return prefs.getBoolean("share", false);
    }

    public String country() {
        return prefs.getString("country", null);
    }

    public String district() {
        return prefs.getString("district", null);
    }

    /**
     * A random key that tells the officer service which reports are this phone's, so it can send
     * back their statuses and answers. Made once; it says nothing about the farmer.
     */
    public synchronized String deviceKey() {
        String key = prefs.getString("device_key", null);
        if (key == null) {
            key = UUID.randomUUID().toString();
            prefs.edit().putString("device_key", key).apply();
        }
        return key;
    }

    public void save(boolean share, String country, String district) {
        prefs.edit()
                .putBoolean("decided", true)
                .putBoolean("share", share)
                .putString("country", country)
                .putString("district", district)
                .apply();
    }
}
