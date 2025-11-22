package com.example.yellow.utils;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages device-based identity for Admin privileges.
 * <p>
 * This allows an Admin to maintain their role even if the App Data is cleared
 * (which resets the Anonymous UID).
 * The logic relies on Settings.Secure.ANDROID_ID, which persists across app
 * re-installs on the same device/emulator.
 */
public class DeviceIdentityManager {

    private static final String TAG = "DeviceIdentity";
    private static final String COLLECTION_DEVICES = "devices";

    // In-memory cache for admin status to avoid repeated Firestore reads
    private static boolean isAdminCache = false;
    private static boolean isCacheLoaded = false;

    /**
     * Returns the unique Android Device ID.
     */
    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Ensures the current device is registered in Firestore.
     * <p>
     * 1. Gets Device ID.
     * 2. Updates 'devices/{deviceId}' with the current User ID and timestamp.
     * 3. Reads the 'isAdmin' flag from the device document.
     * 4. Updates the local cache.
     */
    public static Task<Boolean> ensureDeviceDocument(Context context, FirebaseUser user) {
        if (user == null)
            return Tasks.forResult(false);

        String deviceId = getDeviceId(context);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d(TAG, "Ensuring device doc for ID: " + deviceId);

        // Data to update
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getUid());
        data.put("lastSeen", FieldValue.serverTimestamp());

        // Create if not exists, merge if exists
        // We use merge() so we don't overwrite an existing 'isAdmin' flag
        return db.collection(COLLECTION_DEVICES).document(deviceId)
                .set(data, SetOptions.merge())
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Failed to write device doc", task.getException());
                        return Tasks.forResult(false);
                    }
                    // Now read back to check admin status
                    return db.collection(COLLECTION_DEVICES).document(deviceId).get()
                            .continueWith(readTask -> {
                                if (readTask.isSuccessful() && readTask.getResult() != null) {
                                    DocumentSnapshot doc = readTask.getResult();
                                    // Handle both Boolean and String "true" for robustness
                                    Object adminField = doc.get("isAdmin");
                                    boolean isAdmin = false;
                                    if (adminField instanceof Boolean) {
                                        isAdmin = (Boolean) adminField;
                                    } else if (adminField instanceof String) {
                                        isAdmin = "true".equalsIgnoreCase((String) adminField);
                                    }

                                    isAdminCache = isAdmin;
                                    isCacheLoaded = true;
                                    Log.d(TAG, "Device " + deviceId + " isAdmin: " + isAdminCache);
                                    return isAdminCache;
                                } else {
                                    Log.w(TAG, "Failed to read device doc", readTask.getException());
                                    return false;
                                }
                            });
                });
    }

    /**
     * Checks if the current device has Admin privileges.
     * Uses cached value if available.
     */
    public static boolean isCurrentDeviceAdmin() {
        if (!isCacheLoaded) {
            Log.w(TAG, "Admin check requested before cache loaded. Defaulting to false.");
        }
        return isAdminCache;
    }

    /**
     * Force refresh the admin status from Firestore.
     * Useful if permissions change while app is running.
     */
    public static void refreshAdminStatus(Context context) {
        String deviceId = getDeviceId(context);
        FirebaseFirestore.getInstance().collection(COLLECTION_DEVICES).document(deviceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean isAdmin = doc.getBoolean("isAdmin");
                        isAdminCache = isAdmin != null && isAdmin;
                        isCacheLoaded = true;
                    }
                });
    }
}
