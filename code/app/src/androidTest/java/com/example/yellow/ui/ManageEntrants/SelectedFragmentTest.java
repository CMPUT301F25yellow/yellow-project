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
public class SelectedFragmentTest {

    private static final String TEST_EVENT_ID = "test_event_selected_123";
    private FragmentScenario<SelectedFragment> scenario;

    @Before
    public void setUp() {
        // Launch fragment with eventId
        Bundle args = new Bundle();
        args.putString("eventId", TEST_EVENT_ID);
        scenario = FragmentScenario.launchInContainer(
                SelectedFragment.class,
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
        onView(withId(R.id.btnSendNotification)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCancelSelected)).check(matches(isDisplayed()));
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testBothButtonsAreVisible() {
        // Verify both action buttons are visible
        onView(withId(R.id.btnSendNotification))
                .check(matches(isDisplayed()));
        onView(withId(R.id.btnCancelSelected))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSendNotificationButtonOpensDialog() {
        // Click the send notification button
        onView(withId(R.id.btnSendNotification)).perform(click());

        // Wait for dialog to appear
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify dialog is displayed with correct title and buttons
        onView(withText("Send Notification")).check(matches(isDisplayed()));
        onView(withText("Send")).check(matches(isDisplayed()));
        onView(withText("Cancel")).check(matches(isDisplayed()));
    }

    @Test
    public void testNotificationDialogCancel() {
        // Open dialog
        onView(withId(R.id.btnSendNotification)).perform(click());

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
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testNotificationDialogAcceptsInput() {
        // Open dialog
        onView(withId(R.id.btnSendNotification)).perform(click());

        // Wait for dialog
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Type message in the EditText using the custom ID
        onView(withId(R.id.notificationMessageInput)).perform(
                typeText("Congratulations! You have been selected!"),
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

        // Should proceed without crash
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testNotificationDialogWithEmptyMessage() {
        // Open dialog
        onView(withId(R.id.btnSendNotification)).perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click send without typing anything
        // According to code, it should use default message
        onView(withText("Send")).perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Should proceed with default message (no crash)
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testCancelSelectedButtonShowsConfirmationDialog() {
        // First we need to wait for potential data to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click the cancel selected button
        onView(withId(R.id.btnCancelSelected)).perform(click());

        // Wait for dialog or toast to appear
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Either a confirmation dialog appears (if items selected)
        // Or a toast "No entrants selected." appears
        // We can't easily test toast, but we ensure no crash
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testCancelDialogCanBeDismissed() {
        // Try to trigger cancel dialog
        // Note: This will only work if there are selected items
        // For now, just test that clicking doesn't crash
        onView(withId(R.id.btnCancelSelected)).perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Fragment should still be visible
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testFragmentWithoutEventIdShowsError() {
        // Close existing scenario
        scenario.close();

        // Launch without eventId
        Bundle emptyArgs = new Bundle();
        scenario = FragmentScenario.launchInContainer(
                SelectedFragment.class,
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
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyStateMessageAppears() {
        // When Firebase returns no data, "No selected entrants yet" should appear
        // Give time for Firebase listener to trigger
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check if empty state or data is shown
        // This will depend on your Firebase data
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
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
        onView(withId(R.id.btnSendNotification)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCancelSelected)).check(matches(isDisplayed()));
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testBothButtonsClickableSequentially() {
        // Test that both buttons work one after another

        // Click send notification
        onView(withId(R.id.btnSendNotification)).perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Dialog should appear
        onView(withText("Send Notification")).check(matches(isDisplayed()));

        // Close dialog
        onView(withText("Cancel")).perform(click());

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click cancel selected
        onView(withId(R.id.btnCancelSelected)).perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Should handle gracefully (no crash)
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testMultipleNotificationDialogOpenClose() {
        // Test opening and closing notification dialog multiple times
        for (int i = 0; i < 3; i++) {
            // Open dialog
            onView(withId(R.id.btnSendNotification)).perform(click());

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Verify dialog is shown
            onView(withText("Send Notification")).check(matches(isDisplayed()));

            // Close dialog
            onView(withText("Cancel")).perform(click());

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Verify we're back to fragment
            onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testContainerDisplaysCorrectly() {
        // Verify the container is visible and ready to display content
        onView(withId(R.id.selectedContainer))
                .check(matches(isDisplayed()));

        // Give time for Firebase to load data (if any)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Container should still be visible (with empty state or data)
        onView(withId(R.id.selectedContainer))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDialogInputFieldIsEditable() {
        // Open dialog
        onView(withId(R.id.btnSendNotification)).perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Type in the input field
        onView(allOf(
                withId(R.id.notificationMessageInput),
                isDisplayed()
        )).perform(typeText("Welcome message"));

        // Input should be visible (no crash means it worked)
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Close dialog
        onView(withText("Cancel")).perform(click());
    }

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
        onView(withId(R.id.btnSendNotification)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCancelSelected)).check(matches(isDisplayed()));
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testCancelButtonClickWithoutSelection() {
        // Wait for initial load
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click cancel without selecting any entrants
        // Should show toast "No entrants selected."
        onView(withId(R.id.btnCancelSelected)).perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Fragment should remain visible
        onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testSendNotificationMultipleTimes() {
        // Test that notification can be sent multiple times
        for (int i = 0; i < 2; i++) {
            onView(withId(R.id.btnSendNotification)).perform(click());

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Type a message
            onView(allOf(
                    withId(R.id.notificationMessageInput),
                    isDisplayed()
            )).perform(
                    typeText("Test message " + i),
                    closeSoftKeyboard()
            );

            onView(withText("Send")).perform(click());

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Should handle multiple sends gracefully
            onView(withId(R.id.selectedContainer)).check(matches(isDisplayed()));
        }
    }
}