package com.example.yellow.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Represents a single notification sent to a user.
 */
public class NotificationItem {

    private String message;
    private String eventId;
    private boolean read;
    private Timestamp timestamp;
    private String type;

    @DocumentId
    private String notificationId; // Auto-populated with the Firestore doc ID

    // Required empty constructor for Firestore
    public NotificationItem() {
    }

    // Getters
    public String getMessage() {
        return message;
    }

    public String getEventId() {
        return eventId;
    }

    public boolean isRead() {
        return read;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getType() {
        return type;
    }
}
