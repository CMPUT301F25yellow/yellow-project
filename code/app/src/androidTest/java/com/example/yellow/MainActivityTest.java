package com.example.yellow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.yellow.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    // Test 1 — Clicking Profile opens ProfileUserFragment
    @Test
    public void clickProfileIcon_OpensProfileFragment() {
        ActivityScenario.launch(MainActivity.class);

        onView(withId(R.id.iconProfile)).perform(click());

        // MUST check root view of ProfileUserFragment
        onView(withId(R.id.root_profile)).check(matches(isDisplayed()));
    }

    // Test 2 — Clicking Notifications opens NotificationFragment
    @Test
    public void clickNotificationsIcon_OpensNotificationFragment() {
        ActivityScenario.launch(MainActivity.class);

        onView(withId(R.id.iconNotifications)).perform(click());

        onView(withId(R.id.notifications_root)).check(matches(isDisplayed()));
    }

    //NOT IN PLACE YET
    // Test 3 — History bottom nav
//    @Test
//    public void bottomNav_History_OpensHistoryFragment() {
//        ActivityScenario.launch(MainActivity.class);
//
//        onView(withId(R.id.nav_history)).perform(click());
//
//        onView(withId(R.id.root_history)).check(matches(isDisplayed()));
//    }

    // Test 4 — My Events bottom nav
    @Test
    public void bottomNav_MyEvents_OpensMyEventsFragment() {
        ActivityScenario.launch(MainActivity.class);

        onView(withId(R.id.nav_my_events)).perform(click());

        onView(withId(R.id.root_my_events)).check(matches(isDisplayed()));
    }

    // Test 5 — QR Scan bottom nav
    @Test
    public void bottomNav_QRScan_OpensQrScanFragment() {
        ActivityScenario.launch(MainActivity.class);

        onView(withId(R.id.nav_scan)).perform(click());

        onView(withId(R.id.qr_root)).check(matches(isDisplayed()));
    }

    // Test 6 — openWaitingRoom loads WaitingListFragment
    @Test
    public void openWaitingRoom_LoadsWaitingListFragment() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        scenario.onActivity(activity -> activity.openWaitingRoom("TEST_EVENT_123"));

        onView(withId(R.id.root_waiting_room)).check(matches(isDisplayed()));
    }

    // Test 7 — Back button restores home UI
    @Test
    public void backButton_PopsFragment_AndReturnsHome() {
        ActivityScenario.launch(MainActivity.class);

        onView(withId(R.id.nav_history)).perform(click());

        pressBack();

        onView(withId(R.id.header_main)).check(matches(isDisplayed()));
        onView(withId(R.id.scrollContent)).check(matches(isDisplayed()));
    }
}
