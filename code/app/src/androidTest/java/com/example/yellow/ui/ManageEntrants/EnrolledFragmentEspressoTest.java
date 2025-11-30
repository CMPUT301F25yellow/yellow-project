package com.example.yellow.ui.ManageEntrants;

import android.os.Bundle;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.action.ViewActions;

import com.example.yellow.R;
import com.example.yellow.TestActivity;
import com.example.yellow.ui.ManageEntrants.EnrolledFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class EnrolledFragmentEspressoTest {

    @Test
    public void fragmentLoadsUI() {
        ActivityScenario<TestActivity> scenario =
                ActivityScenario.launch(TestActivity.class);

        scenario.onActivity(activity -> {
            EnrolledFragment fragment = new EnrolledFragment();
            Bundle args = new Bundle();
            args.putString("eventId", "test-event-id");
            fragment.setArguments(args);

            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commitNow();
        });

        onView(withId(R.id.enrolledContainer))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        onView(withId(R.id.btnNotifyEnrolled))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void clickingNotifyButtonOpensDialog() {
        ActivityScenario<TestActivity> scenario =
                ActivityScenario.launch(TestActivity.class);

        scenario.onActivity(activity -> {
            EnrolledFragment fragment = new EnrolledFragment();
            Bundle args = new Bundle();
            args.putString("eventId", "test-event-id");
            fragment.setArguments(args);

            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commitNow();
        });

        onView(withId(R.id.btnNotifyEnrolled))
                .perform(ViewActions.click());

        onView(withText("Notify Enrolled Entrants"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }
}
