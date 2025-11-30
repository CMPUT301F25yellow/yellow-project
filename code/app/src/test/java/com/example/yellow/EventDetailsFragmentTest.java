package com.example.yellow;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.target.ViewTarget;
import com.example.yellow.organizers.Event;
import com.example.yellow.ui.EventDetailsFragment;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EventDetailsFragmentTest {

    // Mocks for Firebase services
    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockEventsCollection;
    @Mock private DocumentReference mockEventDoc;
    @Mock private Task<DocumentSnapshot> mockGetTask;
    @Mock private ListenerRegistration mockListenerRegistration;

    // Mocks for Glide image loading
    @Mock private RequestManager mockGlideRequestManager;
    @Mock private RequestBuilder<Drawable> mockRequestBuilder;
    @Mock ViewTarget mockViewTarget;


    private MockedStatic<FirebaseFirestore> mockedDbStatic;
    private MockedStatic<Glide> mockedGlideStatic;

    private final String TEST_EVENT_ID = "test-event-123";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock the static getInstance() methods to return our mock objects
        mockedDbStatic = mockStatic(FirebaseFirestore.class);
        mockedDbStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        // Mock Glide to prevent it from trying to load images during the test
        mockedGlideStatic = mockStatic(Glide.class);
        mockedGlideStatic.when(() -> Glide.with(any(Fragment.class))).thenReturn(mockGlideRequestManager);
        when(mockGlideRequestManager.load(anyString())).thenReturn(mockRequestBuilder);
        when(mockRequestBuilder.into(any(ImageView.class))).thenReturn(mockViewTarget);

        // Chain the mock calls for Firestore
        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(anyString())).thenReturn(mockEventDoc);
        when(mockEventDoc.get()).thenReturn(mockGetTask);

        // Also mock the waitingList listener to prevent crashes
        CollectionReference mockWaitingList = mock(CollectionReference.class);
        when(mockEventDoc.collection("waitingList")).thenReturn(mockWaitingList);
        when(mockWaitingList.addSnapshotListener(any())).thenReturn(mockListenerRegistration);
    }

    @After
    public void tearDown() {
        // Close the static mocks
        mockedDbStatic.close();
        mockedGlideStatic.close();
    }

    // Helper to launch the fragment consistently
    private FragmentScenario<EventDetailsFragment> launchFragmentWithArgs(String eventId) {
        Bundle args = new Bundle();
        String qrData = "yellow://eventdetails/" + eventId;
        args.putString("qr_code_data", qrData);
        return FragmentScenario.launchInContainer(
                EventDetailsFragment.class,
                args,
                R.style.Theme_Yellow,
                Lifecycle.State.RESUMED
        );
    }

    // Event details are displayed correctly
    @Test
    public void eventDetails_areDisplayedCorrectly() {
        // 1. Arrange: Create fake event data and mock the Firestore response
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Test Music Festival");
        data.put("organizerName", "Test Organizer");
        data.put("location", "Test Location");
        data.put("description", "An event for testing.");
        mockEventDocument(TEST_EVENT_ID, data);

        // 2. Act: Launch the fragment
        FragmentScenario<EventDetailsFragment> scenario = launchFragmentWithArgs(TEST_EVENT_ID);

        // 3. Assert: Check the TextViews directly
        scenario.onFragment(fragment -> {
            TextView title = fragment.getView().findViewById(R.id.eventTitle);
            TextView organizer = fragment.getView().findViewById(R.id.eventOrganizer);
            TextView location = fragment.getView().findViewById(R.id.eventLocation);
            TextView description = fragment.getView().findViewById(R.id.eventDescription);

            assertEquals("Test Music Festival", title.getText().toString());
            assertEquals("Test Organizer", organizer.getText().toString());
            assertEquals("Test Location", location.getText().toString());
            assertEquals("An event for testing.", description.getText().toString());
        });
    }

    // Event not fount, invalid Id
    @Test
    public void eventDetails_showsNotFound_forInvalidId() {
        // 1. Arrange: Mock the Firestore response for a non-existent document
        mockEventDocument("invalid-id", null);

        // 2. Act: Launch the fragment with an invalid ID
        FragmentScenario<EventDetailsFragment> scenario = launchFragmentWithArgs("invalid-id");

        // 3. Assert: Check that the header shows the "Not Found" message
        scenario.onFragment(fragment -> {
            TextView header = fragment.getView().findViewById(R.id.headerTitle);
            assertEquals("Event Not Found", header.getText().toString());
        });
    }

    // Event with missing data
    @Test
    public void eventDetails_handlesMissingData_gracefully() {
        // 1. Arrange: Create partial data and mock the Firestore response
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Partial Event");
        data.put("organizerName", "Partial Organizer");
        // location and description are missing
        mockEventDocument("partial-event", data);

        // 2. Act: Launch the fragment
        FragmentScenario<EventDetailsFragment> scenario = launchFragmentWithArgs("partial-event");

        // 3. Assert: Check that views are populated correctly
        scenario.onFragment(fragment -> {
            TextView title = fragment.getView().findViewById(R.id.eventTitle);
            TextView organizer = fragment.getView().findViewById(R.id.eventOrganizer);
            TextView location = fragment.getView().findViewById(R.id.eventLocation);
            TextView description = fragment.getView().findViewById(R.id.eventDescription);

            assertEquals("Partial Event", title.getText().toString());
            assertEquals("Partial Organizer", organizer.getText().toString());
            assertEquals("", location.getText().toString()); // Should be empty
            assertEquals("", description.getText().toString()); // Should be empty
        });
    }

    /**
     * Helper method to configure the mock DocumentReference and Task
     * to return a specific DocumentSnapshot.
     * @param eventId The event ID to mock the document for.
     * @param data The data to put in the mock snapshot. If null, the snapshot will not exist.
     */
    private void mockEventDocument(String eventId, Map<String, Object> data) {
        when(mockEventsCollection.document(eventId)).thenReturn(mockEventDoc);

        when(mockGetTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
            if (data != null) {
                when(snapshot.exists()).thenReturn(true);

                // Mock direct getString calls for all fields
                when(snapshot.getString("name")).thenReturn((String) data.get("name"));
                when(snapshot.getString("organizerName")).thenReturn((String) data.get("organizerName"));
                when(snapshot.getString("location")).thenReturn((String) data.get("location"));
                when(snapshot.getString("description")).thenReturn((String) data.get("description"));

                // Also mock the toObject conversion in case it's used
                Event mockEvent = new Event(
                        eventId,
                        (String)data.get("name"),
                        (String)data.get("description"),
                        (String)data.get("location"),
                        new Timestamp(new Date()), new Timestamp(new Date()),
                        "",
                        0,
                        "",
                        (String)data.get("organizerName"),
                        false,
                        "",
                        ""
                );
                when(snapshot.toObject(Event.class)).thenReturn(mockEvent);

            } else {
                when(snapshot.exists()).thenReturn(false);
            }
            // Trigger the onSuccess listener with our mock snapshot
            invocation.<com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>>getArgument(0)
                    .onSuccess(snapshot);
            return mockGetTask;
        });
    }
}
