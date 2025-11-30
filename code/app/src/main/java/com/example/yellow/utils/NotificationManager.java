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
        sendNotification(context, eventId, eventName, message, null, userIds, listener);
    }

    public static void sendNotification(Context context, String eventId, String eventName, String message,
            String type, List<String> userIds, OnNotificationSentListener listener) {
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
            performSend(db, eventId, eventName, organizerId, organizerName, message, type, userIds, listener);
        });
    }

    private static void performSend(FirebaseFirestore db, String eventId, String eventName, String organizerId,
            String organizerName, String message, String type, List<String> userIds,
            OnNotificationSentListener listener) {

        // 1. Send to individual users (write-only)
        WriteBatch batch = db.batch();
        for (String userId : userIds) {
            Map<String, Object> data = new HashMap<>();
            data.put("message", message);
            data.put("eventId", eventId);
            data.put("timestamp", FieldValue.serverTimestamp());
            data.put("read", false);
            if (type != null) {
                data.put("type", type);
            }
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

    /**
     * Builds recipient display names for the notification log.
     *
     * For each uid in userIds (up to maxCount), if a non-empty name exists in nameMap,
     * that name is used. Otherwise, we fall back to "User " + first 6 chars of uid.
     */
    static java.util.List<String> buildRecipientNames(
            java.util.List<String> userIds,
            java.util.Map<String, String> nameMap,
            int maxCount
    ) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (userIds == null || userIds.isEmpty() || maxCount <= 0) {
            return result;
        }

        int count = Math.min(userIds.size(), maxCount);
        for (int i = 0; i < count; i++) {
            String uid = userIds.get(i);
            String name = nameMap != null ? nameMap.get(uid) : null;
            if (name != null && !name.isEmpty()) {
                result.add(name);
            } else {
                String prefix = uid == null ? "" : uid.substring(0, Math.min(uid.length(), 6));
                result.add("User " + prefix);
            }
        }
        return result;
    }
}
