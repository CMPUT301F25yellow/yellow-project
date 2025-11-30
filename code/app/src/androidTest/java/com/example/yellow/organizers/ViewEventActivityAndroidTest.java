package com.example.yellow.organizers;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.yellow.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ViewEventActivityAndroidTest {

    private Intent createIntentWithEventId() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                ViewEventActivity.class
        );
        intent.putExtra("eventId", "dummyEventId");
        return intent;
    }

    @Test
    public void launchesWithEventId_showsCoreUiScaffold() {
        Intent intent = createIntentWithEventId();

        try (ActivityScenario<ViewEventActivity> scenario = ActivityScenario.launch(intent)) {

            // Back button visible
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));

            // Header text views: at least VISIBLE (even if width is 0 initially)
            onView(withId(R.id.tvEventName))
                    .check(matches(withEffectiveVisibility(VISIBLE)));
            onView(withId(R.id.tvEventDate))
                    .check(matches(withEffectiveVisibility(VISIBLE)));

            // Tabs + pager visible
            onView(withId(R.id.tabLayout)).check(matches(isDisplayed()));
            onView(withId(R.id.viewPager)).check(matches(isDisplayed()));

            // Poster/settings button visible
            onView(withId(R.id.btnEventSettings)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void backButtonClick_marksActivityAsFinishing() {
        Intent intent = createIntentWithEventId();

        try (ActivityScenario<ViewEventActivity> scenario = ActivityScenario.launch(intent)) {

            // Click the back button
            onView(withId(R.id.btnBack)).perform(click());

            // Check on the main thread that the Activity is finishing or destroyed
            scenario.onActivity(activity -> {
                assertTrue(activity.isFinishing() || activity.isDestroyed());
            });
        }
    }
}
