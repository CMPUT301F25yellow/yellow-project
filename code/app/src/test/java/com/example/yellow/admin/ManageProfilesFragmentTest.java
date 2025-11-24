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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

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

/**
 * Unit tests for ManageProfilesFragment using Robolectric.
 * Verifies loading and deleting profiles from a mocked Firestore database.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ManageProfilesFragmentTest {

    @Mock
    private FirebaseAuth mockAuth;
    @Mock
    private FirebaseUser mockUser;
    @Mock
    private FirebaseFirestore mockDb;
    @Mock
    private CollectionReference mockProfilesCollection;
    @Mock
    private CollectionReference mockRolesCollection;
    @Mock
    private Task<QuerySnapshot> mockQueryTask;
    @Mock
    private QuerySnapshot mockQuerySnapshot;
    @Mock
    private DocumentReference mockProfileDocRef;
    @Mock
    private DocumentReference mockRoleDocRef;
    @Mock
    private WriteBatch mockBatch;
    @Mock
    private Task<Void> mockBatchCommitTask;

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

        // Mock collections
        when(mockDb.collection("profiles")).thenReturn(mockProfilesCollection);
        when(mockDb.collection("roles")).thenReturn(mockRolesCollection);

        when(mockProfilesCollection.get()).thenReturn(mockQueryTask);

        // Mock addSnapshotListener
        when(mockProfilesCollection.addSnapshotListener(any())).thenAnswer(invocation -> {
            com.google.firebase.firestore.EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(mockQuerySnapshot, null);
            return mock(com.google.firebase.firestore.ListenerRegistration.class);
        });

        // Mock document() globally
        when(mockProfilesCollection.document(anyString())).thenReturn(mockProfileDocRef);
        when(mockRolesCollection.document(anyString())).thenReturn(mockRoleDocRef);

        // Mock Batch
        when(mockDb.batch()).thenReturn(mockBatch);
        when(mockBatch.delete(any(DocumentReference.class))).thenReturn(mockBatch);
        when(mockBatch.commit()).thenReturn(mockBatchCommitTask);
        mockTaskSuccess(mockBatchCommitTask);
    }

    @After
    public void tearDown() {
        mockedAuthStatic.close();
        mockedDbStatic.close();
        mockedDeviceIdentity.close();
    }

    @Test
    public void testNoProfilesToManage() {
        // Mock empty snapshot
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        when(mockQuerySnapshot.getDocuments()).thenReturn(Collections.emptyList());

        FragmentScenario<ManageProfilesFragment> scenario = FragmentScenario.launchInContainer(
                ManageProfilesFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 1 child: the "No profiles found." TextView
            assertEquals(1, listContainer.getChildCount());
            View child = listContainer.getChildAt(0);
            assertTrue(child instanceof TextView);
            assertEquals("No profiles found.", ((TextView) child).getText().toString());
        });
    }

    @Test
    public void testOneProfile() {
        // Mock 1 profile
        List<DocumentSnapshot> docs = new ArrayList<>();
        docs.add(createMockProfileSnapshot("user_1", "John Doe", "john@example.com"));

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageProfilesFragment> scenario = FragmentScenario.launchInContainer(
                ManageProfilesFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 1 child: the profile card
            assertEquals(1, listContainer.getChildCount());

            View card = listContainer.getChildAt(0);
            TextView tvName = card.findViewById(R.id.name);
            TextView tvEmail = card.findViewById(R.id.email);

            assertEquals("John Doe", tvName.getText().toString());
            assertEquals("john@example.com", tvEmail.getText().toString());
        });
    }

    @Test
    public void testManyProfiles() {
        // Mock 15 profiles
        List<DocumentSnapshot> docs = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            docs.add(createMockProfileSnapshot("user_" + i, "User " + i, "user" + i + "@example.com"));
        }

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageProfilesFragment> scenario = FragmentScenario.launchInContainer(
                ManageProfilesFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 15 children
            assertEquals(15, listContainer.getChildCount());
        });
    }

    @Test
    public void testDeleteProfileRemovesItFromFirestore() {
        // Mock 1 profile
        List<DocumentSnapshot> docs = new ArrayList<>();
        docs.add(createMockProfileSnapshot("user_to_delete", "Delete Me", "delete@example.com"));

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageProfilesFragment> scenario = FragmentScenario.launchInContainer(
                ManageProfilesFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            View card = listContainer.getChildAt(0);
            MaterialButton btnDelete = card.findViewById(R.id.btnDeleteUser);

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

        // Verify batch delete was called for profile and role
        verify(mockProfilesCollection).document("user_to_delete");
        verify(mockRolesCollection).document("user_to_delete");

        // Verify batch operations
        verify(mockBatch).delete(mockProfileDocRef);
        verify(mockBatch).delete(mockRoleDocRef);
        verify(mockBatch).commit();
    }

    // --- Helpers ---

    private DocumentSnapshot createMockProfileSnapshot(String id, String name, String email) {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.getId()).thenReturn(id);
        when(doc.getString("fullName")).thenReturn(name);
        when(doc.getString("email")).thenReturn(email);
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
