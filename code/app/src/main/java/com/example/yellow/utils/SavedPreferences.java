package com.example.yellow.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Class for accessing SharedPreferences.
 * SharedPreferences writes simple data to local device storage.
 * The idea is that we access this class without instantiating.
 * We can use this to verify login without a login page.
 * Not recommended for passwords.
 *
 * @author mdlau
 */
public class SavedPreferences {
    private static SharedPreferences sPref;
    private static SharedPreferences.Editor editor;
    /**
     * These are attributes to be used to read/write certain key values in the preference file
     */
    public static final String USER_NAME = "user";
    public static final String EMAIL = "email";
    public static final String PHONE_NUMBER = "phone_number";

    // No Constructor required
    private SavedPreferences() {}

    /**
     * Initializes a SharedPreference object with the name of our app
     * package as the file to read/write from
     *
     * @param ctx The Context of the app
     */
    public static void getInstance(Context ctx) {
        if(sPref == null)
            // MODE_PRIVATE ensures only the app has access the preferences file
            sPref = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
    }

    /**
     * Adds a key value pair in the preferences file
     *
     * @param key Name of the key
     * @param value Value associated with the key
     */
    public static void setString(String key, String value) {
        editor.putString(key, value);
        editor.commit();
    }

    /**
     * Retrieve a String value from the preference file
     *
     * @param key Name of the String key
     * @param defValue A default value for if the key does not exist.
     *                 Can specify it based on what we're looking for.
     * @return Value of the String key
     */
    public static String getString(String key, String defValue) {
        return sPref.getString(key, defValue);
    }

    /**
     * Sets a boolean key value in the preference file.
     * We can use this to set a loggedIn value to remember the user
     *
     * @param key Name of the boolean key
     * @param value Value of the boolean key value
     */
    public static void setBool(String key, Boolean value) {
        editor.putBoolean(key, value);
        editor.commit();
    }

    /**
     * Return boolean value from preferences
     *
     * @param key Name of boolean key in preference file
     * @return The value of the boolean key
     */
    public static Boolean getBool(String key) {
        return sPref.getBoolean(key, false);
    }

    /**
     * Clears preferences, including username, email, phone number, and loggedIn.
     */
    public static void clearPrefs() {
        editor.clear().commit();
    }
}
