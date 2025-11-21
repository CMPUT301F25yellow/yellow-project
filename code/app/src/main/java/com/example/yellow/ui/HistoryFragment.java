package com.example.yellow.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yellow.R;
import com.example.yellow.organizers.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays the user's event history (events they have joined the
 * waiting list for).
 */
public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Match the status bar color to the header
        requireActivity().getWindow().setStatusBarColor(
                ContextCompat.getColor(requireContext(), R.color.surface_dark));

        // Size the spacer to the exact status bar height for a perfect top band
        View spacer = v.findViewById(R.id.statusBarSpacer);
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.LayoutParams lp = spacer.getLayoutParams();
            if (lp.height != bars.top) {
                lp.height = bars.top;
                spacer.setLayoutParams(lp);
            }
            return insets;
        });

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Setup RecyclerView
        recyclerView = v.findViewById(R.id.rvHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(getContext());
        recyclerView.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collection Group Query: Find all documents in "waitingList" collections
        // where the document ID matches the current user's ID.
        // Note: This assumes the document ID in waitingList IS the userId (which is
        // true in WaitingListFragment).
        // Alternatively, if we stored userId as a field, we would use
        // .whereEqualTo("userId", user.getUid())

        // Based on WaitingListFragment:
        // .collection("waitingList").document(userId) -> so the doc ID is the userId.
        // BUT, Collection Group Queries on document IDs are tricky.
        // It is better to query by a field.
        // Checked WaitingListFragment: WaitingUser class HAS a 'userId' field.
        // So we can query by field "userId".

        db.collectionGroup("waitingList")
                .whereEqualTo("userId", user.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING) // Optional: order by join time
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No history
                        adapter.setEvents(new ArrayList<>());
                        return;
                    }

                    List<String> eventIds = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // The parent of the waitingList collection is the Event document
                        // path: events/{eventId}/waitingList/{userId}
                        // parent: events/{eventId}
                        // We can get the event ID from the reference parent's parent.

                        // doc.getReference().getParent() -> waitingList collection
                        // doc.getReference().getParent().getParent() -> event document

                        if (doc.getReference().getParent().getParent() != null) {
                            eventIds.add(doc.getReference().getParent().getParent().getId());
                        }
                    }

                    fetchEvents(eventIds);
                })
                .addOnFailureListener(e -> {
                    Log.e("HistoryFragment", "Error loading history", e);
                    Toast.makeText(getContext(), "Error loading history: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void fetchEvents(List<String> eventIds) {
        if (eventIds.isEmpty())
            return;

        // Firestore 'in' query supports up to 10 items.
        // If we have more, we might need to batch or fetch individually.
        // For simplicity, let's fetch individually or in batches of 10.
        // OR, simpler: just fetch them one by one and add to list.
        // Since we want to show them all, let's fetch all.

        List<Event> loadedEvents = new ArrayList<>();
        // Counter to know when done
        final int[] completed = { 0 };

        for (String id : eventIds) {
            db.collection("events").document(id).get()
                    .addOnSuccessListener(doc -> {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            // Manually set ID if it's missing in the object (though @DocumentId should
                            // handle it)
                            event.setId(doc.getId());
                            loadedEvents.add(event);
                        }
                        completed[0]++;
                        if (completed[0] == eventIds.size()) {
                            adapter.setEvents(loadedEvents);
                        }
                    })
                    .addOnFailureListener(e -> {
                        completed[0]++; // count even if failed
                        if (completed[0] == eventIds.size()) {
                            adapter.setEvents(loadedEvents);
                        }
                    });
        }
    }
}