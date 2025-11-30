package com.example.yellow;

import com.example.yellow.utils.QrUtils;
import org.junit.Test;
import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests for the QrUtils class.
 * Uses Robolectric to run the tests in a simulated Android environment.
 * Tests the deep link parsing logic for host, scheme, and event id.
 *
 * @author Marcus Lau mdlau
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class QrUtilsTest {

    // Valid case
    @Test
    public void getEventIdFromUri_withValidUri_returnsEventId() {
        String validUri = "yellow://eventdetails/event-abc-123";
        assertEquals("event-abc-123", QrUtils.getEventIdFromUri(validUri));
    }

    // Invalid Cases, should return null
    // Wrong scheme
    @Test
    public void getEventIdFromUri_withInvalidScheme_returnsNull() {
        String invalidUri = "http://eventdetails/event-abc-123";
        assertNull(QrUtils.getEventIdFromUri(invalidUri));
    }

    // Wrong host
    @Test
    public void getEventIdFromUri_withInvalidHost_returnsNull() {
        String invalidUri = "yellow://wrong-host/event-abc-123";
        assertNull(QrUtils.getEventIdFromUri(invalidUri));
    }

    // No event id
    @Test
    public void getEventIdFromUri_withMissingId_returnsNull() {
        String invalidUri = "yellow://eventdetails/";
        assertNull(QrUtils.getEventIdFromUri(invalidUri));
    }

    // Null input
    @Test
    public void getEventIdFromUri_withNullInput_returnsNull() {
        assertNull(QrUtils.getEventIdFromUri(null));
    }

    // Random string input
    @Test
    public void getEventIdFromUri_withGibberish_returnsNull() {
        String gibberish = "this is not a valid uri at all";
        assertNull(QrUtils.getEventIdFromUri(gibberish));
    }
}
