package com.example.yellow.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.View;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.yellow.R;
import com.google.android.gms.tasks.Task;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ProfileUserFragmentTest {

    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseUser mockUser;
    @Mock private FirebaseFirestore mockDb;

    @Mock private CollectionReference mockProfilesCollection;
    @Mock private DocumentReference mockProfileDoc;

    @Mock private Task<DocumentSnapshot> mockGetTask;
    @Mock private Task<Void> mockSetTask;
    @Mock private Task<Void> mockDeleteTask;
    @Mock private Task<Void> mockUpdateTask;
    @Mock private Task<AuthResult> mockAuthTask;

    @Mock private CollectionReference mockEventsCollection;
    @Mock private CollectionReference mockLogsCollection;
    @Mock private com.google.firebase.firestore.Query mockQuery;
    @Mock private com.google.firebase.firestore.QuerySnapshot mockQuerySnapshot;
    @Mock private Task<com.google.firebase.firestore.QuerySnapshot> mockQueryTask;
    @Mock private com.google.firebase.firestore.WriteBatch mockBatch;
    @Mock private Task<Void> mockBatchCommitTask;

    private MockedStatic<FirebaseAuth> mockedAuthStatic;
    private MockedStatic<FirebaseFirestore> mockedDbStatic;
    private MockedStatic<com.example.yellow.utils.DeviceIdentityManager> mockedDeviceIdentity;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockedAuthStatic = mockStatic(FirebaseAuth.class);
        mockedDbStatic = mockStatic(FirebaseFirestore.class);
        mockedDeviceIdentity = mockStatic(com.example.yellow.utils.DeviceIdentityManager.class);

        // Static singletons
        mockedAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
        mockedDbStatic.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        // Default: not admin
        mockedDeviceIdentity
                .when(com.example.yellow.utils.DeviceIdentityManager::isCurrentDeviceAdmin)
                .thenReturn(false);

        @SuppressWarnings("unchecked")
        Task<Boolean> mockAdminTask = mock(Task.class);
        mockBooleanTaskSuccess(mockAdminTask, false);

        mockedDeviceIdentity
                .when(() -> com.example.yellow.utils.DeviceIdentityManager.ensureDeviceDocument(any(), any()))
                .thenReturn(mockAdminTask);

        // Auth mocks
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn("test_user_id");

        // Firestore mocks
        when(mockDb.collection("profiles")).thenReturn(mockProfilesCollection);
        when(mockProfilesCollection.document(anyString())).thenReturn(mockProfileDoc);

        when(mockProfileDoc.get()).thenReturn(mockGetTask);
        when(mockProfileDoc.set(any(), any())).thenReturn(mockSetTask);
        when(mockProfileDoc.delete()).thenReturn(mockDeleteTask);
        when(mockProfileDoc.update(anyString(), any())).thenReturn(mockUpdateTask);

        // Successful write/update/delete operations
        mockTaskSuccess(mockSetTask);
        mockTaskSuccess(mockDeleteTask);
        mockTaskSuccess(mockUpdateTask);

        // Mock events and logs for ProfileSyncUtils
        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        when(mockDb.collection("notification_logs")).thenReturn(mockLogsCollection);
        when(mockDb.batch()).thenReturn(mockBatch);
        when(mockBatch.commit()).thenReturn(mockBatchCommitTask);
        mockTaskSuccess(mockBatchCommitTask);

        when(mockEventsCollection.whereEqualTo(anyString(), anyString())).thenReturn(mockQuery);
        when(mockLogsCollection.whereEqualTo(anyString(), anyString())).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockQueryTask);

        // Mock query success with empty snapshot
        when(mockQueryTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            com.google.android.gms.tasks.OnSuccessListener listener = invocation.getArgument(0);
            when(mockQuerySnapshot.isEmpty()).thenReturn(true);
            listener.onSuccess(mockQuerySnapshot);
            return mockQueryTask;
        });
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);
    }

    @After
    public void tearDown() {
        mockedAuthStatic.close();
        mockedDbStatic.close();
        mockedDeviceIdentity.close();
    }

    // Helper to always launch fragment with app theme so Material components inflate
    private FragmentScenario<ProfileUserFragment> launchProfileWithTheme() {
        return FragmentScenario.launchInContainer(
                ProfileUserFragment.class,
                null,
                R.style.Theme_Yellow,
                Lifecycle.State.RESUMED
        );
    }

    // ------------------------- LOAD TESTS -------------------------

    @Test
    public void testLoadProfile_noProfile() {
        mockProfileDocument(null);

        FragmentScenario<ProfileUserFragment> scenario = launchProfileWithTheme();

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            TextInputEditText name = root.findViewById(R.id.inputFullName);
            TextInputEditText email = root.findViewById(R.id.inputEmail);
            TextInputEditText phone = root.findViewById(R.id.inputPhone);
            MaterialSwitch notifications = root.findViewById(R.id.switchNotifications);

            assertEquals("", name.getText().toString());
            assertEquals("", email.getText().toString());
            assertEquals("", phone.getText().toString());
            // New / no profile => notifications default ON
            assertTrue(notifications.isChecked());
        });
    }

    @Test
    public void testLoadProfile_onlyRequiredFields() {
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("fullName", "John Doe");
        profileData.put("email", "john.doe@example.com");
        profileData.put("notificationsEnabled", true);

        mockProfileDocument(profileData);

        FragmentScenario<ProfileUserFragment> scenario = launchProfileWithTheme();

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            TextInputEditText name = root.findViewById(R.id.inputFullName);
            TextInputEditText email = root.findViewById(R.id.inputEmail);
            TextInputEditText phone = root.findViewById(R.id.inputPhone);

            assertEquals("John Doe", name.getText().toString());
            assertEquals("john.doe@example.com", email.getText().toString());
            // phone missing => empty
            assertEquals("", phone.getText().toString());
        });
    }

    @Test
    public void testLoadProfile_completeProfile() {
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", "Jane Smith");
        data.put("email", "jane@example.com");
        data.put("phone", "555");
        data.put("notificationsEnabled", false);

        mockProfileDocument(data);

        FragmentScenario<ProfileUserFragment> scenario = launchProfileWithTheme();

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            TextInputEditText phone = root.findViewById(R.id.inputPhone);
            MaterialSwitch notifications = root.findViewById(R.id.switchNotifications);

            assertEquals("555", phone.getText().toString());
            assertFalse(notifications.isChecked());
        });
    }

    @Test
    public void testLoadProfile_nullNotifications_defaultsToOn() {
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", "T");
        data.put("email", "t@t.com");

        mockProfileDocument(data);

        FragmentScenario<ProfileUserFragment> scenario = launchProfileWithTheme();

        scenario.onFragment(fragment -> {
            MaterialSwitch notifications = fragment.getView().findViewById(R.id.switchNotifications);
            // Missing notificationsEnabled => defaults to ON in fragment
            assertTrue(notifications.isChecked());
        });
    }

    // ------------------------- TEST: SAVE PROFILE -------------------------

    @Test
    public void testSaveProfile_callsFirestoreSet() {
        mockProfileDocument(null);

        FragmentScenario<ProfileUserFragment> scenario = launchProfileWithTheme();

        scenario.onFragment(fragment -> {
            View root = fragment.getView();

            TextInputEditText name = root.findViewById(R.id.inputFullName);
            TextInputEditText email = root.findViewById(R.id.inputEmail);
            TextInputEditText phone = root.findViewById(R.id.inputPhone);

            name.setText("Alice");
            email.setText("alice@test.com");
            phone.setText("123");

            root.findViewById(R.id.btnSave).performClick();

            verify(mockProfileDoc).set(anyMap(), any(SetOptions.class));
        });
    }

    // ------------------------- TEST: UPDATE PROFILE -------------------------

    @Test
    public void testSaveProfile_overwritesExistingProfile() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("fullName", "Old Name");
        mockProfileDocument(existing);

        FragmentScenario<ProfileUserFragment> scenario = launchProfileWithTheme();

        scenario.onFragment(fragment -> {
            View root = fragment.getView();

            TextInputEditText name = root.findViewById(R.id.inputFullName);
            name.setText("New Name");

            root.findViewById(R.id.btnSave).performClick();

            verify(mockProfileDoc).set(anyMap(), any(SetOptions.class));
        });
    }

    // ------------------------- TEST: DELETE PROFILE -------------------------

    @Test
    public void testDeleteProfile_callsFirestoreDelete() {
        mockProfileDocument(null);

        FragmentScenario<ProfileUserFragment> scenario = launchProfileWithTheme();

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            root.findViewById(R.id.btnDeleteProfile).performClick();

            verify(mockProfileDoc).delete();
        });
    }

    // ------------------------- TEST: TOGGLE NOTIFICATIONS -------------------------

    @Test
    public void testToggleNotifications_updatesFirestore() {
        mockProfileDocument(null); // no profile; switch defaults to ON (true)

        FragmentScenario<ProfileUserFragment> scenario = launchProfileWithTheme();

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            MaterialSwitch notifications = root.findViewById(R.id.switchNotifications);

            // Flip from true -> false
            notifications.setChecked(false);

            verify(mockProfileDoc).update("notificationsEnabled", false);
        });
    }

    // ------------------------- TEST: ADMIN BUTTON VISIBLE WHEN ADMIN -------------------------

    @Test
    public void testAdminButtonVisibleWhenDeviceIsAdmin() {
        // Override default for this test: now device is admin
        mockedDeviceIdentity
                .when(com.example.yellow.utils.DeviceIdentityManager::isCurrentDeviceAdmin)
                .thenReturn(true);

        mockProfileDocument(null);

        FragmentScenario<ProfileUserFragment> scenario = launchProfileWithTheme();

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            View btnAdmin = root.findViewById(R.id.btnAdminDashboard);
            assertEquals(View.VISIBLE, btnAdmin.getVisibility());
        });
    }

    // ------------------------- TEST: ADMIN BUTTON VISIBILITY WHEN NOT ADMIN -------------------------

    @Test
    public void testAdminButtonHiddenWhenNotAdmin() {
        // Explicitly ensure not admin
        mockedDeviceIdentity
                .when(com.example.yellow.utils.DeviceIdentityManager::isCurrentDeviceAdmin)
                .thenReturn(false);

        mockProfileDocument(null);

        FragmentScenario<ProfileUserFragment> scenario = launchProfileWithTheme();

        scenario.onFragment(fragment -> {
            View btnAdmin = fragment.getView().findViewById(R.id.btnAdminDashboard);
            assertEquals(View.GONE, btnAdmin.getVisibility());
        });
    }

    // ------------------------- HELPERS -------------------------

    private void mockProfileDocument(Map<String, Object> data) {
        when(mockGetTask.addOnSuccessListener(any())).thenAnswer(inv -> {
            DocumentSnapshot snapshot = mock(DocumentSnapshot.class);

            if (data == null) {
                when(snapshot.exists()).thenReturn(false);
            } else {
                when(snapshot.exists()).thenReturn(true);

                if (!data.containsKey("notificationsEnabled")) {
                    when(snapshot.getBoolean("notificationsEnabled")).thenReturn(null);
                }

                for (String key : data.keySet()) {
                    Object v = data.get(key);
                    if (v instanceof String) {
                        when(snapshot.getString(key)).thenReturn((String) v);
                    } else if (v instanceof Boolean) {
                        when(snapshot.getBoolean(key)).thenReturn((Boolean) v);
                    } else if (v instanceof Timestamp) {
                        when(snapshot.getTimestamp(key)).thenReturn((Timestamp) v);
                    }
                }
            }

            inv.<com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>>getArgument(0)
                    .onSuccess(snapshot);
            return mockGetTask;
        });

        when(mockGetTask.addOnFailureListener(any())).thenReturn(mockGetTask);
    }

    private void mockTaskSuccess(Task<Void> task) {
        when(task.addOnSuccessListener(any())).thenAnswer(inv -> {
            inv.<com.google.android.gms.tasks.OnSuccessListener<Void>>getArgument(0).onSuccess(null);
            return task;
        });
        when(task.addOnFailureListener(any())).thenReturn(task);
    }

    private void mockBooleanTaskSuccess(Task<Boolean> task, boolean value) {
        when(task.addOnSuccessListener(any())).thenAnswer(inv -> {
            inv.<com.google.android.gms.tasks.OnSuccessListener<Boolean>>getArgument(0)
                    .onSuccess(value);
            return task;
        });
        when(task.addOnFailureListener(any())).thenReturn(task);
    }
}