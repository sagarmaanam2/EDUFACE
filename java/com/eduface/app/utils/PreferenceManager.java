package com.eduface.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class for managing user preferences and session data
 */
public class PreferenceManager {

    private static final String PREF_NAME = "EduFacePrefs";
    private static final String KEY_USER_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    // Added key for total meetings attended
    private static final String KEY_TOTAL_MEETINGS_ATTENDED = "total_meetings_attended";

    private SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Save user session data
     */
    public void saveUserSession(String userId, String name, String email, String role) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_USER_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        // When a new session is saved, you might want to reset or load
        // the attendance count associated with this user ID if storing per user
        // For simplicity here, we'll assume the count is global or handled elsewhere initially.
        editor.apply();
    }

    /**
     * Clear user session data
     */
    public void clearUserSession() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_USER_LOGGED_IN, false);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_ROLE);
        // You might choose to keep the attendance count or remove it here
        // depending on whether it's tied to the session or the user ID.
        // For now, we won't remove the attendance count on session clear.
        editor.apply();
    }

    /**
     * Check if user is logged in
     */
    public boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false);
    }

    /**
     * Get current user ID
     */
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    /**
     * Get current user name
     */
    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, null);
    }

    /**
     * Get current user email
     */
    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Get current user role
     */
    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, null);
    }

    /**
     * Check if user is a teacher
     */
    public boolean isTeacher() {
        return "teacher".equals(getUserRole());
    }

    /**
     * Save a boolean preference
     */
    public void saveBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Get a boolean preference
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    /**
     * Save a string preference
     */
    public void saveString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Get a string preference
     */
    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    /**
     * Save an integer preference
     */
    public void saveInt(String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Get an integer preference
     */
    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    /**
     * Remove a preference
     */
    public void remove(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    // --- Methods for Total Meetings Attended Count ---

    /**
     * Get the total number of meetings attended.
     * Returns 0 if the count is not set.
     */
    public int getTotalMeetingsAttended() {
        return sharedPreferences.getInt(KEY_TOTAL_MEETINGS_ATTENDED, 0);
    }

    /**
     * Increment the total number of meetings attended by 1.
     */
    public void incrementTotalMeetingsAttended() {
        int currentCount = getTotalMeetingsAttended();
        saveInt(KEY_TOTAL_MEETINGS_ATTENDED, currentCount + 1);
    }

    /**
     * Reset the total number of meetings attended to 0.
     * Useful for testing or if attendance is tied to user ID and needs reset on login.
     */
    public void resetTotalMeetingsAttended() {
        saveInt(KEY_TOTAL_MEETINGS_ATTENDED, 0);
    }
}
