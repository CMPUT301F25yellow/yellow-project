package com.example.yellow.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class NotificationLog {
    @DocumentId
    private String id;
    private String eventId;
    private String eventName;
    private String organizerId;
    private String organizerName;
    private String message;
    private Timestamp timestamp;
    private int recipientCount;

    private java.util.List<String> recipientIds;
    private java.util.List<String> recipientNames;

    public NotificationLog() {
    }

    public NotificationLog(String eventId, String eventName, String organizerId, String organizerName, String message,
            Timestamp timestamp, int recipientCount, java.util.List<String> recipientIds,
            java.util.List<String> recipientNames) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.message = message;
        this.timestamp = timestamp;
        this.recipientCount = recipientCount;
        this.recipientIds = recipientIds;
        this.recipientNames = recipientNames;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public int getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
    }

    public java.util.List<String> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(java.util.List<String> recipientIds) {
        this.recipientIds = recipientIds;
    }

    public java.util.List<String> getRecipientNames() {
        return recipientNames;
    }

    public void setRecipientNames(java.util.List<String> recipientNames) {
        this.recipientNames = recipientNames;
    }
}
