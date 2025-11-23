package com.example.yellow.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.yellow.R;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
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
 * Unit tests for ManageEventsFragment using Robolectric.
 * Verifies loading and deleting events from a mocked Firestore database.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ManageEventsFragmentTest {

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
    private DocumentReference mockEventDocRef;
    @Mock
    private Task<Void> mockDeleteTask;

    private MockedStatic<FirebaseAuth> mockedAuthStatic;
    private MockedStatic<FirebaseFirestore> mockedDbStatic;
    private MockedStatic<com.example.yellow.utils.DeviceIdentityManager> mockedDeviceIdentity;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockedAuthStatic = mockStatic(FirebaseAuth.class);
        mockedDbStatic = mockStatic(FirebaseFirestore.class);
        mockedDeviceIdentity = mockStatic(com.example.yellow.utils.DeviceIdentityManager.class);

        mockedAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
        mockedDbStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        // Mock DeviceIdentityManager to always return admin = true
        mockedDeviceIdentity
                .when(com.example.yellow.utils.DeviceIdentityManager::isCurrentDeviceAdmin)
                .thenReturn(true);

        @SuppressWarnings("unchecked")
        Task<Boolean> mockAdminTask = mock(Task.class);
        mockBooleanTaskSuccess(mockAdminTask, true);

        mockedDeviceIdentity
                .when(() -> com.example.yellow.utils.DeviceIdentityManager.ensureDeviceDocument(any(), any()))
                .thenReturn(mockAdminTask);

        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn("test_admin_id");

        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.get()).thenReturn(mockQueryTask);

        // Default: addSnapshotListener returns a registration
        when(mockEventsCollection.addSnapshotListener(any())).thenAnswer(invocation -> {
            // We can trigger the listener immediately if we want, or handle it via manual
            // invocation
            // For simplicity in these tests, we'll assume the fragment uses
            // addSnapshotListener
            // and we trigger it by capturing the listener or mocking the behavior.
            // However, the fragment uses addSnapshotListener.
            // We need to capture the EventListener and call onEvent.
            com.google.firebase.firestore.EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(mockQuerySnapshot, null);
            return mock(com.google.firebase.firestore.ListenerRegistration.class);
        });
        // Mock document() globally to avoid NPEs if called with other IDs
        when(mockEventsCollection.document(anyString())).thenReturn(mockEventDocRef);

        // Mock delete task success by default
        when(mockEventDocRef.delete()).thenReturn(mockDeleteTask);
        mockTaskSuccess(mockDeleteTask);
    }

    @After
    public void tearDown() {
        mockedAuthStatic.close();
        mockedDbStatic.close();
        mockedDeviceIdentity.close();
    }

    @Test
    public void testNoEventsToManage() {
        // Mock empty snapshot
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        when(mockQuerySnapshot.getDocuments()).thenReturn(Collections.emptyList());

        FragmentScenario<ManageEventsFragment> scenario = FragmentScenario.launchInContainer(
                ManageEventsFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 1 child: the "No events found" TextView
            assertEquals(1, listContainer.getChildCount());
            View child = listContainer.getChildAt(0);
            assertTrue(child instanceof TextView);
            assertEquals("No events found.", ((TextView) child).getText().toString());
        });
    }

    @Test
    public void testSingleEventInList() {
        // Mock 1 event
        List<DocumentSnapshot> docs = new ArrayList<>();
        docs.add(createMockEventSnapshot("event_1", "Test Event"));

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageEventsFragment> scenario = FragmentScenario.launchInContainer(
                ManageEventsFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 1 child: the event card
            assertEquals(1, listContainer.getChildCount());

            View card = listContainer.getChildAt(0);
            TextView tvTitle = card.findViewById(R.id.title);
            assertEquals("Test Event", tvTitle.getText().toString());
        });
    }

    @Test
    public void testManyEventsInList() {
        // Mock 20 events
        List<DocumentSnapshot> docs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            docs.add(createMockEventSnapshot("event_" + i, "Event " + i));
        }

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageEventsFragment> scenario = FragmentScenario.launchInContainer(
                ManageEventsFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 20 children
            assertEquals(20, listContainer.getChildCount());
        });
    }

    @Test
    public void testDeleteEventRemovesItFromFirestore() {
        // Mock 1 event
        List<DocumentSnapshot> docs = new ArrayList<>();
        docs.add(createMockEventSnapshot("event_to_delete", "Delete Me"));

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageEventsFragment> scenario = FragmentScenario.launchInContainer(
                ManageEventsFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            View card = listContainer.getChildAt(0);
            MaterialButton btnDelete = card.findViewById(R.id.btnDelete);

            // Perform click
            btnDelete.performClick();
        });

        // Check for dialog and click positive button
        android.app.AlertDialog dialog = (android.app.AlertDialog) org.robolectric.shadows.ShadowAlertDialog
                .getLatestDialog();
        assertTrue(dialog.isShowing());
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();

        // Ensure async tasks (like dialog listeners) execute
        org.robolectric.shadows.ShadowLooper.idleMainLooper();

        // Verify delete was called on the correct document
        verify(mockEventsCollection).document("event_to_delete");
        verify(mockEventDocRef).delete();
    }

    // --- Helpers ---

    private DocumentSnapshot createMockEventSnapshot(String id, String name) {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.getId()).thenReturn(id);
        when(doc.getString("name")).thenReturn(name);
        when(doc.getString("organizerName")).thenReturn("Test Organizer");
        when(doc.getString("posterImageUrl")).thenReturn(null);
        // Mock timestamp for date
        when(doc.get("startDate")).thenReturn(null);
        return doc;
    }

    private void mockBooleanTaskSuccess(Task<Boolean> task, boolean value) {
        when(task.addOnSuccessListener(any())).thenAnswer(inv -> {
            com.google.android.gms.tasks.OnSuccessListener<Boolean> listener = inv.getArgument(0);
            listener.onSuccess(value);
            return task;
        });
        when(task.addOnFailureListener(any())).thenReturn(task);
    }

    private void mockTaskSuccess(Task<Void> task) {
        when(task.addOnSuccessListener(any())).thenAnswer(inv -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> listener = inv.getArgument(0);
            listener.onSuccess(null);
            return task;
        });
        when(task.addOnFailureListener(any())).thenReturn(task);
    }
}
