package com.example.yellow.users;

import com.google.firebase.firestore.FieldValue;

public class WaitingUser {
    public String userId;
    public String eventId;
    public Object timestamp = FieldValue.serverTimestamp();
    private String name;
    private Double latitude;
    private Double longitude;
    // Add any other fields from your user profile, like profileImageUrl

    public WaitingUser() {}

    public WaitingUser(String userId, String eventId) {
        this.userId = userId;
        this.eventId = eventId;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
