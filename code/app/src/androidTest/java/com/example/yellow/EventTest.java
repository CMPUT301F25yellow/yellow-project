package com.example.yellow;

import com.google.firebase.Timestamp;
import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

import com.example.yellow.organizers.Event;

public class EventTest {

    @Test
    public void gettersSetters_basicFields() {
        Event e = new Event();
        e.setId("ABC");
        e.setName("Hackathon");
        e.setDescription("Fun event");
        e.setLocation("UofA");
        e.setPosterImageUrl("data:image/jpeg;base64,xxx");
        e.setOrganizerId("user123");
        e.setOrganizerName("Alice");

        assertEquals("ABC", e.getId());
        assertEquals("Hackathon", e.getName());
        assertEquals("Fun event", e.getDescription());
        assertEquals("UofA", e.getLocation());
        assertEquals("data:image/jpeg;base64,xxx", e.getPosterImageUrl());

        assertEquals("user123", e.getOrganizerId());
        assertEquals("Alice", e.getOrganizerName());
    }

    @Test
    public void nullSafeBoxedValues_defaultsWork() {
        Event e = new Event();
        assertEquals(0, e.getMaxEntrants());
        assertFalse(e.isRequireGeolocation());

        e.setMaxEntrants(42);
        e.setRequireGeolocation(true);

        assertEquals(42, e.getMaxEntrants());
        assertTrue(e.isRequireGeolocation());
    }

    @Test
    public void startEndDate_andFormattedString() {
        Event e = new Event();
        Calendar c = Calendar.getInstance(Locale.getDefault());
        c.set(2025, Calendar.MARCH, 10, 10, 0, 0);
        e.setStartDate(new Timestamp(c.getTime()));

        e.setLocation("Edmonton");

        String s = e.getFormattedDateAndLocation();
        assertTrue(s.contains("Mar 10, 2025"));
        assertTrue(s.contains("Edmonton"));
    }

    @Test
    public void toMap_writesDefaultsAndFields() {
        Event e = new Event();
        e.setName("Name");
        e.setDescription("Desc");
        e.setLocation("Loc");
        e.setPosterImageUrl("data:image/png;base64,zzz");
        e.setOrganizerId("uid123");
        e.setOrganizerName("Bob");

        Calendar c = Calendar.getInstance();
        e.setStartDate(new Timestamp(c.getTime()));
        e.setEndDate(new Timestamp(c.getTime()));
        e.setMaxEntrants(0);
        e.setRequireGeolocation(false);

        Map<String, Object> m = e.toMap();
        assertEquals("Name", m.get("name"));
        assertEquals("Desc", m.get("description"));
        assertEquals("Loc", m.get("location"));
        assertEquals("data:image/png;base64,zzz", m.get("posterImageUrl"));
        assertEquals("uid123", m.get("organizerId"));
        assertEquals("Bob", m.get("organizerName"));
        assertTrue(m.containsKey("startDate"));
        assertTrue(m.containsKey("endDate"));
        assertEquals(0, m.get("maxEntrants"));
        assertEquals(false, m.get("requireGeolocation"));
        assertTrue(m.containsKey("createdAt")); // may be null, but key exists if set by caller
    }
}
