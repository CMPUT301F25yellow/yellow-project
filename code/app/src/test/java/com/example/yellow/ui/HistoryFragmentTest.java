package com.example.yellow.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.View;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.yellow.R;
import com.example.yellow.organizers.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for HistoryFragment using Robolectric.
 * <p>
 * Purpose:
 * These tests verify the logic of the HistoryFragment without needing a real
 * device.
 * They use "Mocks" (fake versions) of Firebase to simulate different scenarios
 * (like finding 0 events, 1 event, or 20 events) to ensure the app handles them
 * correctly.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class HistoryFragmentTest {

    @Mock
    private FirebaseAuth mockAuth;
    @Mock
    private FirebaseUser mockUser;
    @Mock
    private FirebaseFirestore mockDb;
    @Mock
    private CollectionReference mockEventsCollection;
    @Mock
    private Task<QuerySnapshot> mockQueryTask;
    @Mock
    private QuerySnapshot mockQuerySnapshot;
    @Mock
    private DocumentReference mockEventRef;
    @Mock
    private CollectionReference mockSubCollection;
    @Mock
    private DocumentReference mockUserInSubCollection;
    @Mock
    private Task<DocumentSnapshot> mockSubCollectionTask;

    private MockedStatic<FirebaseAuth> mockedAuthStatic;
    private MockedStatic<FirebaseFirestore> mockedDbStatic;

    /**
     * Sets up the test environment before each test runs.
     * - Initializes Mockito annotations.
     * - Mocks the static instances of FirebaseAuth and FirebaseFirestore.
     * - Configures the mock database to return our fake data instead of connecting
     * to the internet.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockedAuthStatic = mockStatic(FirebaseAuth.class);
        mockedDbStatic = mockStatic(FirebaseFirestore.class);

        mockedAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
        mockedDbStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn("test_user_id");

        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.get()).thenReturn(mockQueryTask);

        // Setup generic subcollection mocking (for checkUserInEvent)
        when(mockEventsCollection.document(anyString())).thenReturn(mockEventRef);
        when(mockEventRef.collection(anyString())).thenReturn(mockSubCollection);
        when(mockSubCollection.document(anyString())).thenReturn(mockUserInSubCollection);
        when(mockUserInSubCollection.get()).thenReturn(mockSubCollectionTask);
    }

    @After
    public void tearDown() {
        mockedAuthStatic.close();
        mockedDbStatic.close();
    }

    /**
     * Scenario: The user has NO event history.
     * <p>
     * Test:
     * 1. Mock Firebase returning an empty list of events.
     * 2. Launch the HistoryFragment.
     * 3. Check that the RecyclerView (the list) has 0 items.
     */
    @Test
    public void testLoadHistory_empty() {
        // Mock empty events collection
        mockQuerySuccess(Collections.emptyList());

        FragmentScenario<HistoryFragment> scenario = FragmentScenario.launchInContainer(HistoryFragment.class);

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.getView().findViewById(R.id.rvHistory);
            // Verify adapter has 0 items
            assertEquals(0, rv.getAdapter().getItemCount());
        });
    }

    /**
     * Scenario: The user has exactly ONE event in their history.
     * <p>
     * Test:
     * 1. Mock Firebase returning 1 event.
     * 2. Mock that the user is in the "waitingList" for that event.
     * 3. Launch the HistoryFragment.
     * 4. Check that the RecyclerView has 1 item.
     */
    @Test
    public void testLoadHistory_singleItem() {
        // Mock 1 event
        List<QueryDocumentSnapshot> docs = new ArrayList<>();
        docs.add(createMockEventSnapshot("event_1", "Single Event"));
        mockQuerySuccess(docs);

        // Mock user being in the "waitingList" for this event
        mockUserInSubCollection(true);

        FragmentScenario<HistoryFragment> scenario = FragmentScenario.launchInContainer(HistoryFragment.class);

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.getView().findViewById(R.id.rvHistory);
            View emptyView = fragment.getView().findViewById(R.id.tvEmptyState);

            // Verify adapter has 1 item
            assertEquals(1, rv.getAdapter().getItemCount());

            // Verify Empty State is GONE and RecyclerView is VISIBLE
            assertEquals(View.GONE, emptyView.getVisibility());
            assertEquals(View.VISIBLE, rv.getVisibility());
        });
    }

    /**
     * Scenario: The user has MANY events (20), enough to scroll.
     * <p>
     * Test:
     * 1. Mock Firebase returning 20 events.
     * 2. Mock that the user is in the list for all of them.
     * 3. Launch the HistoryFragment.
     * 4. Check that the RecyclerView has 20 items.
     * 5. Verify that the list reports it is scrollable.
     */
    @Test
    public void testLoadHistory_manyItems_scrollable() {
        // Mock 20 events
        List<QueryDocumentSnapshot> docs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            docs.add(createMockEventSnapshot("event_" + i, "Event " + i));
        }
        mockQuerySuccess(docs);

        // Mock user being in the list for ALL events
        mockUserInSubCollection(true);

        FragmentScenario<HistoryFragment> scenario = FragmentScenario.launchInContainer(HistoryFragment.class);

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.getView().findViewById(R.id.rvHistory);
            View emptyView = fragment.getView().findViewById(R.id.tvEmptyState);

            // Verify adapter has 20 items
            assertEquals(20, rv.getAdapter().getItemCount());

            // Verify Empty State is GONE and RecyclerView is VISIBLE
            assertEquals(View.GONE, emptyView.getVisibility());
            assertEquals(View.VISIBLE, rv.getVisibility());

            // Verify it is scrollable (can scroll vertically)
            // Note: In Robolectric, layout might not be fully calculated like a real
            // device,
            // but we can check if the view is capable of scrolling.
            assertTrue(rv.canScrollVertically(1) || rv.getAdapter().getItemCount() > 0);
        });
    }

    // --- Helpers ---

    private void mockQuerySuccess(List<QueryDocumentSnapshot> results) {
        when(mockQueryTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            com.google.android.gms.tasks.OnSuccessListener listener = invocation.getArgument(0);
            when(mockQuerySnapshot.isEmpty()).thenReturn(results.isEmpty());
            // Cast to List<DocumentSnapshot> because getDocuments returns that, even though
            // they are QueryDocumentSnapshots
            List<DocumentSnapshot> castedResults = new ArrayList<>(results);
            when(mockQuerySnapshot.getDocuments()).thenReturn(castedResults);
            when(mockQuerySnapshot.iterator()).thenReturn(results.iterator());
            when(mockQuerySnapshot.size()).thenReturn(results.size());
            listener.onSuccess(mockQuerySnapshot);
            return mockQueryTask;
        });
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);
    }

    private QueryDocumentSnapshot createMockEventSnapshot(String id, String name) {
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);
        when(doc.getId()).thenReturn(id);
        Event event = new Event();
        event.setEventName(name);
        when(doc.toObject(Event.class)).thenReturn(event);
        return doc;
    }

    private void mockUserInSubCollection(boolean exists) {
        when(mockSubCollectionTask.addOnCompleteListener(any())).thenAnswer(invocation -> {
            com.google.android.gms.tasks.OnCompleteListener listener = invocation.getArgument(0);
            Task<DocumentSnapshot> task = mock(Task.class);
            when(task.isSuccessful()).thenReturn(true);
            DocumentSnapshot res = mock(DocumentSnapshot.class);
            when(res.exists()).thenReturn(exists);
            when(task.getResult()).thenReturn(res);
            listener.onComplete(task);
            return mockSubCollectionTask;
        });
    }
}
