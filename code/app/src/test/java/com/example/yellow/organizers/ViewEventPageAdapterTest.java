package com.example.yellow.organizers;

import static org.junit.Assert.*;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.yellow.ui.QrFragmentAfterCreateEvent;
import com.example.yellow.ui.ViewEvent.EntrantsFragment;
import com.example.yellow.ui.ViewEvent.Map.MapFragment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33) // use any API level your app supports
public class ViewEventPageAdapterTest {

    @Test
    public void createFragment_returnsCorrectFragmentsAndPassesEventId() {
        // Arrange: create a FragmentActivity via Robolectric
        FragmentActivity activity =
                Robolectric.buildActivity(FragmentActivity.class).setup().get();

        String eventId = "TEST_EVENT_ID";
        ViewEventPageAdapter adapter = new ViewEventPageAdapter(activity, eventId);

        // Assert: item count is 3
        assertEquals(3, adapter.getItemCount());

        // Position 0 -> EntrantsFragment with eventId in arguments
        Fragment f0 = adapter.createFragment(0);
        assertTrue(f0 instanceof EntrantsFragment);
        Bundle args0 = f0.getArguments();
        assertNotNull(args0);
        assertEquals(eventId, args0.getString("eventId"));

        // Position 1 -> MapFragment
        Fragment f1 = adapter.createFragment(1);
        assertTrue(f1 instanceof MapFragment);

        // Position 2 -> QrFragmentAfterCreateEvent
        Fragment f2 = adapter.createFragment(2);
        assertTrue(f2 instanceof QrFragmentAfterCreateEvent);
    }
}
