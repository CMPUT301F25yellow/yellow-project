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
import com.google.firebase.firestore.DocumentReference;
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

        String userId = user.getUid();

        // Since we cannot create Collection Group Indexes, we must query all events
        // and check if the user is in any of the lists.
        // This is not ideal for scalability but works without custom indexes.

        db.collection("events").get().addOnSuccessListener(eventSnapshots -> {
            if (eventSnapshots.isEmpty()) {
                adapter.setEvents(new ArrayList<>());
                return;
            }

            List<Event> userEvents = new ArrayList<>();
            java.util.concurrent.atomic.AtomicInteger pendingChecks = new java.util.concurrent.atomic.AtomicInteger(
                    eventSnapshots.size());

            for (DocumentSnapshot eventDoc : eventSnapshots) {
                String eventId = eventDoc.getId();
                Event event = eventDoc.toObject(Event.class);
                if (event != null)
                    event.setId(eventId);

                // Check all 4 lists for this event
                checkUserInEvent(eventId, userId, isIn -> {
                    if (isIn && event != null) {
                        synchronized (userEvents) {
                            userEvents.add(event);
                        }
                    }

                    if (pendingChecks.decrementAndGet() == 0) {
                        if (userEvents.isEmpty()) {
                            Toast.makeText(getContext(), "No history found.", Toast.LENGTH_SHORT).show();
                        }
                        adapter.setEvents(userEvents);
                    }
                });
            }
        }).addOnFailureListener(e -> {
            Log.e("HistoryFragment", "Error loading events", e);
            Toast.makeText(getContext(), "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void checkUserInEvent(String eventId, String userId, OnCheckResult listener) {
        // We need to check if the user exists in ANY of the 4 subcollections.
        // We can do this by trying to get the specific document for the user in each
        // collection.
        // This is much faster than querying the whole collection.

        DocumentReference waitRef = db.collection("events").document(eventId).collection("waitingList")
                .document(userId);
        DocumentReference selRef = db.collection("events").document(eventId).collection("selected").document(userId);
        DocumentReference enrRef = db.collection("events").document(eventId).collection("enrolled").document(userId);
        DocumentReference canRef = db.collection("events").document(eventId).collection("cancelled").document(userId);

        // Check Waiting List
        waitRef.get().addOnCompleteListener(t1 -> {
            if (t1.isSuccessful() && t1.getResult().exists()) {
                listener.onResult(true);
                return;
            }
            // Check Selected
            selRef.get().addOnCompleteListener(t2 -> {
                if (t2.isSuccessful() && t2.getResult().exists()) {
                    listener.onResult(true);
                    return;
                }
                // Check Enrolled
                enrRef.get().addOnCompleteListener(t3 -> {
                    if (t3.isSuccessful() && t3.getResult().exists()) {
                        listener.onResult(true);
                        return;
                    }
                    // Check Cancelled
                    canRef.get().addOnCompleteListener(t4 -> {
                        if (t4.isSuccessful() && t4.getResult().exists()) {
                            listener.onResult(true);
                        } else {
                            listener.onResult(false);
                        }
                    });
                });
            });
        });
    }

    interface OnCheckResult {
        void onResult(boolean isIn);
    }

}