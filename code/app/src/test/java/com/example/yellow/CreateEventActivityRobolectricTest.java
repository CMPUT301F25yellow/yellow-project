package com.example.yellow;

import android.net.Uri;

import com.example.yellow.organizers.CreateEventActivity;
import com.google.android.material.textfield.TextInputEditText;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Unit tests for CreateEventActivity logic.
 * These tests run on the JVM without Android devices or Robolectric.
 */
public class CreateEventTest {

    private CreateEventActivity activity;

    @Before
    public void setUp() {
        activity = new CreateEventActivity();
    }

    @Test
    public void textOf_returnsTrimmedString() throws Exception {
        // Use reflection to access the private static textOf method
        Method textOf = CreateEventActivity.class
                .getDeclaredMethod("textOf", TextInputEditText.class);
        textOf.setAccessible(true);

        TextInputEditText input = new TextInputEditText(null);
        input.setText("  Hello  ");
        String result = (String) textOf.invoke(null, input);

        assertEquals("Hello", result);
    }

    @Test
    public void textOf_returnsEmpty_whenNullText() throws Exception {
        Method textOf = CreateEventActivity.class
                .getDeclaredMethod("textOf", TextInputEditText.class);
        textOf.setAccessible(true);

        TextInputEditText input = new TextInputEditText(null);
        input.setText(null);
        String result = (String) textOf.invoke(null, input);

        assertEquals("", result);
    }

    @Test
    public void guessMime_returnsDefaultWhenNull() throws Exception {
        // Use reflection to call private guessMime()
        Method m = CreateEventActivity.class
                .getDeclaredMethod("guessMime", android.content.ContentResolver.class, Uri.class);
        m.setAccessible(true);

        // Pass null ContentResolver and URI to force default
        String result = (String) m.invoke(activity, null, Uri.EMPTY);
        assertEquals("image/jpeg", result);
    }

    @Test
    public void encodeImageUriToDataUri_throwsFileNotFound() {
        try {
            // should fail because no real content resolver is available in unit test
            activity.encodeImageUriToDataUri(Uri.parse("content://fakepath/image.jpg"));
            fail("Expected exception not thrown");
        } catch (Exception e) {
            assertTrue(e instanceof java.io.FileNotFoundException
                    || e instanceof IllegalArgumentException);
        }
    }
}
