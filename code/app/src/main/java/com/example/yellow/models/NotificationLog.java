package com.example.yellow.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Model class representing a log of a notification sent for an event.
 * Stores details about the message, sender, and recipients.
 * 
 * @author Tabrez
 */
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

    /**
     * Default constructor required for Firestore serialization.
     */
    public NotificationLog() {
    }

    /**
     * Constructs a new NotificationLog with specified details.
     *
     * @param eventId        ID of the event related to the notification
     * @param eventName      Name of the event
     * @param organizerId    ID of the organizer sending the notification
     * @param organizerName  Name of the organizer
     * @param message        Content of the notification message
     * @param timestamp      Time when the notification was sent
     * @param recipientCount Total number of recipients
     * @param recipientIds   List of user IDs who received the notification
     * @param recipientNames List of user names who received the notification
     */
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

    /**
     * Gets the unique document ID of this log.
     * 
     * @return The Firestore document ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique document ID.
     * 
     * @param id The Firestore document ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the associated event ID.
     * 
     * @return The event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the associated event ID.
     * 
     * @param eventId The event ID
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the name of the event.
     * 
     * @return The event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the name of the event.
     * 
     * @param eventName The event name
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Gets the ID of the organizer who sent the notification.
     * 
     * @return The organizer's user ID
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Sets the ID of the organizer.
     * 
     * @param organizerId The organizer's user ID
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Gets the name of the organizer.
     * 
     * @return The organizer's name
     */
    public String getOrganizerName() {
        return organizerName;
    }

    /**
     * Sets the name of the organizer.
     * 
     * @param organizerName The organizer's name
     */
    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    /**
     * Gets the content of the notification message.
     * 
     * @return The message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the content of the notification message.
     * 
     * @param message The message string
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the timestamp when the notification was sent.
     * 
     * @return The timestamp
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the notification was sent.
     * 
     * @param timestamp The timestamp
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the total number of recipients.
     * 
     * @return The recipient count
     */
    public int getRecipientCount() {
        return recipientCount;
    }

    /**
     * Sets the total number of recipients.
     * 
     * @param recipientCount The recipient count
     */
    public void setRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
    }

    /**
     * Gets the list of recipient user IDs.
     * 
     * @return List of recipient IDs
     */
    public java.util.List<String> getRecipientIds() {
        return recipientIds;
    }

    /**
     * Sets the list of recipient user IDs.
     * 
     * @param recipientIds List of recipient IDs
     */
    public void setRecipientIds(java.util.List<String> recipientIds) {
        this.recipientIds = recipientIds;
    }

    /**
     * Gets the list of recipient names.
     * 
     * @return List of recipient names
     */
    public java.util.List<String> getRecipientNames() {
        return recipientNames;
    }

    /**
     * Sets the list of recipient names.
     * 
     * @param recipientNames List of recipient names
     */
    public void setRecipientNames(java.util.List<String> recipientNames) {
        this.recipientNames = recipientNames;
    }
}
