package com.example.yellow.utils;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

/**
 * Utility class for synchronizing user profile updates across the application.
 * 
 * This class handles the propagation of profile changes (like name updates) to
 * denormalized data stored in other collections (events, logs, etc.) to ensure
 * data consistency.
 * 
 * @author Tabrez
 */
public class ProfileSyncUtils {

    private static final String TAG = "ProfileSyncUtils";

    /**
     * Updates the user's display name in all Firestore documents where it is
     * cached.
     * Currently updates:
     * - events where organizerId == uid (field: organizerName)
     * - notification_logs where organizerId == uid (field: organizerName)
     *
     * @param db      The Firestore instance
     * @param uid     The UID of the user
     * @param newName The new full name of the user
     */
    public static void updateUserDisplayNameEverywhere(FirebaseFirestore db, String uid, String newName) {
        // 1. Update events where this user is the organizer
        db.collection("events")
                .whereEqualTo("organizerId", uid)
                .get()
                .addOnSuccessListener(eventSnap -> {
                    if (eventSnap.isEmpty())
                        return;
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : eventSnap) {
                        batch.update(doc.getReference(), "organizerName", newName);
                    }
                    batch.commit()
                            .addOnFailureListener(e -> Log.w(TAG, "Failed to batch update event organizer names", e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to query events for name propagation", e));

        // 2. Update notification logs where this user is the organizer/sender
        db.collection("notification_logs")
                .whereEqualTo("organizerId", uid)
                .get()
                .addOnSuccessListener(logSnap -> {
                    if (logSnap.isEmpty())
                        return;
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : logSnap) {
                        batch.update(doc.getReference(), "organizerName", newName);
                    }
                    batch.commit()
                            .addOnFailureListener(e -> Log.w(TAG, "Failed to batch update notification log names", e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to query notification logs for name propagation", e));
    }
}
