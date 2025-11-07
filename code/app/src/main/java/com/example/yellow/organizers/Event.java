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
 * <p>
 * It holds all event information such as name, date, location, and poster.
 * The fields are made null-safe so the app doesn’t crash when Firestore sends null values.
 */
public class Event {

    /** Firestore document ID (auto-generated). */
    @DocumentId
    private String id;

    private String name;
    private String description;
    private String location;

    private String posterUrl;
    private String organizerId;
    private String organizerName; // optional display name

    private Timestamp startDate;
    private Timestamp endDate;

    // Use boxed types so Firestore can handle null values
    private Integer maxEntrants;        // was int
    private Boolean requireGeolocation; // was boolean

    private Timestamp createdAt;

    private String qrDeepLink;
    private String qrImagePng;

    /** Empty constructor required by Firestore. */
    public Event() {}

    // -------------------- Getters and Setters --------------------

    /**
     * @return the event document ID
     */
    public String getId() { return id; }

    /**
     * @param id sets the event document ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * @return the event name
     */
    public String getName() { return name; }

    /**
     * @param name sets the event name
     */
    public void setName(String name) { this.name = name; }

    /**
     * @return the event description
     */
    public String getDescription() { return description; }

    /**
     * @param description sets the event description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * @return the event location
     */
    public String getLocation() { return location; }

    /**
     * @param location sets the event location
     */
    public void setLocation(String location) { this.location = location; }

    /**
     * @return the URL or Base64 string for the event poster
     */
    public String getPosterUrl() { return posterUrl; }

    /**
     * @param posterUrl sets the event poster URL or Base64 string
     */
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    /**
     * Alias getter used by some UI adapters.
     *
     * @return same value as {@link #getPosterUrl()}
     */
    public String getPosterImageUrl() { return posterUrl; }

    /**
     * Alias setter used by some UI adapters.
     *
     * @param posterImageUrl sets the event poster image URL
     */
    public void setPosterImageUrl(String posterImageUrl) { this.posterUrl = posterImageUrl; }

    /**
     * @return the ID of the organizer who created the event
     */
    public String getOrganizerId() { return organizerId; }

    /**
     * @param organizerId sets the organizer’s user ID
     */
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    /**
     * @return the organizer’s display name
     */
    public String getOrganizerName() { return organizerName; }

    /**
     * @param organizerName sets the organizer’s display name
     */
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    /**
     * @return the start date and time of the event
     */
    public Timestamp getStartDate() { return startDate; }

    /**
     * @param startDate sets the event start date and time
     */
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    /**
     * @return the end date and time of the event
     */
    public Timestamp getEndDate() { return endDate; }

    /**
     * @param endDate sets the event end date and time
     */
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }

    /**
     * @return the maximum number of people allowed (0 if not set)
     */
    public int getMaxEntrants() { return maxEntrants == null ? 0 : maxEntrants; }

    /**
     * @param maxEntrants sets the maximum number of people allowed
     */
    public void setMaxEntrants(Integer maxEntrants) { this.maxEntrants = maxEntrants; }

    /**
     * @return true if geolocation is required, false otherwise
     */
    public boolean isRequireGeolocation() { return requireGeolocation != null && requireGeolocation; }

    /**
     * @param requireGeolocation sets whether geolocation is required
     */
    public void setRequireGeolocation(Boolean requireGeolocation) { this.requireGeolocation = requireGeolocation; }

    /**
     * @return the timestamp when the event was created
     */
    public Timestamp getCreatedAt() { return createdAt; }

    /**
     * @param createdAt sets the creation timestamp
     */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getQrDeepLink() { return qrDeepLink; }
    public void setQrDeepLink(String qrDeepLink) { this.qrDeepLink = qrDeepLink; }
    public String getQrImagePng() { return qrImagePng; }
    public void setQrImagePng(String qrImagePng) { this.qrImagePng = qrImagePng; }

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
            if (sb.length() > 0) sb.append(" @ ");
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
        m.put("posterUrl", posterUrl);
        m.put("organizerId", organizerId);
        if (organizerName != null) m.put("organizerName", organizerName);
        m.put("startDate", startDate);
        m.put("endDate", endDate);
        // Use safe defaults for missing fields
        m.put("maxEntrants", getMaxEntrants());
        m.put("requireGeolocation", isRequireGeolocation());
        m.put("createdAt", createdAt);

        if (qrDeepLink != null)  m.put("qrDeepLink", qrDeepLink);
        if (qrImagePng != null)  m.put("qrImagePng", qrImagePng);

        return m;
    }
}