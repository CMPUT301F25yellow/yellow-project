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
 * 
 * @author Tabrez
 */
public class DeviceIdentityManager {

    private static final String TAG = "DeviceIdentity";
    private static final String COLLECTION_DEVICES = "devices";

    // In-memory cache for admin status to avoid repeated Firestore reads
    private static boolean isAdminCache = false;
    private static boolean isCacheLoaded = false;

    /**
     * Returns the unique Android Device ID.
     * 
     * @param context The application context
     * @return The unique ANDROID_ID string
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
     * 
     * @param context The application context
     * @param user    The currently signed-in Firebase user
     * @return A Task resolving to true if the device is an admin, false otherwise
     */
    public static Task<Boolean> ensureDeviceDocument(Context context, FirebaseUser user) {
        if (context == null || user == null) {
            return Tasks.forResult(false);
        }

        String deviceId = getDeviceId(context);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d(TAG, "Ensuring device doc for ID: " + deviceId);

        // First, check if the document exists
        return db.collection(COLLECTION_DEVICES).document(deviceId)
                .get()
                .continueWithTask(getTask -> {
                    if (!getTask.isSuccessful()) {
                        Log.e(TAG, "Failed to check device doc existence", getTask.getException());
                        return Tasks.forResult(false);
                    }

                    DocumentSnapshot snapshot = getTask.getResult();

                    if (snapshot != null && snapshot.exists()) {
                        // Document exists: only update uid and lastSeen, preserve isAdmin
                        Log.d(TAG, "Device doc exists, updating uid and lastSeen only");
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("uid", user.getUid());
                        updateData.put("lastSeen", FieldValue.serverTimestamp());

                        return db.collection(COLLECTION_DEVICES).document(deviceId)
                                .update(updateData)
                                .continueWith(updateTask -> {
                                    if (!updateTask.isSuccessful()) {
                                        Log.e(TAG, "Failed to update device doc", updateTask.getException());
                                        return false;
                                    }

                                    // Read back the isAdmin value
                                    Boolean isAdminObj = snapshot.getBoolean("isAdmin");
                                    boolean isAdmin = Boolean.TRUE.equals(isAdminObj);

                                    isAdminCache = isAdmin;
                                    isCacheLoaded = true;
                                    Log.d(TAG, "Device " + deviceId + " isAdmin: " + isAdminCache);
                                    return isAdminCache;
                                });
                    } else {
                        // First run: create new document with isAdmin = false
                        Log.d(TAG, "Creating new device doc with isAdmin = false");
                        Map<String, Object> data = new HashMap<>();
                        data.put("uid", user.getUid());
                        data.put("lastSeen", FieldValue.serverTimestamp());
                        data.put("createdAt", FieldValue.serverTimestamp());
                        data.put("isAdmin", false); // Default for new devices

                        return db.collection(COLLECTION_DEVICES).document(deviceId)
                                .set(data)
                                .continueWith(setTask -> {
                                    if (!setTask.isSuccessful()) {
                                        Log.e(TAG, "Failed to create device doc", setTask.getException());
                                        return false;
                                    }

                                    isAdminCache = false;
                                    isCacheLoaded = true;
                                    Log.d(TAG, "Created new device " + deviceId + " with isAdmin = false");
                                    return false;
                                });
                    }
                });
    }

    /**
     * Checks if the current device has Admin privileges.
     * Uses cached value if available.
     * 
     * @return True if the device is cached as an admin, false otherwise
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
     * 
     * @param context The application context
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
