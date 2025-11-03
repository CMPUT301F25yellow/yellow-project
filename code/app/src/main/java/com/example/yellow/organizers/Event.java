package com.example.yellow.organizers;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Event {
    @DocumentId
    private String id;
    private String name;
    private String description;
    private String location;
    private Timestamp startDate;
    private Timestamp endDate;
    private Integer maxEntrants;
    private boolean requireGeolocation;
    private String posterImageUrl;
    private String organizerId;
    private String organizerName;
    private int totalEntrants;
    private int waitlisted;
    private int selected;
    private int cancelled;
    private int enrolled;
    @ServerTimestamp
    private Timestamp createdAt;

    // Required empty constructor for Firebase
    public Event() {}

    public Event(String name, String description, String location,
                 Timestamp startDate, Timestamp endDate, Integer maxEntrants,
                 boolean requireGeolocation, String posterImageUrl,
                 String organizerId, String organizerName) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxEntrants = maxEntrants;
        this.requireGeolocation = requireGeolocation;
        this.posterImageUrl = posterImageUrl;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.totalEntrants = 0;
        this.waitlisted = 0;
        this.selected = 0;
        this.cancelled = 0;
        this.enrolled = 0;
    }

    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("location", location);
        map.put("startDate", startDate);
        map.put("endDate", endDate);
        map.put("maxEntrants", maxEntrants);
        map.put("requireGeolocation", requireGeolocation);
        map.put("posterImageUrl", posterImageUrl);
        map.put("organizerId", organizerId);
        map.put("organizerName", organizerName);
        map.put("totalEntrants", totalEntrants);
        map.put("waitlisted", waitlisted);
        map.put("selected", selected);
        map.put("cancelled", cancelled);
        map.put("enrolled", enrolled);
        map.put("createdAt", createdAt);
        return map;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public Timestamp getEndDate() { return endDate; }
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }

    public Integer getMaxEntrants() { return maxEntrants; }
    public void setMaxEntrants(Integer maxEntrants) { this.maxEntrants = maxEntrants; }

    public boolean isRequireGeolocation() { return requireGeolocation; }
    public void setRequireGeolocation(boolean requireGeolocation) {
        this.requireGeolocation = requireGeolocation;
    }

    public String getPosterImageUrl() { return posterImageUrl; }
    public void setPosterImageUrl(String posterImageUrl) {
        this.posterImageUrl = posterImageUrl;
    }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public int getTotalEntrants() { return totalEntrants; }
    public void setTotalEntrants(int totalEntrants) { this.totalEntrants = totalEntrants; }

    public int getWaitlisted() { return waitlisted; }
    public void setWaitlisted(int waitlisted) { this.waitlisted = waitlisted; }

    public int getSelected() { return selected; }
    public void setSelected(int selected) { this.selected = selected; }

    public int getCancelled() { return cancelled; }
    public void setCancelled(int cancelled) { this.cancelled = cancelled; }

    public int getEnrolled() { return enrolled; }
    public void setEnrolled(int enrolled) { this.enrolled = enrolled; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
