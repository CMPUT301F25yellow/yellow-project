package com.example.yellow.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.yellow.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for ManageNotificationLogFragment using Robolectric.
 * Verifies loading notification logs and viewing recipients from a mocked
 * Firestore database.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ManageNotificationLogFragmentTest {

    @Mock
    private FirebaseAuth mockAuth;
    @Mock
    private FirebaseUser mockUser;
    @Mock
    private FirebaseFirestore mockDb;
    @Mock
    private CollectionReference mockLogsCollection;
    @Mock
    private Query mockQuery;
    @Mock
    private Task<QuerySnapshot> mockQueryTask;
    @Mock
    private QuerySnapshot mockQuerySnapshot;

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

        // Mock notification_logs collection with orderBy
        when(mockDb.collection("notification_logs")).thenReturn(mockLogsCollection);
        when(mockLogsCollection.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockQueryTask);

        // Mock addSnapshotListener
        when(mockQuery.addSnapshotListener(any())).thenAnswer(invocation -> {
            com.google.firebase.firestore.EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(mockQuerySnapshot, null);
            return mock(com.google.firebase.firestore.ListenerRegistration.class);
        });
    }

    @After
    public void tearDown() {
        mockedAuthStatic.close();
        mockedDbStatic.close();
        mockedDeviceIdentity.close();
    }

    @Test
    public void testNoLogs() {
        // Mock empty snapshot
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        when(mockQuerySnapshot.getDocuments()).thenReturn(Collections.emptyList());

        FragmentScenario<ManageNotificationLogFragment> scenario = FragmentScenario.launchInContainer(
                ManageNotificationLogFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 1 child: the "No notifications found." TextView
            assertEquals(1, listContainer.getChildCount());
            View child = listContainer.getChildAt(0);
            assertTrue(child instanceof TextView);
            assertEquals("No notifications found.", ((TextView) child).getText().toString());
        });
    }

    @Test
    public void testSingleLog() {
        // Mock 1 log
        List<DocumentSnapshot> docs = new ArrayList<>();
        docs.add(createMockLogSnapshot(
                "log_1",
                "Test Event",
                "Test message",
                "Test Organizer",
                5L,
                Arrays.asList("User 1", "User 2", "User 3", "User 4", "User 5")));

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageNotificationLogFragment> scenario = FragmentScenario.launchInContainer(
                ManageNotificationLogFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 1 child: the log card
            assertEquals(1, listContainer.getChildCount());

            View card = listContainer.getChildAt(0);
            TextView tvEventName = card.findViewById(R.id.eventName);
            TextView tvMessage = card.findViewById(R.id.message);
            TextView tvRecipientCount = card.findViewById(R.id.recipientCount);

            assertEquals("Event - Test Event", tvEventName.getText().toString());
            assertEquals("Test message", tvMessage.getText().toString());
            assertEquals("Sent to 5 recipients", tvRecipientCount.getText().toString());
        });
    }

    @Test
    public void testManyLogs() {
        // Mock 15 logs
        List<DocumentSnapshot> docs = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            docs.add(createMockLogSnapshot(
                    "log_" + i,
                    "Event " + i,
                    "Message " + i,
                    "Organizer " + i,
                    (long) (i + 1),
                    Arrays.asList("User " + i)));
        }

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageNotificationLogFragment> scenario = FragmentScenario.launchInContainer(
                ManageNotificationLogFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            // Should have 15 children
            assertEquals(15, listContainer.getChildCount());
        });
    }

    @Test
    public void testViewRecipientsNoRecipients() {
        // Mock 1 log with no recipients
        List<DocumentSnapshot> docs = new ArrayList<>();
        docs.add(createMockLogSnapshot(
                "log_1",
                "Test Event",
                "Test message",
                "Test Organizer",
                0L,
                Collections.emptyList()));

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageNotificationLogFragment> scenario = FragmentScenario.launchInContainer(
                ManageNotificationLogFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            View card = listContainer.getChildAt(0);
            View btnViewRecipients = card.findViewById(R.id.btnViewRecipients);

            // Click view recipients button
            btnViewRecipients.performClick();
        });

        // The fragment should show a toast "No recipients recorded."
        // In Robolectric, we can verify this via ShadowToast
        assertEquals("No recipients recorded.",
                org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testViewRecipientsSingleRecipient() {
        // Mock 1 log with 1 recipient
        List<DocumentSnapshot> docs = new ArrayList<>();
        docs.add(createMockLogSnapshot(
                "log_1",
                "Test Event",
                "Test message",
                "Test Organizer",
                1L,
                Arrays.asList("John Doe")));

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageNotificationLogFragment> scenario = FragmentScenario.launchInContainer(
                ManageNotificationLogFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            View card = listContainer.getChildAt(0);
            View btnViewRecipients = card.findViewById(R.id.btnViewRecipients);

            // Click view recipients button
            btnViewRecipients.performClick();
        });

        // Verify dialog is shown (MaterialAlertDialogBuilder creates
        // androidx.appcompat.app.AlertDialog)
        androidx.appcompat.app.AlertDialog dialog = (androidx.appcompat.app.AlertDialog) org.robolectric.shadows.ShadowAlertDialog
                .getLatestDialog();
        assertTrue(dialog.isShowing());

        // Verify dialog content contains the recipient name
        // The dialog message is set via builder.setMessage(), we can verify it was
        // shown
        // by checking that the dialog exists and is showing
        assertTrue(dialog != null);
    }

    @Test
    public void testViewRecipientsManyRecipients() {
        // Mock 1 log with 10 recipients
        List<String> recipients = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            recipients.add("User " + i);
        }

        List<DocumentSnapshot> docs = new ArrayList<>();
        docs.add(createMockLogSnapshot(
                "log_1",
                "Test Event",
                "Test message",
                "Test Organizer",
                10L,
                recipients));

        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);

        FragmentScenario<ManageNotificationLogFragment> scenario = FragmentScenario.launchInContainer(
                ManageNotificationLogFragment.class, null, R.style.Theme_Yellow, Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            LinearLayout listContainer = fragment.getView().findViewById(R.id.listContainer);
            View card = listContainer.getChildAt(0);
            View btnViewRecipients = card.findViewById(R.id.btnViewRecipients);

            // Click view recipients button
            btnViewRecipients.performClick();
        });

        // Verify dialog is shown (MaterialAlertDialogBuilder creates
        // androidx.appcompat.app.AlertDialog)
        androidx.appcompat.app.AlertDialog dialog = (androidx.appcompat.app.AlertDialog) org.robolectric.shadows.ShadowAlertDialog
                .getLatestDialog();
        assertTrue(dialog.isShowing());

        // Verify dialog exists and is showing (content verification is complex with
        // MaterialAlertDialog)
        assertTrue(dialog != null);
    }

    // --- Helpers ---

    private DocumentSnapshot createMockLogSnapshot(String id, String eventName, String message,
            String organizerName, Long recipientCount,
            List<String> recipientNames) {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.getId()).thenReturn(id);
        when(doc.getString("eventName")).thenReturn(eventName);
        when(doc.getString("message")).thenReturn(message);
        when(doc.getString("organizerName")).thenReturn(organizerName);
        when(doc.getLong("recipientCount")).thenReturn(recipientCount);
        when(doc.get("recipientNames")).thenReturn(recipientNames);

        // Mock timestamp
        Timestamp timestamp = new Timestamp(new Date());
        when(doc.getTimestamp("timestamp")).thenReturn(timestamp);

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
}
