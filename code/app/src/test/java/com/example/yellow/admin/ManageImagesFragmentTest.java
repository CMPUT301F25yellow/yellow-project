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
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.yellow.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for ManageImagesFragment using Robolectric.
 * Verifies loading and deleting poster images from a mocked Firestore database.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ManageImagesFragmentTest {

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
    private Task<Void> mockUpdateTask;

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

        // Mock addSnapshotListener
        when(mockEventsCollection.addSnapshotListener(any())).thenAnswer(invocation -> {
            com.google.firebase.firestore.EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(mockQuerySnapshot, null);
            return mock(com.google.firebase.firestore.ListenerRegistration.class);
        });

        // Mock document() globally
        when(mockEventsCollection.document(anyString())).thenReturn(mockEventDocRef);

        // Mock update() for deletion
        when(mockEventDocRef.update(any(Map.class))).thenReturn(mockUpdateTask);
        mockTaskSuccess(mockUpdateTask);
    }

    @After
    public void tearDown() {
        mockedAuthStatic.close();
        mockedDbStatic.close();
        mockedDeviceIdentity.close();
    }

    @Test
    public void testNoImagesToManage() {
        // Mock empty snapshot
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        when(mockQuerySnapshot.getDocuments()).thenReturn(Collections.emptyList());

        FragmentScenario<ManageImagesFragment> scenario = FragmentScenario.launchInContainer(
                ManageImagesFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            GridLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 1 child: the "No poster images found." TextView
            assertEquals(1, listContainer.getChildCount());
            View child = listContainer.getChildAt(0);
            assertTrue(child instanceof TextView);
            assertEquals("No poster images found.", ((TextView) child).getText().toString());
        });
    }

    @Test
    public void testSingleImage() {
        // Mock 1 event with a poster
        List<DocumentSnapshot> docs = new ArrayList<>();
        docs.add(createMockEventSnapshot("event_1", "Test Event", "http://example.com/poster.jpg"));

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageImagesFragment> scenario = FragmentScenario.launchInContainer(
                ManageImagesFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            GridLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 1 child: the image card
            assertEquals(1, listContainer.getChildCount());

            View card = listContainer.getChildAt(0);
            TextView tvTitle = card.findViewById(R.id.title);
            // The fragment prefixes "Event : "
            assertEquals("Event : Test Event", tvTitle.getText().toString());
        });
    }

    @Test
    public void testManyImages() {
        // Mock 10 events with posters
        List<DocumentSnapshot> docs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            docs.add(createMockEventSnapshot("event_" + i, "Event " + i, "http://example.com/poster_" + i + ".jpg"));
        }

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageImagesFragment> scenario = FragmentScenario.launchInContainer(
                ManageImagesFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            GridLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 10 children
            assertEquals(10, listContainer.getChildCount());
        });
    }

    @Test
    public void testDeleteImageRemovesItFromDataSource() {
        // Mock 1 event with a poster
        List<DocumentSnapshot> docs = new ArrayList<>();
        docs.add(createMockEventSnapshot("event_to_delete", "Delete Me", "http://example.com/poster.jpg"));

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageImagesFragment> scenario = FragmentScenario.launchInContainer(
                ManageImagesFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            GridLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            View card = listContainer.getChildAt(0);
            View btnDelete = card.findViewById(R.id.btnDeleteImage);

            // Perform click
            btnDelete.performClick();
        });

        // Check for dialog and click positive button
        android.app.AlertDialog dialog = (android.app.AlertDialog) org.robolectric.shadows.ShadowAlertDialog
                .getLatestDialog();
        assertTrue(dialog.isShowing());
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();

        // Ensure async tasks execute
        ShadowLooper.idleMainLooper();

        // Verify update was called to remove poster fields
        verify(mockEventsCollection).document("event_to_delete");
        verify(mockEventDocRef).update(any(Map.class));
    }

    // --- Helpers ---

    private DocumentSnapshot createMockEventSnapshot(String id, String name, String posterUrl) {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.getId()).thenReturn(id);
        when(doc.getString("name")).thenReturn(name);
        when(doc.getString("organizerName")).thenReturn("Test Organizer");
        when(doc.getString("posterImageUrl")).thenReturn(posterUrl);
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
