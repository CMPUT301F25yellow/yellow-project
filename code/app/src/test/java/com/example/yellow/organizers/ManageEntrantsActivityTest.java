package com.example.yellow.organizers;

import static org.junit.Assert.*;

import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.yellow.R;
import com.example.yellow.ui.ManageEntrants.ManageEntrantsActivity;
import com.example.yellow.ui.ManageEntrants.WaitingFragment;
import com.example.yellow.ui.ManageEntrants.SelectedFragment;
import com.example.yellow.ui.ManageEntrants.CancelledFragment;
import com.example.yellow.ui.ManageEntrants.EnrolledFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

/**
 * Tests for ManageEntrantsActivity using Robolectric.
 * These tests verify UI behavior, tab setup, and fragment loading
 * while Firestore interactions are fully mocked.
 */
@RunWith(RobolectricTestRunner.class)
public class ManageEntrantsActivityTest {

    private ManageEntrantsActivity activity;
    private MockedStatic<FirebaseFirestore> firestoreMock;

    @Before
    public void setup() {

        // Create all required Firestore mock objects
        FirebaseFirestore mockDb = Mockito.mock(FirebaseFirestore.class);
        CollectionReference mockCollection = Mockito.mock(CollectionReference.class);
        DocumentReference mockDocument = Mockito.mock(DocumentReference.class);
        Task<QuerySnapshot> mockTask = Mockito.mock(Task.class);

        // Make db.collection("...") return our collection mock
        Mockito.when(mockDb.collection(Mockito.anyString()))
                .thenReturn(mockCollection);

        // Make collection.document("...") return our document mock
        Mockito.when(mockCollection.document(Mockito.anyString()))
                .thenReturn(mockDocument);

        // When nested collections are requested, return collection mock again
        Mockito.when(mockDocument.collection(Mockito.anyString()))
                .thenReturn(mockCollection);

        // Simulate snapshot listeners without doing anything
        Mockito.when(mockCollection.addSnapshotListener(Mockito.any()))
                .thenReturn(null);

        // Simulate get() returning a Task object
        Mockito.when(mockCollection.get())
                .thenReturn(mockTask);

        // Make addOnSuccessListener / addOnFailureListener return the same task
        Mockito.when(mockTask.addOnSuccessListener(Mockito.any()))
                .thenReturn(mockTask);
        Mockito.when(mockTask.addOnFailureListener(Mockito.any()))
                .thenReturn(mockTask);

        // Mock FirebaseFirestore.getInstance() so that it returns our fake db
        firestoreMock = Mockito.mockStatic(FirebaseFirestore.class);
        firestoreMock.when(FirebaseFirestore::getInstance)
                .thenReturn(mockDb);

        // Launch the activity under test
        Intent intent = new Intent();
        intent.putExtra("eventId", "abc123");
        intent.putExtra("eventName", "Sample Event");

        activity = Robolectric.buildActivity(ManageEntrantsActivity.class, intent)
                .setup()
                .get();
    }

    @After
    public void teardown() {
        // Always close the static mock when the test ends
        firestoreMock.close();
    }

    /**
     * Ensures the activity title displays the event name passed via intent.
     */
    @Test
    public void titleDisplaysEventName() {
        TextView title = activity.findViewById(R.id.tvEventTitle);
        assertEquals("Sample Event", title.getText().toString());
    }

    /**
     * Verifies that tapping the back button finishes the activity.
     */
    @Test
    public void backButtonFinishesActivity() {
        ImageView back = activity.findViewById(R.id.btnBack);
        back.performClick();
        assertTrue(activity.isFinishing());
    }

    /**
     * Checks that the TabLayout creates four tabs with the expected labels.
     */
    @Test
    public void tabLayoutHasFourTabs() {
        TabLayout tabs = activity.findViewById(R.id.tabLayout);

        assertEquals(4, tabs.getTabCount());
        assertEquals("Waiting", tabs.getTabAt(0).getText());
        assertEquals("Selected", tabs.getTabAt(1).getText());
        assertEquals("Cancelled", tabs.getTabAt(2).getText());
        assertEquals("Enrolled", tabs.getTabAt(3).getText());
    }

    /**
     * Confirms that the ViewPager initially loads the WaitingFragment.
     */
    @Test
    public void viewPagerStartsWithWaitingFragment() {
        ViewPager2 pager = activity.findViewById(R.id.viewPager);

        Fragment current = activity.getSupportFragmentManager()
                .findFragmentByTag("f" + pager.getCurrentItem());

        assertTrue(current instanceof WaitingFragment);
    }

    /**
     * Tests that switching ViewPager pages loads the correct fragments.
     */
    @Test
    public void switchingTabsChangesFragments() {
        ViewPager2 pager = activity.findViewById(R.id.viewPager);

        // Switch to "Selected"
        pager.setCurrentItem(1, false);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        Fragment f1 = activity.getSupportFragmentManager().findFragmentByTag("f1");
        assertTrue(f1 instanceof SelectedFragment);

        // Switch to "Cancelled"
        pager.setCurrentItem(2, false);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        Fragment f2 = activity.getSupportFragmentManager().findFragmentByTag("f2");
        assertTrue(f2 instanceof CancelledFragment);

        // Switch to "Enrolled"
        pager.setCurrentItem(3, false);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        Fragment f3 = activity.getSupportFragmentManager().findFragmentByTag("f3");
        assertTrue(f3 instanceof EnrolledFragment);
    }
}
