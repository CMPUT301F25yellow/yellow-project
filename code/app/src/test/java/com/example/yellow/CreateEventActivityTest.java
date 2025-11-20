package com.example.yellow;

import android.content.Intent;
import android.net.Uri;

import com.example.yellow.organizers.CreateEventActivity;
import com.google.android.material.textfield.TextInputEditText;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowToast;
import java.lang.reflect.Field;
import java.util.Calendar;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class CreateEventActivityTest {

    private CreateEventActivity activity;

    @Before
    public void setup() {
        ActivityController<CreateEventActivity> controller =
                Robolectric.buildActivity(CreateEventActivity.class, new Intent())
                        .create().start().resume();
        activity = controller.get();
        assertNotNull(activity);
        ShadowToast.reset();
    }

    @Test
    public void missingName_setsError_onEventNameInput() {
        setText(R.id.descriptionInput, "desc");
        setText(R.id.locationInput, "loc");
        click(R.id.createEventButton);

        TextInputEditText name = activity.findViewById(R.id.eventNameInput);
        assertNotNull(name.getError());
        assertEquals("Required", String.valueOf(name.getError()));
    }

    @Test
    public void missingPoster_showsToast() {
        setText(R.id.eventNameInput, "My Event");
        setText(R.id.descriptionInput, "desc");
        setText(R.id.locationInput, "loc");

        click(R.id.createEventButton);

        assertEquals("Please select a poster image", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void endDateBeforeStart_showsToast() throws Exception {
        setText(R.id.eventNameInput, "My Event");
        setText(R.id.descriptionInput, "desc");
        setText(R.id.locationInput, "loc");
        setPrivateUriField("selectedPosterUri", Uri.parse("content://dummy"));

        Calendar start = getPrivateCalendar("startCal");
        Calendar end = getPrivateCalendar("endCal");
        start.set(2025, Calendar.MARCH, 10);
        end.set(2025, Calendar.MARCH, 9);

        click(R.id.createEventButton);

        assertEquals("End date cannot be before start date", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void negativeMaxEntrants_setsError() throws Exception {
        setText(R.id.eventNameInput, "My Event");
        setText(R.id.descriptionInput, "desc");
        setText(R.id.locationInput, "loc");
        setPrivateUriField("selectedPosterUri", Uri.parse("content://dummy"));

        Calendar start = getPrivateCalendar("startCal");
        Calendar end = getPrivateCalendar("endCal");
        start.set(2025, Calendar.MARCH, 10);
        end.set(2025, Calendar.MARCH, 10);

        setText(R.id.maxEntrantsInput, "-3");
        click(R.id.createEventButton);

        TextInputEditText max = activity.findViewById(R.id.maxEntrantsInput);
        assertNotNull(max.getError());
        assertEquals("Invalid number", String.valueOf(max.getError()));
    }

    // helpers
    private void setText(int id, String value) {
        TextInputEditText et = activity.findViewById(id);
        assertNotNull(et);
        et.setText(value);
    }
    private void click(int id) {
        activity.findViewById(id).performClick();
    }
    private void setPrivateUriField(String name, Uri value) throws Exception {
        Field f = CreateEventActivity.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(activity, value);
    }
    private Calendar getPrivateCalendar(String name) throws Exception {
        Field f = CreateEventActivity.class.getDeclaredField(name);
        f.setAccessible(true);
        return (Calendar) f.get(activity);
    }
}
