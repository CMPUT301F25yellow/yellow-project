package com.example.yellow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Flow:
 * 1. Anonymous test user + profile are created.
 * 2. Go to History → assert TEST_EVENT_NAME is NOT present.
 * 3. Go Home → find TEST_EVENT_NAME in main list, join waiting list.
 * 4. Go to History → assert TEST_EVENT_NAME IS present.
 * 5. Go Home → open event → leave waiting list.
 * 6. Go to History → assert TEST_EVENT_NAME is NOT present again.
 * 7. Delete profile + auth user.
 */
@RunWith(AndroidJUnit4.class)
public class HistoryFragmentEspressoTest {

    private static final String TEST_EVENT_NAME = "UI Test Event (History)"; // existing event name

    // TODO: replace these with your actual resource IDs if they differ.
    private static final int ID_NAV_HISTORY = R.id.nav_history; // bottom nav: history
    private static final int ID_NAV_HOME = R.id.nav_home; // bottom nav: home/main
    private static final int ID_MAIN_EVENT_BUTTON = R.id.eventButton; // button in main list row (View details / Join)
    private static final int ID_DETAILS_JOIN_BUTTON = R.id.eventButton; // button in details to JOIN waiting list
    private static final int ID_DETAILS_LEAVE_BUTTON = R.id.leaveButton; // button in details to LEAVE waiting list
    private static final int ID_HISTORY_RECYCLER = R.id.rvHistory; // RecyclerView in HistoryFragment
    private static final int ID_HISTORY_EVENT_TITLE = R.id.eventTitle; // TextView for event title in history row
    private static final int ID_BACK_ARROW = R.id.backArrow; // Back arrow button in fragments

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String testUid;

    @Before
    public void setUp() throws Exception {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1) Create anonymous test user (fresh user for this test).
        CountDownLatch loginLatch = new CountDownLatch(1);
        auth.signOut();

        auth.signInAnonymously().addOnCompleteListener(task -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                testUid = user.getUid();
            }
            loginLatch.countDown();
        });

        loginLatch.await(10, TimeUnit.SECONDS);
        if (testUid == null) {
            throw new IllegalStateException("Failed to sign in anonymously for test user");
        }

        // 2) Create a simple profile document so app doesn't complain.
        CountDownLatch profileLatch = new CountDownLatch(1);
        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", "Espresso Test User");
        profile.put("email", "espresso@test.com");

        db.collection("profiles").document(testUid)
                .set(profile)
                .addOnCompleteListener(task -> profileLatch.countDown());

        profileLatch.await(10, TimeUnit.SECONDS);

        // 3) Launch MainActivity AFTER user + profile exist.
        ActivityScenario.launch(MainActivity.class);

        // 4) Normalize: make sure this test user is NOT on the waiting list
        // for TEST_EVENT_NAME before the test starts.
        // Go to History
        onView(withId(ID_NAV_HISTORY)).perform(click());
        waitForView(withId(ID_HISTORY_RECYCLER), 10000);

        // Wait a bit for HistoryFragment to load data from Firestore
        Thread.sleep(2000);

        // Check if event is in the History RecyclerView specifically
        boolean alreadyInHistory = isViewInRecyclerView(
                ID_HISTORY_RECYCLER,
                allOf(withId(ID_HISTORY_EVENT_TITLE), withText(TEST_EVENT_NAME)));

        if (alreadyInHistory) {
            // Go home
            onView(withId(ID_NAV_HOME)).perform(click());

            // Open the event from main list
            waitForView(withText(TEST_EVENT_NAME), 20000);
            onView(allOf(
                    withId(ID_MAIN_EVENT_BUTTON),
                    hasSibling(withText(TEST_EVENT_NAME)))).perform(scrollTo(), click());

            // Leave waiting list (WaitingListFragment auto-joins, so we need to leave)
            waitForView(withId(ID_DETAILS_LEAVE_BUTTON), 5000);
            onView(withId(ID_DETAILS_LEAVE_BUTTON)).perform(click());

            // Wait for Firestore to propagate
            Thread.sleep(1000);
        }
    }

    @Test
    public void history_addAndRemoveEventFromWaitingList() throws Exception {
        // STEP 1: History → event should NOT be there.
        onView(withId(ID_NAV_HISTORY)).perform(click());
        waitForView(withId(ID_HISTORY_RECYCLER), 10000);
        Thread.sleep(2000); // Wait for HistoryFragment to load

        // Check that the RecyclerView does NOT contain the event
        try {
            onView(withId(ID_HISTORY_RECYCLER))
                    .check(matches(hasDescendant(allOf(
                            withId(ID_HISTORY_EVENT_TITLE),
                            withText(TEST_EVENT_NAME)))));
            throw new AssertionError("Event should NOT be in history initially");
        } catch (NoMatchingViewException | AssertionError e) {
            // Expected - event should not be in history
            if (e.getMessage() != null && e.getMessage().contains("should NOT be in history")) {
                throw e;
            }
        }

        // Visual pause after checking history is empty
        Thread.sleep(2000);

        // STEP 2: Home → join waiting list for the test event.
        onView(withId(ID_NAV_HOME)).perform(click());
        Thread.sleep(2000); // Visual pause after navigating home

        waitForView(withText(TEST_EVENT_NAME), 20000);
        Thread.sleep(2000); // Visual pause after finding event

        onView(allOf(
                withId(ID_MAIN_EVENT_BUTTON),
                hasSibling(withText(TEST_EVENT_NAME)))).perform(scrollTo(), click());
        Thread.sleep(2000); // Visual pause after clicking event

        // WaitingListFragment auto-joins when opened with complete profile
        // Wait for the leave button to appear (confirms we're joined)
        waitForView(withId(ID_DETAILS_LEAVE_BUTTON), 5000);
        Thread.sleep(2000); // Visual pause showing we're in waiting list

        // Wait for Firestore write to propagate
        Thread.sleep(2000);

        // Click back arrow to return to MainActivity
        onView(withId(ID_BACK_ARROW)).perform(click());
        Thread.sleep(2000); // Visual pause after going back

        // STEP 3: History → event SHOULD be there.
        onView(withId(ID_NAV_HISTORY)).perform(click());
        Thread.sleep(2000); // Visual pause after navigating to history

        waitForView(withId(ID_HISTORY_RECYCLER), 20000);
        Thread.sleep(3000); // Wait longer for HistoryFragment to reload and query Firestore

        onView(withId(ID_HISTORY_RECYCLER))
                .check(matches(hasDescendant(allOf(
                        withId(ID_HISTORY_EVENT_TITLE),
                        withText(TEST_EVENT_NAME)))));

        // Visual pause showing event is in history
        Thread.sleep(2000);

        // STEP 4: Home → leave waiting list again.
        onView(withId(ID_NAV_HOME)).perform(click());
        Thread.sleep(2000); // Visual pause after navigating home

        waitForView(withText(TEST_EVENT_NAME), 20000);
        Thread.sleep(2000); // Visual pause after finding event

        onView(allOf(
                withId(ID_MAIN_EVENT_BUTTON),
                hasSibling(withText(TEST_EVENT_NAME)))).perform(scrollTo(), click());
        Thread.sleep(2000); // Visual pause after clicking event

        waitForView(withId(ID_DETAILS_LEAVE_BUTTON), 5000);
        Thread.sleep(2000); // Visual pause before leaving

        onView(withId(ID_DETAILS_LEAVE_BUTTON)).perform(click());
        Thread.sleep(2000); // Visual pause after clicking leave

        // Wait longer for Firestore write to propagate (leave operation)
        Thread.sleep(2000);

        // After leaving, the fragment might auto-close or we might need to go back
        // Try to click back arrow if it exists, otherwise we're already in MainActivity
        try {
            waitForView(withId(ID_BACK_ARROW), 2000);
            onView(withId(ID_BACK_ARROW)).perform(click());
            Thread.sleep(2000); // Visual pause after going back
        } catch (Exception e) {
            // Already back in MainActivity, no need to navigate
            Thread.sleep(2000); // Visual pause anyway
        }

        // STEP 5: History → event should be gone again.
        onView(withId(ID_NAV_HISTORY)).perform(click());
        Thread.sleep(2000); // Visual pause after navigating to history

        waitForView(withId(ID_HISTORY_RECYCLER), 10000);
        Thread.sleep(3000); // Wait longer for HistoryFragment to reload after leaving

        // Check that the RecyclerView does NOT contain the event
        try {
            onView(withId(ID_HISTORY_RECYCLER))
                    .check(matches(hasDescendant(allOf(
                            withId(ID_HISTORY_EVENT_TITLE),
                            withText(TEST_EVENT_NAME)))));
            throw new AssertionError("Event should NOT be in history after leaving");
        } catch (NoMatchingViewException | AssertionError e) {
            // Expected - event should not be in history
            if (e.getMessage() != null && e.getMessage().contains("should NOT be in history")) {
                throw e;
            }
        }

        // Final visual pause showing history is empty again
        Thread.sleep(2000);
    }

    @After
    public void tearDown() throws Exception {
        // Delete profile document.
        if (testUid != null) {
            CountDownLatch profileDeleteLatch = new CountDownLatch(1);
            db.collection("profiles").document(testUid)
                    .delete()
                    .addOnCompleteListener(task -> profileDeleteLatch.countDown());
            profileDeleteLatch.await(10, TimeUnit.SECONDS);
        }

        // Delete auth user.
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            CountDownLatch userDeleteLatch = new CountDownLatch(1);
            user.delete().addOnCompleteListener(task -> userDeleteLatch.countDown());
            userDeleteLatch.await(10, TimeUnit.SECONDS);
        }

        // Sign out.
        auth.signOut();
    }

    // ---------- Helpers ----------

    /**
     * Returns true if a view matching matcher is currently displayed, false
     * otherwise.
     */
    private boolean isViewDisplayed(Matcher<View> matcher) {
        try {
            onView(matcher).check(matches(isDisplayed()));
            return true;
        } catch (NoMatchingViewException | AssertionError e) {
            return false;
        }
    }

    /**
     * Returns true if a view matching itemMatcher exists as a descendant of the
     * RecyclerView.
     */
    private boolean isViewInRecyclerView(int recyclerViewId, Matcher<View> itemMatcher) {
        try {
            onView(withId(recyclerViewId)).check(matches(hasDescendant(itemMatcher)));
            return true;
        } catch (NoMatchingViewException | AssertionError e) {
            return false;
        }
    }

    /** Custom sibling matcher (button in same row as a given text). */
    public static Matcher<View> hasSibling(final Matcher<View> siblingMatcher) {
        return new androidx.test.espresso.matcher.BoundedMatcher<View, View>(View.class) {
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("has sibling: ");
                siblingMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (view.getParent() instanceof android.view.ViewGroup) {
                    android.view.ViewGroup parent = (android.view.ViewGroup) view.getParent();
                    for (int i = 0; i < parent.getChildCount(); i++) {
                        if (siblingMatcher.matches(parent.getChildAt(i))) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    /** Waits until a view matching viewMatcher appears, or times out. */
    public static void waitForView(final Matcher<View> viewMatcher, final long millis) {
        onView(isRoot()).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait up to " + millis + "ms for: " + viewMatcher;
            }

            @Override
            public void perform(UiController uiController, View root) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + millis;

                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(root)) {
                        if (viewMatcher.matches(child)) {
                            return;
                        }
                    }
                    uiController.loopMainThreadForAtLeast(50);
                } while (System.currentTimeMillis() < endTime);

                throw new PerformException.Builder()
                        .withActionDescription(getDescription())
                        .withViewDescription(HumanReadables.describe(root))
                        .withCause(new TimeoutException())
                        .build();
            }
        });
    }
}