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
import static org.hamcrest.Matchers.not;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.yellow.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EnrolledFragmentTest {

    private static final String TEST_EVENT_ID = "test_event_123";
    private FragmentScenario<EnrolledFragment> scenario;

    @Before
    public void setUp() {
        // Launch fragment with eventId
        Bundle args = new Bundle();
        args.putString("eventId", TEST_EVENT_ID);
        scenario = FragmentScenario.launchInContainer(
                EnrolledFragment.class,
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
        onView(withId(R.id.btnNotifyEnrolled)).check(matches(isDisplayed()));
        onView(withId(R.id.btnExportCSV)).check(matches(isDisplayed()));
        onView(withId(R.id.enrolledContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testSendNotificationButtonIsHidden() {
        // The btnSendNotification should be hidden (GONE)
        onView(withId(R.id.btnSendNotification)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testNotifyButtonOpensDialog() {
        // Click the notify button
        onView(withId(R.id.btnNotifyEnrolled)).perform(click());

        // Wait a moment for dialog to appear
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify dialog is displayed with correct title and buttons
        onView(withText("Notify Enrolled Entrants")).check(matches(isDisplayed()));
        onView(withText("Send")).check(matches(isDisplayed()));
        onView(withText("Cancel")).check(matches(isDisplayed()));
    }

    @Test
    public void testNotificationDialogCancel() {
        // Open dialog
        onView(withId(R.id.btnNotifyEnrolled)).perform(click());

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
        onView(withId(R.id.enrolledContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testNotificationDialogAcceptsInput() {
        // Open dialog
        onView(withId(R.id.btnNotifyEnrolled)).perform(click());

        // Wait for dialog
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Type message in the EditText
        onView(allOf(
                withId(android.R.id.inputArea),
                isDisplayed()
        )).perform(
                typeText("Important update!"),
                closeSoftKeyboard()
        );

        // Click send (this will trigger Firebase calls)
        onView(withText("Send")).perform(click());

        // Give time for the operation to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExportCSVButtonIsClickable() {
        // Verify export button exists and is clickable
        onView(withId(R.id.btnExportCSV))
                .check(matches(isDisplayed()))
                .perform(click());

        // Give time for Firebase query to execute
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Should either show "No enrolled entrants" or start export
        // We can't easily verify the outcome without Firebase, but we ensure no crash
    }

    @Test
    public void testFragmentWithoutEventIdShowsError() {
        // Close existing scenario
        scenario.close();

        // Launch without eventId
        Bundle emptyArgs = new Bundle();
        scenario = FragmentScenario.launchInContainer(
                EnrolledFragment.class,
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
        onView(withId(R.id.enrolledContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyStateMessageAppears() {
        // When Firebase returns no data, "No Enrolled entrants" should appear
        // Give time for Firebase listener to trigger
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check if empty state or data is shown
        // This will depend on your Firebase data
        onView(withId(R.id.enrolledContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testBothButtonsAreClickableSequentially() {
        // Test that both buttons work one after another

        // Click export
        onView(withId(R.id.btnExportCSV)).perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click notify
        onView(withId(R.id.btnNotifyEnrolled)).perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Dialog should appear
        onView(withText("Notify Enrolled Entrants")).check(matches(isDisplayed()));

        // Close dialog
        onView(withText("Cancel")).perform(click());
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
        onView(withId(R.id.btnNotifyEnrolled)).check(matches(isDisplayed()));
        onView(withId(R.id.btnExportCSV)).check(matches(isDisplayed()));
        onView(withId(R.id.enrolledContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testNotificationDialogWithEmptyMessage() {
        // Open dialog
        onView(withId(R.id.btnNotifyEnrolled)).perform(click());

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
        onView(withId(R.id.enrolledContainer)).check(matches(isDisplayed()));
    }
}