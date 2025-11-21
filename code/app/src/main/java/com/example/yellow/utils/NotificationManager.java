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
        WriteBatch batch = db.batch();

        for (String userId : userIds) {
            Map<String, Object> data = new HashMap<>();
            data.put("message", message);
            data.put("eventId", eventId);
            data.put("timestamp", FieldValue.serverTimestamp());
            data.put("read", false);
            batch.set(db.collection("profiles").document(userId).collection("notifications").document(), data);
        }

        NotificationLog log = new NotificationLog(
                eventId, eventName, organizerId, organizerName, message, Timestamp.now(), userIds.size());
        batch.set(db.collection("notification_logs").document(), log);

        batch.commit().addOnSuccessListener(aVoid -> {
            if (listener != null)
                listener.onSuccess();
        }).addOnFailureListener(e -> {
            if (listener != null)
                listener.onFailure(e);
        });
    }
}
