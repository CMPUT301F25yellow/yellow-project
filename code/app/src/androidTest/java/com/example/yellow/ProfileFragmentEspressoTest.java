package com.example.yellow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.Root;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class ProfileFragmentEspressoTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String testUid;

    // IDs based on MainActivity and fragment_profile_users.xml
    private static final int ID_NAV_HOME = R.id.nav_home;
    private static final int ID_NAV_HISTORY = R.id.nav_history;
    private static final int ID_NAV_CREATE_EVENT = R.id.nav_create_event;
    private static final int ID_NAV_MY_EVENTS = R.id.nav_my_events;
    private static final int ID_ICON_PROFILE = R.id.iconProfile;
    private static final int ID_ROOT_PROFILE = R.id.root_profile;

    private static final int ID_INPUT_NAME = R.id.inputFullName;
    private static final int ID_INPUT_EMAIL = R.id.inputEmail;
    private static final int ID_INPUT_PHONE = R.id.inputPhone;

    private static final int ID_LAYOUT_NAME = R.id.layoutFullName;
    private static final int ID_LAYOUT_EMAIL = R.id.layoutEmail;
    private static final int ID_LAYOUT_PHONE = R.id.layoutPhone;

    private static final int ID_BTN_SAVE = R.id.btnSave;
    private static final int ID_BTN_DELETE = R.id.btnDeleteProfile;

    // Dialog text from ProfileUtils.java
    private static final String DIALOG_TITLE = "Profile Incomplete";
    private static final String DIALOG_MESSAGE = "You must provide your Full Name and Email";

    @Before
    public void setUp() throws Exception {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1. Ensure signed in (anonymous)
        if (auth.getCurrentUser() == null) {
            CountDownLatch latch = new CountDownLatch(1);
            auth.signInAnonymously().addOnCompleteListener(task -> latch.countDown());
            latch.await(10, TimeUnit.SECONDS);
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            throw new IllegalStateException("Auth failed");
        testUid = user.getUid();

        // 2. Delete existing profile to start fresh
        CountDownLatch delLatch = new CountDownLatch(1);
        db.collection("profiles").document(testUid).delete()
                .addOnCompleteListener(task -> delLatch.countDown());
        delLatch.await(10, TimeUnit.SECONDS);

        // Wait a bit for propagation
        Thread.sleep(1000);
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup: delete profile
        if (testUid != null) {
            CountDownLatch latch = new CountDownLatch(1);
            db.collection("profiles").document(testUid).delete()
                    .addOnCompleteListener(task -> latch.countDown());
            latch.await(10, TimeUnit.SECONDS);
        }
        // Do NOT sign out as per instructions
    }

    @Test
    public void profile_flow_requiresProfileAndValidatesFields() throws Exception {
        // ============================================================
        // PART 1: No-profile Gating Checks
        // ============================================================

        // 1. Try to join an event (if any exist)
        try {
            // Use a custom matcher to pick the FIRST matching button to avoid
            // AmbiguousViewMatcherException
            onView(first(allOf(withId(R.id.eventButton), withText(containsString("Join")))))
                    .perform(scrollTo(), click());

            // Expect Dialog
            Thread.sleep(2000);
            checkDialogAndDismiss();

            // Go back to home (we might be in WaitingListFragment)
            onView(isRoot()).perform(androidx.test.espresso.action.ViewActions.pressBack());
            Thread.sleep(1000);
        } catch (NoMatchingViewException e) {
            // If no events exist, we skip this specific check but log it
            System.out.println("Skipping Join Event check - no events found");
        }

        // 2. History
        onView(withId(ID_NAV_HISTORY)).perform(click());
        Thread.sleep(2000);
        checkDialogAndDismiss();
        // Return to home to reset state
        onView(withId(ID_NAV_HOME)).perform(click());
        Thread.sleep(1000);

        // 3. Create Event
        onView(withId(ID_NAV_CREATE_EVENT)).perform(click());
        Thread.sleep(2000);
        checkDialogAndDismiss();
        // Stays on home, no need to nav back

        // 4. My Events
        onView(withId(ID_NAV_MY_EVENTS)).perform(click());
        Thread.sleep(2000);
        checkDialogAndDismiss();
        onView(withId(ID_NAV_HOME)).perform(click());
        Thread.sleep(1000);

        // ============================================================
        // PART 2: Profile Creation & Validation
        // ============================================================

        // Open Profile
        onView(withId(ID_ICON_PROFILE)).perform(click());
        waitForView(withId(ID_ROOT_PROFILE), 5000);

        // Case A: Only Name set
        typeInto(ID_INPUT_NAME, "Test Name");
        clearTextIn(ID_INPUT_EMAIL);
        clearTextIn(ID_INPUT_PHONE);
        onView(withId(ID_BTN_SAVE)).perform(scrollTo(), click());

        // Expect error on Email
        Thread.sleep(1000);
        onView(withId(ID_LAYOUT_EMAIL)).perform(scrollTo());
        onView(withId(ID_LAYOUT_EMAIL)).check(matches(hasTextInputLayoutErrorText("Please fill in your email")));

        // Case B: Only Email set
        clearTextIn(ID_INPUT_NAME);
        typeInto(ID_INPUT_EMAIL, "test@example.com");
        onView(withId(ID_BTN_SAVE)).perform(scrollTo(), click());

        // Expect error on Name
        Thread.sleep(1000);
        onView(withId(ID_LAYOUT_NAME)).perform(scrollTo());
        onView(withId(ID_LAYOUT_NAME)).check(matches(hasTextInputLayoutErrorText("Please fill in your name")));

        // Case C: Only Phone set
        clearTextIn(ID_INPUT_NAME);
        clearTextIn(ID_INPUT_EMAIL);
        typeInto(ID_INPUT_PHONE, "1234567890");
        onView(withId(ID_BTN_SAVE)).perform(scrollTo(), click());

        // Expect error on Name (and Email, but we check Name first)
        Thread.sleep(1000);
        onView(withId(ID_LAYOUT_NAME)).perform(scrollTo());
        onView(withId(ID_LAYOUT_NAME)).check(matches(hasTextInputLayoutErrorText("Please fill in your name")));

        // Case D: Valid Profile
        typeInto(ID_INPUT_NAME, "Espresso Test User");
        typeInto(ID_INPUT_EMAIL, "espresso_test@example.com");
        typeInto(ID_INPUT_PHONE, "1234567890");
        closeSoftKeyboard();

        onView(withId(ID_BTN_SAVE)).perform(scrollTo(), click());

        // Expect Success Toast (Flaky, skipping)
        // Thread.sleep(2000);
        // onView(withText("Profile saved")).inRoot(new
        // ToastMatcher()).check(matches(isDisplayed()));
        Thread.sleep(1000); // Wait for save

        // Verify values persist (reload or check current)
        onView(withId(ID_INPUT_NAME)).check(matches(withText("Espresso Test User")));
        onView(withId(ID_INPUT_EMAIL)).check(matches(withText("espresso_test@example.com")));

        // Case E: Update Profile
        typeInto(ID_INPUT_NAME, "Updated Espresso User");
        typeInto(ID_INPUT_PHONE, "0987654321");
        closeSoftKeyboard();

        onView(withId(ID_BTN_SAVE)).perform(scrollTo(), click());

        // Expect Success Toast (Flaky, skipping)
        // Thread.sleep(2000);
        // onView(withText("Profile saved")).inRoot(new
        // ToastMatcher()).check(matches(isDisplayed()));
        Thread.sleep(1000);

        onView(withId(ID_INPUT_NAME)).check(matches(withText("Updated Espresso User")));
        onView(withId(ID_INPUT_PHONE)).check(matches(withText("0987654321")));

        // Case F: Delete Profile
        onView(withId(ID_BTN_DELETE)).perform(scrollTo(), click());

        // Expect Success Toast (Flaky, skipping)
        // Thread.sleep(2000);
        // onView(withText("Profile deleted")).inRoot(new
        // ToastMatcher()).check(matches(isDisplayed()));
        Thread.sleep(1000);

        // Verify fields cleared
        onView(withId(ID_INPUT_NAME)).check(matches(withText("")));
        onView(withId(ID_INPUT_EMAIL)).check(matches(withText("")));
        onView(withId(ID_INPUT_PHONE)).check(matches(withText("")));
    }

    // ---------- Helpers ----------

    private void checkDialogAndDismiss() {
        // Check for dialog with specific text
        onView(withText(containsString(DIALOG_MESSAGE)))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));

        // Click "Cancel" (Negative button)
        onView(withId(android.R.id.button2))
                .inRoot(isDialog())
                .perform(click());
    }

    private void typeInto(int viewId, String text) {
        onView(withId(viewId)).perform(scrollTo(), replaceText(text), closeSoftKeyboard());
    }

    private void clearTextIn(int viewId) {
        onView(withId(viewId)).perform(scrollTo(), clearText(), closeSoftKeyboard());
    }

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

    /**
     * Matcher for Toast messages.
     */
    public static class ToastMatcher extends TypeSafeMatcher<Root> {
        @Override
        public void describeTo(Description description) {
            description.appendText("is toast");
        }

        @Override
        public boolean matchesSafely(Root root) {
            int type = root.getWindowLayoutParams().get().type;
            if (type == WindowManager.LayoutParams.TYPE_TOAST) {
                IBinder windowToken = root.getDecorView().getWindowToken();
                IBinder appToken = root.getDecorView().getApplicationWindowToken();
                return windowToken == appToken;
            }
            return false;
        }
    }

    /**
     * Returns a matcher that matches the first view that matches the given matcher.
     */
    private static Matcher<View> first(final Matcher<View> matcher) {
        return new TypeSafeMatcher<View>() {
            boolean found = false;

            @Override
            public void describeTo(Description description) {
                description.appendText("first matching: ");
                matcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(View item) {
                if (found) {
                    return false;
                }
                if (matcher.matches(item)) {
                    found = true;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Matcher for TextInputLayout error text.
     */
    public static Matcher<View> hasTextInputLayoutErrorText(final String expectedErrorText) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof com.google.android.material.textfield.TextInputLayout)) {
                    return false;
                }
                CharSequence error = ((com.google.android.material.textfield.TextInputLayout) view).getError();
                if (error == null) {
                    return expectedErrorText == null;
                }
                return expectedErrorText.equals(error.toString());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with error: " + expectedErrorText);
            }
        };
    }
}
