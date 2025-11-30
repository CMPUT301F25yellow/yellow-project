package com.example.yellow.organizers;

import static org.junit.Assert.*;

import com.example.yellow.organizers.CreateEventFormValidator.Result;

import org.junit.Test;

import java.util.Calendar;

public class CreateEventActivityAndroidTest {

    private Calendar makeDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    @Test
    public void emptyName_setsRequiredError() {
        Calendar start = makeDate(2025, Calendar.JANUARY, 1);
        Calendar end   = makeDate(2025, Calendar.JANUARY, 2);

        Result r = CreateEventFormValidator.validate(
                "",
                "10",
                "",
                start,
                end
        );

        assertFalse(r.isValid);
        assertEquals("Required", r.nameError);
        assertNull(r.maxParticipantsError);
    }

    @Test
    public void emptyMaxParticipants_setsRequiredError() {
        Calendar start = makeDate(2025, Calendar.JANUARY, 1);
        Calendar end   = makeDate(2025, Calendar.JANUARY, 2);

        Result r = CreateEventFormValidator.validate(
                "Event Name",
                "",
                "",
                start,
                end
        );

        assertFalse(r.isValid);
        assertEquals("Required", r.maxParticipantsError);
    }

    @Test
    public void invalidMaxParticipants_setsInvalidNumberError() {
        Calendar start = makeDate(2025, Calendar.JANUARY, 1);
        Calendar end   = makeDate(2025, Calendar.JANUARY, 2);

        Result r = CreateEventFormValidator.validate(
                "Event Name",
                "0",   // <= 0 is invalid in your Activity logic
                "",
                start,
                end
        );

        assertFalse(r.isValid);
        assertEquals("Invalid number", r.maxParticipantsError);
    }

    @Test
    public void endBeforeStart_setsToastMessage() {
        Calendar start = makeDate(2025, Calendar.JANUARY, 10);
        Calendar end   = makeDate(2025, Calendar.JANUARY, 5);

        Result r = CreateEventFormValidator.validate(
                "Event Name",
                "10",
                "",
                start,
                end
        );

        assertFalse(r.isValid);
        assertEquals("End date cannot be before start date", r.toastMessage);
    }

    @Test
    public void negativeMaxEntrants_setsInvalidNumberError() {
        Calendar start = makeDate(2025, Calendar.JANUARY, 1);
        Calendar end   = makeDate(2025, Calendar.JANUARY, 2);

        Result r = CreateEventFormValidator.validate(
                "Event Name",
                "10",
                "-1",
                start,
                end
        );

        assertFalse(r.isValid);
        assertEquals("Invalid number", r.maxEntrantsError);
    }

    @Test
    public void maxEntrantsLessThanMaxParticipants_setsRelationError() {
        Calendar start = makeDate(2025, Calendar.JANUARY, 1);
        Calendar end   = makeDate(2025, Calendar.JANUARY, 2);

        Result r = CreateEventFormValidator.validate(
                "Event Name",
                "10",
                "5",
                start,
                end
        );

        assertFalse(r.isValid);
        assertEquals("Must be ≥ max participants", r.maxEntrantsError);
    }

    @Test
    public void validInputs_markResultAsValid() {
        Calendar start = makeDate(2025, Calendar.JANUARY, 1);
        Calendar end   = makeDate(2025, Calendar.JANUARY, 2);

        Result r = CreateEventFormValidator.validate(
                "Valid Event",
                "10",
                "20",
                start,
                end
        );

        assertTrue(r.isValid);
        assertNull(r.nameError);
        assertNull(r.maxParticipantsError);
        assertNull(r.maxEntrantsError);
        assertNull(r.toastMessage);
    }
}
