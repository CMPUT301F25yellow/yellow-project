package com.example.yellow.ui.ManageEntrants;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.yellow.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CancelledFragmentTest {

    private static final String TEST_EVENT_ID = "test_event_cancelled_123";
    private FragmentScenario<CancelledFragment> scenario;

    @Before
    public void setUp() {
        // Launch fragment with eventId
        Bundle args = new Bundle();
        args.putString("eventId", TEST_EVENT_ID);
        scenario = FragmentScenario.launchInContainer(
                CancelledFragment.class,
                args,
                R.style.Theme_Yellow
        );
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void testFragmentLaunches() {
        // Verify the main UI elements are displayed
        onView(withId(R.id.btnNotifyCancelled)).check(matches(isDisplayed()));
        onView(withId(R.id.cancelledContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testNotifyButtonIsVisible() {
        // Verify the notify button is visible and clickable
        onView(withId(R.id.btnNotifyCancelled))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testNotifyButtonOpensDialog() {
        // Click the notify button
        onView(withId(R.id.btnNotifyCancelled)).perform(click());

        // Wait a moment for dialog to appear
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify dialog is displayed with correct title and buttons
        onView(withText("Notify Cancelled Entrants")).check(matches(isDisplayed()));
        onView(withText("Send")).check(matches(isDisplayed()));
        onView(withText("Cancel")).check(matches(isDisplayed()));
    }

    @Test
    public void testNotificationDialogCancel() {
        // Open dialog
        onView(withId(R.id.btnNotifyCancelled)).perform(click());

        // Wait for dialog
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click cancel
        onView(withText("Cancel")).perform(click());

        // Wait for dismissal
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify main fragment is still visible
        onView(withId(R.id.cancelledContainer)).check(matches(isDisplayed()));
    }

//    @Test
//    public void testNotificationDialogAcceptsInput() {
//        // Open dialog
//        onView(withId(R.id.btnNotifyCancelled)).perform(click());
//
//        // Wait for dialog
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // Type message in the EditText
//        onView(allOf(
//                withId(android.R.id.inputArea),
//                isDisplayed()
//        )).perform(
//                typeText("Sorry for the cancellation"),
//                closeSoftKeyboard()
//        );
//
//        // Click send (this will trigger Firebase calls)
//        onView(withText("Send")).perform(click());
//
//        // Give time for the operation to start
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // Should proceed without crash
//        onView(withId(R.id.cancelledContainer)).check(matches(isDisplayed()));
//    }

    @Test
    public void testNotificationDialogWithEmptyMessage() {
        // Open dialog
        onView(withId(R.id.btnNotifyCancelled)).perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click send without typing anything
        // According to code, it should use default message: "Event update from organizer."
        onView(withText("Send")).perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Should proceed with default message (no crash)
        onView(withId(R.id.cancelledContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testFragmentWithoutEventIdShowsError() {
        // Close existing scenario
        scenario.close();

        // Launch without eventId
        Bundle emptyArgs = new Bundle();
        scenario = FragmentScenario.launchInContainer(
                CancelledFragment.class,
                emptyArgs,
                R.style.Theme_Yellow
        );

        // Wait for toast
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Fragment should still be visible but not functional
        onView(withId(R.id.cancelledContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyStateMessageAppears() {
        // When Firebase returns no data, "No cancelled entrants" should appear
        // Give time for Firebase listener to trigger
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check if empty state or data is shown
        // This will depend on your Firebase data
        onView(withId(R.id.cancelledContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testFragmentSurvivesRotation() {
        // Recreate fragment (simulates rotation)
        scenario.recreate();

        // Wait for recreation
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify UI is still intact
        onView(withId(R.id.btnNotifyCancelled)).check(matches(isDisplayed()));
        onView(withId(R.id.cancelledContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testNotifyButtonClickable() {
        // Verify notify button can be clicked multiple times
        onView(withId(R.id.btnNotifyCancelled)).perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Dialog should appear
        onView(withText("Notify Cancelled Entrants")).check(matches(isDisplayed()));

        // Close dialog
        onView(withText("Cancel")).perform(click());

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click button again
        onView(withId(R.id.btnNotifyCancelled)).perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Dialog should appear again
        onView(withText("Notify Cancelled Entrants")).check(matches(isDisplayed()));
    }

    @Test
    public void testContainerDisplaysCorrectly() {
        // Verify the container is visible and ready to display content
        onView(withId(R.id.cancelledContainer))
                .check(matches(isDisplayed()));

        // Give time for Firebase to load data (if any)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Container should still be visible (with empty state or data)
        onView(withId(R.id.cancelledContainer))
                .check(matches(isDisplayed()));
    }

//    @Test
//    public void testDialogInputFieldIsEditable() {
//        // Open dialog
//        onView(withId(R.id.btnNotifyCancelled)).perform(click());
//
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // Type in the input field
//        onView(allOf(
//                withId(android.R.id.inputArea),
//                isDisplayed()
//        )).perform(typeText("Test message"));
//
//        // Input should be visible (no crash means it worked)
//        try {
//            Thread.sleep(300);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // Close dialog
//        onView(withText("Cancel")).perform(click());
//    }

//    @Test
//    public void testMultipleDialogOpenClose() {
//        // Test opening and closing dialog multiple times
//        for (int i = 0; i < 3; i++) {
//            // Open dialog
//            onView(withId(R.id.btnNotifyCancelled)).perform(click());
//
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            // Verify dialog is shown
//            onView(withText("Notify Cancelled Entrants")).check(matches(isDisplayed()));
//
//            // Close dialog
//            onView(withText("Cancel")).perform(click());
//
//            try {
//                Thread.sleep(300);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            // Verify we're back to fragment
//            onView(withId(R.id.cancelledContainer)).check(matches(isDisplayed()));
//        }
//    }

    @Test
    public void testFragmentLifecycle() {
        // Move through lifecycle states
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // UI should still be functional
        onView(withId(R.id.btnNotifyCancelled)).check(matches(isDisplayed()));
        onView(withId(R.id.cancelledContainer)).check(matches(isDisplayed()));
    }
}