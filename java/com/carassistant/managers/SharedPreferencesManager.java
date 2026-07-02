package com.carassistant.managers;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;

public class SharedPreferencesManager {

    private static final String PREFERENCES_FILE_NAME = "carassistant.shared_preferences";
    private static final String DISTANCE = "distance"; //m
    private static final String HAPTIC = "haptic_enabled";
    private static final String VOLUME = "notification_volume";
    private static final String THEME = "theme_mode";

    private SharedPreferences sharedPreferences;

    @Inject
    public SharedPreferencesManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

    public void setDistance(float distance) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(DISTANCE, distance);
        editor.apply();
    }

    public float getDistance() {
        return sharedPreferences.getFloat(DISTANCE, 0f);
    }

    public void setBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void setInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }
}
