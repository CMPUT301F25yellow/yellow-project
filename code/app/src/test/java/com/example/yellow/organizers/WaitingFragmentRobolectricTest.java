package com.example.yellow.organizers;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.yellow.R;
import com.example.yellow.ui.ManageEntrants.WaitingFragment;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowLooper;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class WaitingFragmentRobolectricTest {

    private MockedStatic<FirebaseFirestore> firestoreMock;
    private FirebaseFirestore mockDb;
    private CollectionReference mockCollection;
    private DocumentReference mockDocument;

    private FragmentActivity activity;
    private WaitingFragment fragment;

    @Before
    public void setup() {

        // -------------------------
        // Firestore Mocking
        // -------------------------
        mockDb = Mockito.mock(FirebaseFirestore.class);
        mockCollection = Mockito.mock(CollectionReference.class);
        mockDocument = Mockito.mock(DocumentReference.class);

        firestoreMock = Mockito.mockStatic(FirebaseFirestore.class);
        firestoreMock.when(FirebaseFirestore::getInstance)
                .thenReturn(mockDb);

        Mockito.when(mockDb.collection(Mockito.anyString()))
                .thenReturn(mockCollection);

        Mockito.when(mockCollection.document(Mockito.anyString()))
                .thenReturn(mockDocument);

        Mockito.when(mockDocument.collection(Mockito.anyString()))
                .thenReturn(mockCollection);

        // Return a SAFE empty snapshot (non-null)
        QuerySnapshot emptySnapshot = Mockito.mock(QuerySnapshot.class);
        Mockito.when(emptySnapshot.isEmpty()).thenReturn(true);
        Mockito.when(emptySnapshot.getDocuments()).thenReturn(Collections.emptyList());
        Mockito.when(mockCollection.get()).thenReturn(Tasks.forResult(emptySnapshot));

        // -------------------------
        // Create Activity
        // -------------------------
        ActivityController<FragmentActivity> controller =
                Robolectric.buildActivity(FragmentActivity.class).setup();

        activity = controller.get();

        // -------------------------
        // Launch fragment
        // -------------------------
        fragment = new WaitingFragment();
        Bundle args = new Bundle();
        args.putString("eventId", "abc123");
        fragment.setArguments(args);

        FragmentTransaction txn = activity.getSupportFragmentManager().beginTransaction();
        txn.add(android.R.id.content, fragment);
        txn.commitNow();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    @After
    public void teardown() {
        firestoreMock.close();
    }

    @Test
    public void testFragmentLaunches() {
        View root = fragment.getView();
        assertNotNull(root);

        LinearLayout container = root.findViewById(R.id.waitingContainer);
        assertNotNull(container);
    }

    @Test
    public void testEmptyStateShown() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        LinearLayout container = fragment.getView().findViewById(R.id.waitingContainer);
        assertNotNull(container);

        TextView msg = (TextView) container.getChildAt(0);
        assertEquals("No waiting entrants.", msg.getText().toString());
    }

    @Test
    public void testFirestoreReturnsEntrants() {

        // Fake populated snapshot
        QuerySnapshot fakeSnapshot = Mockito.mock(QuerySnapshot.class);
        Mockito.when(fakeSnapshot.isEmpty()).thenReturn(false);
        Mockito.when(fakeSnapshot.getDocuments())
                .thenReturn(Collections.singletonList(
                        Mockito.mock(DocumentSnapshot.class)
                ));
        Mockito.when(mockCollection.get()).thenReturn(Tasks.forResult(fakeSnapshot));

        // Force reload via detach/attach
        FragmentTransaction txn = activity.getSupportFragmentManager()
                .beginTransaction();
        txn.detach(fragment);
        txn.attach(fragment);
        txn.commitNow();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        LinearLayout container = fragment.getView().findViewById(R.id.waitingContainer);

        assertTrue("Should contain entrant views", container.getChildCount() > 0);
    }

    @Test
    public void testFragmentWithoutEventIdDoesNotCrash() {
        WaitingFragment frag = new WaitingFragment();
        frag.setArguments(new Bundle()); // no eventId

        FragmentTransaction txn = activity.getSupportFragmentManager()
                .beginTransaction();
        txn.replace(android.R.id.content, frag);
        txn.commitNow();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        View root = frag.getView();
        assertNotNull(root);

        LinearLayout container = root.findViewById(R.id.waitingContainer);
        assertNotNull(container);
    }
}