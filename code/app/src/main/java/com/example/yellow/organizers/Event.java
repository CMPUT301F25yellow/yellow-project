package com.example.yellow.organizers;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This class represents an Event object that is stored in Firestore.
 */
public class Event {

    @DocumentId
    private String id;

    private String name;
    private String description;
    private String location;

    private String posterImageUrl; // This is the single source of truth for the poster.

    private String organizerId;
    private String organizerName;

    private Timestamp startDate;
    private Timestamp endDate;

    private Integer maxEntrants;
    private Integer maxParticipants;
    private Boolean requireGeolocation;

    private Timestamp createdAt;

    private String qrDeepLink;
    private String qrImagePng;

    /**
     * No-argument constructor required for Firebase deserialization.
     */
    public Event() {
    }

    /**
     * Full constructor for creating a complete Event object.
     */
    public Event(String id, String name, String description, String location,
            Timestamp startDate, Timestamp endDate, String posterImageUrl,
            int maxEntrants, int maxParticipants, String organizerId, String organizerName,
            boolean requireGeolocation, String qrDeepLink, String qrImagePng) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.posterImageUrl = posterImageUrl; // Correctly sets the single poster field
        this.maxEntrants = maxEntrants;
        this.maxParticipants = maxParticipants;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.requireGeolocation = requireGeolocation;
        this.qrDeepLink = qrDeepLink;
        this.qrImagePng = qrImagePng;
    }

    // -------------------- Getters and Setters --------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventName() {
        return name;
    } // Aliased for consistency

    public void setEventName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // --- CORRECTED GETTER AND SETTER FOR POSTER ---
    public String getPosterImageUrl() {
        return posterImageUrl;
    }

    public void setPosterImageUrl(String posterImageUrl) {
        this.posterImageUrl = posterImageUrl; // Now correctly sets its own field
    }

    // The confusing posterUrl field and its methods have been REMOVED.

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

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public int getMaxEntrants() {
        return maxEntrants == null ? 0 : maxEntrants;
    }

    public void setMaxEntrants(Integer maxEntrants) {
        this.maxEntrants = maxEntrants;
    }

    public int getMaxParticipants() {
        return maxParticipants == null ? 0 : maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    public boolean isRequireGeolocation() {
        return requireGeolocation != null && requireGeolocation;
    }

    public void setRequireGeolocation(Boolean requireGeolocation) {
        this.requireGeolocation = requireGeolocation;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getQrDeepLink() {
        return qrDeepLink;
    }

    public void setQrDeepLink(String qrDeepLink) {
        this.qrDeepLink = qrDeepLink;
    }

    public String getQrImagePng() {
        return qrImagePng;
    }

    public void setQrImagePng(String qrImagePng) {
        this.qrImagePng = qrImagePng;
    }

    /**
     * Builds a small readable string like "Nov 05, 2025 @ Edmonton"
     * If the date or location is missing, it only shows the available part
     *
     * @return formatted string with date and location
     */
    public String getFormattedDateAndLocation() {
        StringBuilder sb = new StringBuilder();
        if (startDate != null) {
            Date d = startDate.toDate();
            sb.append(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(d));
        }
        if (location != null && !location.isEmpty()) {
            if (sb.length() > 0)
                sb.append(" @ ");
            sb.append(location);
        }
        return sb.toString();
    }

    // -------------------- Firestore Serialization --------------------

    /**
     * Converts the event into a map of key-value pairs
     * for writing to Firestore.
     * <p>
     * This method skips the document ID (using {@link Exclude})
     * because Firestore handles it automatically.
     *
     * @return map of field names and their values
     */
    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("description", description);
        m.put("location", location);
        m.put("organizerId", organizerId);
        if (organizerName != null)
            m.put("organizerName", organizerName);
        m.put("startDate", startDate);
        m.put("endDate", endDate);
        if (posterImageUrl != null)
            m.put("posterImageUrl", posterImageUrl);
        // Use safe defaults for missing fields
        m.put("maxEntrants", getMaxEntrants());
        m.put("maxParticipants", getMaxParticipants());
        m.put("requireGeolocation", isRequireGeolocation());
        m.put("createdAt", createdAt);

        if (qrDeepLink != null)
            m.put("qrDeepLink", qrDeepLink);
        if (qrImagePng != null)
            m.put("qrImagePng", qrImagePng);


        return m;
    }
}
