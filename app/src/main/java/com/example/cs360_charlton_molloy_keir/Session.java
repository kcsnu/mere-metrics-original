package com.example.cs360_charlton_molloy_keir;

import android.content.Context;
import android.content.SharedPreferences;

public final class Session {

    // Name of the SharedPreferences file
    private static final String PREFS_NAME = "cs360_prefs";

    // Key used to store the currently logged-in user id
    private static final String KEY_USER_ID = "current_user_id";

    // Prefix keys used to store SMS settings per userId
    private static final String KEY_SMS_ENABLED_PREFIX = "sms_enabled_user_";
    private static final String KEY_SMS_PHONE_NUMBER_PREFIX = "sms_phone_number_user_";

    // Prefix key used to prevent sending duplicate "goal reached" texts per userId
    private static final String KEY_LAST_GOAL_NOTIFIED_PREFIX = "last_goal_notified_user_";

    // Private constructor to prevent creating instances of this utility class
    private Session() { }

    // Get the app's SharedPreferences instance
    private static SharedPreferences prefs(Context context) {
        // Use application context to avoid leaking an Activity/Fragment context
        Context appContext = context.getApplicationContext();
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Build a unique key for a specific userId
    private static String keyForUser(String prefix, long userId) {
        return prefix + userId;
    }

    // Function to check if a userId value is invalid
    private static boolean isInvalidUserId(long userId) {
        // Database AUTOINCREMENT ids start at 1, and -1 means "not logged in"
        return userId <= 0;
    }

    // Save the logged-in user's id
    public static void storeLoggedInUser(Context context, long userId) {
        prefs(context).edit()
                .putLong(KEY_USER_ID, userId)
                .apply();
    }

    // Read the currently logged-in user's id (-1 means "no user logged in")
    public static long getLoggedInUserId(Context context) {
        return prefs(context).getLong(KEY_USER_ID, -1);
    }

    // Check if SMS alerts are enabled for a specific userId
    public static boolean isSmsEnabled(Context context, long userId) {

        // If userId is invalid, SMS alerts are off
        if (isInvalidUserId(userId)) {
            return false;
        }

        // Read the per-user SMS enabled toggle (default false)
        return prefs(context).getBoolean(keyForUser(KEY_SMS_ENABLED_PREFIX, userId), false);
    }

    // Turn SMS alerts on/off for a specific userId
    public static void setSmsEnabled(Context context, long userId, boolean enabled) {

        // If userId is invalid, do nothing
        if (isInvalidUserId(userId)) {
            return;
        }

        // Save the per-user SMS enabled toggle
        prefs(context).edit()
                .putBoolean(keyForUser(KEY_SMS_ENABLED_PREFIX, userId), enabled)
                .apply();
    }

    // Get the destination phone number for a specific userId
    public static String getSmsPhoneNumber(Context context, long userId) {

        // If userId is invalid, return empty
        if (isInvalidUserId(userId)) {
            return "";
        }

        // Read the per-user destination number (default empty)
        return prefs(context).getString(keyForUser(KEY_SMS_PHONE_NUMBER_PREFIX, userId), "");
    }

    // Save the destination phone number for a specific userId
    public static void setSmsPhoneNumber(Context context, long userId, String phoneNumber) {

        // If userId is invalid, do nothing
        if (isInvalidUserId(userId)) {
            return;
        }

        // Normalize before storing (SmsUtil handles validation)
        String safePhone = phoneNumber == null ? "" : phoneNumber.trim();

        // Save the per-user destination number
        prefs(context).edit()
                .putString(keyForUser(KEY_SMS_PHONE_NUMBER_PREFIX, userId), safePhone)
                .apply();
    }

    // Check if we already sent a "goal reached" text for this goalWeight
    public static boolean wasGoalAlreadyNotified(Context context, long userId, double goalWeight) {

        // If userId is invalid, treat as not notified
        if (isInvalidUserId(userId)) {
            return false;
        }

        String key = keyForUser(KEY_LAST_GOAL_NOTIFIED_PREFIX, userId);

        // Read the stored value (Long.MIN_VALUE means "never notified")
        long stored = prefs(context).getLong(key, Long.MIN_VALUE);

        // Compare using raw bits to avoid rounding/formatting issues
        return stored == Double.doubleToLongBits(goalWeight);
    }

    // Mark this goalWeight as already notified
    public static void markGoalNotified(Context context, long userId, double goalWeight) {

        // If userId is invalid, do nothing
        if (isInvalidUserId(userId)) {
            return;
        }

        String key = keyForUser(KEY_LAST_GOAL_NOTIFIED_PREFIX, userId);

        // Store the goal using raw bits for exact comparison later
        prefs(context).edit()
                .putLong(key, Double.doubleToLongBits(goalWeight))
                .apply();
    }

    // Clear the "already notified" marker
    public static void clearGoalNotified(Context context, long userId) {

        // If userId is invalid, do nothing
        if (isInvalidUserId(userId)) {
            return;
        }

        String key = keyForUser(KEY_LAST_GOAL_NOTIFIED_PREFIX, userId);

        // Remove the stored marker
        prefs(context).edit().remove(key).apply();
    }
}