package com.example.yellow.organizers;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Firestore Event model with null-safe fields for deserialization. */
public class Event {
    @DocumentId
    private String id;

    private String name;
    private String description;
    private String location;

    private String posterUrl;
    private String organizerId;
    private String organizerName; // optional display value

    private Timestamp startDate;
    private Timestamp endDate;

    // Use boxed types so Firestore can pass null without crashing
    private Integer maxEntrants;         // was int
    private Boolean requireGeolocation;  // was boolean

    private Timestamp createdAt;

    public Event() {}

    // ---- Getters / Setters ----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    // Aliases used by UI (Glide/adapters)
    public String getPosterImageUrl() { return posterUrl; }
    public void setPosterImageUrl(String posterImageUrl) { this.posterUrl = posterImageUrl; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public Timestamp getEndDate() { return endDate; }
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }

    // SAFETY: return defaults if Firestore had null/missing
    public int getMaxEntrants() { return maxEntrants == null ? 0 : maxEntrants; }
    public void setMaxEntrants(Integer maxEntrants) { this.maxEntrants = maxEntrants; }

    public boolean isRequireGeolocation() { return requireGeolocation != null && requireGeolocation; }
    public void setRequireGeolocation(Boolean requireGeolocation) { this.requireGeolocation = requireGeolocation; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /** "MMM dd, yyyy @ Location" (handles nulls). */
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

    // ---- Firestore serialization ----
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
        // write defaults so future reads aren’t null
        m.put("maxEntrants", getMaxEntrants());
        m.put("requireGeolocation", isRequireGeolocation());
        m.put("createdAt", createdAt);
        return m;
    }
}
