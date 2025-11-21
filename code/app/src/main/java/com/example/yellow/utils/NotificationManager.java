package com.example.yellow.utils;

import android.content.Context;
import com.example.yellow.models.NotificationLog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationManager {

    public interface OnNotificationSentListener {
        void onSuccess();

        void onFailure(Exception e);
    }

    public static void sendNotification(Context context, String eventId, String eventName, String message,
            List<String> userIds, OnNotificationSentListener listener) {
        if (userIds == null || userIds.isEmpty()) {
            if (listener != null)
                listener.onFailure(new Exception("No recipients"));
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String organizerId = FirebaseAuth.getInstance().getUid();

        db.collection("profiles").document(organizerId).get().addOnCompleteListener(task -> {
            String organizerName = "Unknown";
            if (task.isSuccessful() && task.getResult() != null) {
                organizerName = task.getResult().getString("fullName");
                if (organizerName == null)
                    organizerName = "Unknown";
            }
            performSend(db, eventId, eventName, organizerId, organizerName, message, userIds, listener);
        });
    }

    private static void performSend(FirebaseFirestore db, String eventId, String eventName, String organizerId,
            String organizerName, String message, List<String> userIds, OnNotificationSentListener listener) {

        // 1. Send to individual users (write-only)
        WriteBatch batch = db.batch();
        for (String userId : userIds) {
            Map<String, Object> data = new HashMap<>();
            data.put("message", message);
            data.put("eventId", eventId);
            data.put("timestamp", FieldValue.serverTimestamp());
            data.put("read", false);
            batch.set(db.collection("profiles").document(userId).collection("notifications").document(), data);
        }

        // 2. Fetch names for the log (read-then-write)
        // Limit to 10 for the log display to avoid query limits
        List<String> toFetch = userIds.subList(0, Math.min(userIds.size(), 10));

        db.collection("profiles")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), toFetch)
                .get()
                .addOnCompleteListener(task -> {
                    java.util.List<String> recipientNames = new java.util.ArrayList<>();

                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, String> nameMap = new HashMap<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult()) {
                            String name = doc.getString("fullName");
                            if (name != null && !name.isEmpty()) {
                                nameMap.put(doc.getId(), name);
                            }
                        }

                        // Build the list in order of userIds
                        for (String uid : toFetch) {
                            String name = nameMap.get(uid);
                            recipientNames
                                    .add(name != null ? name : "User " + uid.substring(0, Math.min(uid.length(), 6)));
                        }
                    } else {
                        // Fallback if fetch fails
                        for (String uid : toFetch) {
                            recipientNames.add("User " + uid.substring(0, Math.min(uid.length(), 6)));
                        }
                    }

                    // 3. Create and save the log
                    NotificationLog log = new NotificationLog(
                            eventId, eventName, organizerId, organizerName, message, Timestamp.now(), userIds.size(),
                            userIds, recipientNames);

                    // We need a new batch or just write the log separately.
                    // Since we already prepared the batch for notifications, let's add the log to
                    // it.
                    // Note: batch operations are atomic.
                    batch.set(db.collection("notification_logs").document(), log);

                    batch.commit().addOnSuccessListener(aVoid -> {
                        if (listener != null)
                            listener.onSuccess();
                    }).addOnFailureListener(e -> {
                        if (listener != null)
                            listener.onFailure(e);
                    });
                });
    }
}
