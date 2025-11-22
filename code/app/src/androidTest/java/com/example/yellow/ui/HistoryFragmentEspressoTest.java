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

    @org.junit.Before
    public void setUp() throws InterruptedException {
        // Ensure we start with a fresh anonymous user for EACH test to avoid state
        // leakage.
        ensureFreshLogin();
    }

    private void ensureFreshLogin() throws InterruptedException {
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            auth.signOut();
        }

        auth.signInAnonymously().addOnCompleteListener(task -> {
            latch.countDown();
        });

        // Wait for login to complete (max 10 seconds)
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        Thread.sleep(1000); // Extra buffer for Firestore/Auth propagation
    }

    /**
     * Test Case 1: User logged in (Anonymous), but NO profile.
     * Expected: "Profile Incomplete" Alert Dialog.
     */
    @Test
    public void testHistory_NoProfile_ShowsDialog() throws InterruptedException {
        // Wait for app launch
        Thread.sleep(2000);

        // 1. Click the History tab
        onView(withId(R.id.nav_history)).perform(click());

        // Wait for tab switch
        Thread.sleep(2000);

        // 2. Check if the Alert Dialog is visible
        onView(withText("Profile Incomplete")).check(matches(isDisplayed()));
        onView(withText("You must provide your Full Name and Email to perform this action."))
                .check(matches(isDisplayed()));
    }

    /**
     * Test Case 2: User logged in, HAS profile, but NO events.
     * Expected: "No history found" message (Empty State).
     */
    @Test
    public void testHistory_WithProfile_NoEvents_ShowsEmptyState() throws InterruptedException {
        // 1. Fill Profile first
        fillProfile();

        // 2. Click the History tab
        onView(withId(R.id.nav_history)).perform(click());

        // Wait for tab switch
        Thread.sleep(2000);

        // 3. Check if the Empty State is visible
        onView(withId(R.id.tvEmptyState)).check(matches(isDisplayed()));
    }

    /**
     * Test Case 3: User logged in, joins ONE event.
     * Expected: Event appears in the list.
     */
    @Test
    public void testHistory_OneEvent_ShowsEvent() throws InterruptedException {
        // 1. Fill Profile first (so we can join)
        fillProfile();

        // 2. Wait for Home events to load
        Thread.sleep(3000);

        // 3. Click "Join" on the FIRST event in the list
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

        // 8. Verify the event is in the list (RecyclerView is visible)
        onView(withId(R.id.rvHistory)).check(matches(isDisplayed()));
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

    // --- Helper Matcher for Toasts ---
    public static class ToastMatcher extends org.hamcrest.TypeSafeMatcher<androidx.test.espresso.Root> {
        @Override
        public void describeTo(org.hamcrest.Description description) {
            description.appendText("is toast");
        }

        @Override
        public boolean matchesSafely(androidx.test.espresso.Root root) {
            int type = root.getWindowLayoutParams().get().type;
            if (type == android.view.WindowManager.LayoutParams.TYPE_TOAST) {
                android.os.IBinder windowToken = root.getDecorView().getWindowToken();
                android.os.IBinder appToken = root.getDecorView().getApplicationWindowToken();
                if (windowToken == appToken) {
                    return true;
                }
            }
            return false;
        }
    }
}
