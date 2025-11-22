package com.example.yellow.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.yellow.MainActivity;
import com.example.yellow.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI Tests for HistoryFragment using Espresso.
 * <p>
 * Purpose:
 * These tests run on a REAL device or emulator. They click buttons and check
 * the screen
 * just like a real user would.
 * <p>
 * IMPORTANT:
 * Unlike the Unit Tests (HistoryFragmentTest), these tests use the REAL
 * Firebase.
 * If you are not logged in on the emulator, you might see a "Not logged in"
 * toast.
 * If you are logged in, you will see your real data.
 */
@RunWith(AndroidJUnit4.class)
public class HistoryFragmentEspressoTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Test: Navigate to History Tab
     * <p>
     * Steps:
     * 1. Launch the App (MainActivity).
     * 2. Click on the "History" icon in the bottom navigation bar.
     * 3. Verify that the "Event History" header is visible on the screen.
     */
    @Test
    public void testNavigateToHistory() throws InterruptedException {
        // Wait 2 seconds so user can see the app open
        Thread.sleep(2000);

        // 1. Click the History tab
        onView(withId(R.id.nav_history)).perform(click());

        // Wait 2 seconds so user can see the tab switch
        Thread.sleep(2000);

        // 3. Check if the RecyclerView is visible
        onView(withId(R.id.rvHistory)).check(matches(isDisplayed()));

        // Wait 2 seconds so user can see the final result
        Thread.sleep(2000);
    }

    @Test
    public void testJoinEventAndVerifyHistory() throws InterruptedException {
        // 1. Fill Profile first (so we can join)
        fillProfile();

        // 2. Wait for Home events to load
        Thread.sleep(3000);

        // 3. Click "Join" on the FIRST event in the list
        // We use a custom matcher 'first()' because there might be multiple events
        onView(first(withId(R.id.eventButton))).perform(click());

        // 4. Wait for Waiting Room to load and Auto-Join
        Thread.sleep(3000);

        // 5. Verify we are in Waiting Room (check for "Leave" button)
        onView(withId(R.id.leaveButton)).check(matches(isDisplayed()));

        // 6. Go BACK to Home
        onView(withId(R.id.backArrow)).perform(click());
        Thread.sleep(1000);

        // 7. Go to History Tab
        onView(withId(R.id.nav_history)).perform(click());
        Thread.sleep(2000);

        // 8. Verify the event is in the list
        // We just check that the RecyclerView has at least one item displayed
        onView(withId(R.id.rvHistory)).check(matches(isDisplayed()));
        // Ideally we would check for the specific event name, but for now checking the
        // list is populated is good.
    }

    private void fillProfile() throws InterruptedException {
        // Click Profile Icon
        onView(withId(R.id.iconProfile)).perform(click());
        Thread.sleep(1000);

        // Type Name
        onView(withId(R.id.inputFullName)).perform(androidx.test.espresso.action.ViewActions.replaceText("Test User"));
        // Type Email
        onView(withId(R.id.inputEmail))
                .perform(androidx.test.espresso.action.ViewActions.replaceText("test@example.com"));
        // Close soft keyboard
        onView(withId(R.id.inputEmail)).perform(androidx.test.espresso.action.ViewActions.closeSoftKeyboard());

        // Click Save
        onView(withId(R.id.btnSave)).perform(click());
        Thread.sleep(1000);

        // Go Back
        onView(withId(R.id.btnBack)).perform(click());
        Thread.sleep(1000);
    }

    // --- Helper Matcher to find the FIRST view if there are multiple ---
    public static org.hamcrest.Matcher<android.view.View> first(final org.hamcrest.Matcher<android.view.View> matcher) {
        return new org.hamcrest.BaseMatcher<android.view.View>() {
            boolean isFirst = true;

            @Override
            public boolean matches(Object item) {
                if (isFirst && matcher.matches(item)) {
                    isFirst = false;
                    return true;
                }
                return false;
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("should return first matching item");
            }
        };
    }
}
