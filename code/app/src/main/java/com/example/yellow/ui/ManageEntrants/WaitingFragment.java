package com.example.yellow.ui.ManageEntrants;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment for displaying a list of waiting entrants.
 * @author Waylon Wang - waylon1
 */
public class WaitingFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout container;
    private String eventId;
    private final List<String> currentWaitingEntrants = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private TextView waitingCount;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_waiting_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        container = view.findViewById(R.id.waitingContainer);
        waitingCount = view.findViewById(R.id.waitingCount);

        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        if (eventId == null) {
            if (isSafe()) {
                Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Button drawButton = view.findViewById(R.id.btnDraw);
        Button notifyButton = view.findViewById(R.id.btnNotifyAll);

        loadWaitingEntrants();

        drawButton.setOnClickListener(v -> showDrawDialog());
        notifyButton.setOnClickListener(v -> showNotificationDialog());
    }

    /** Loads all waiting entrants and displays their profile info */
    private void loadWaitingEntrants() {
        if (!isSafe()) return;

        container.removeAllViews();
        waitingCount.setText("0 people waiting");
        currentWaitingEntrants.clear();

        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!isSafe()) return;

                    if (snapshot.isEmpty()) {
                        currentWaitingEntrants.clear();
                        waitingCount.setText("0 people waiting");

                        TextView empty = new TextView(requireContext());
                        empty.setText("No waiting entrants.");
                        empty.setTextColor(getResources().getColor(R.color.hinty));
                        container.addView(empty);
                        return;
                    }

                    // 1️⃣ FIRST pass → collect userIds for count
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");
                        if (userId != null)
                            currentWaitingEntrants.add(userId);
                    }

                    // 2️⃣ update count IMMEDIATELY (before async profile loading)
                    waitingCount.setText(currentWaitingEntrants.size() + " people waiting");

                    // 3️⃣ SECOND pass → fetch profile details + populate cards
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");
                        if (userId == null) continue;

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profileDoc -> {
                                    if (!isSafe()) return;

                                    String name = profileDoc.getString("fullName");
                                    String email = profileDoc.getString("email");
                                    String joinDate = "Unknown date";

                                    if (doc.getTimestamp("timestamp") != null) {
                                        joinDate = dateFormat.format(doc.getTimestamp("timestamp").toDate());
                                    }

                                    if (name == null) name = "Unnamed User";
                                    if (email == null) email = "No email";

                                    addEntrantCard(name, email, joinDate, "Waiting");
                                })
                                .addOnFailureListener(e -> {
                                    if (!isSafe()) return;
                                    addEntrantCard("Unknown User", "Error loading email", "N/A", "Waiting");
                                });
                    }

                })
                .addOnFailureListener(e -> {
                    if (isSafe()) {
                        Toast.makeText(getContext(), "Failed to load entrants", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    /** Opens dialog to ask how many users to draw */
    private void showDrawDialog() {
        if (!isSafe()) return;

        if (currentWaitingEntrants.isEmpty()) {
            Toast.makeText(getContext(), "No entrants to draw from.", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter number to draw");
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});

        new AlertDialog.Builder(requireContext())
                .setTitle("Run Draw")
                .setMessage("Enter how many entrants to select (max: " + currentWaitingEntrants.size() + ")")
                .setView(input)
                .setPositiveButton("Draw", (dialog, which) -> {
                    if (!isSafe()) return;

                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter a number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int drawCount = Integer.parseInt(value);
                    if (drawCount <= 0) {
                        Toast.makeText(requireContext(), "Number must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (drawCount > currentWaitingEntrants.size()) {
                        Toast.makeText(requireContext(),
                                "Cannot draw more than " + currentWaitingEntrants.size(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    runDraw(drawCount);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /** With crash protection */
    private void showNotificationDialog() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (!isSafe()) return;

                    String eventName = eventDoc.getString("name");
                    if (eventName == null) eventName = "this event";

                    String message = "You are on the waiting list for: " + eventName;
                    sendNotificationToAllWaiting(message);
                })
                .addOnFailureListener(e -> {
                    if (isSafe()) {
                        Toast.makeText(getContext(), "Failed to load event info", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Randomly select users from the waiting list and move them to selected list */
    private void runDraw(int count) {
        if (!isSafe()) return;

        if (currentWaitingEntrants.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No entrants in waiting list.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Check event capacity BEFORE drawing
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {

                    Long maxParticipants = eventDoc.getLong("maxParticipants");
                    if (maxParticipants == null) maxParticipants = 0L;

                    // Count enrolled
                    Long finalMaxParticipants = maxParticipants;
                    db.collection("events").document(eventId)
                            .collection("enrolled")
                            .get()
                            .addOnSuccessListener(enrolledSnap -> {

                                int enrolledCount = enrolledSnap.size();

                                // Count selected (they should reserve spots)
                                db.collection("events").document(eventId)
                                        .collection("selected")
                                        .get()
                                        .addOnSuccessListener(selectedSnap -> {

                                            int selectedCount = selectedSnap.size();

                                            int currentCount = enrolledCount + selectedCount;

                                            // Unlimited capacity
                                            if (finalMaxParticipants == 0L) {
                                                actuallyRunDraw(count);
                                                return;
                                            }

                                            long remaining = finalMaxParticipants - currentCount;

                                            if (remaining <= 0) {
                                                Toast.makeText(getContext(),
                                                        "Event is full (" + finalMaxParticipants + " spots)",
                                                        Toast.LENGTH_LONG).show();
                                                return;
                                            }

                                            if (count > remaining) {
                                                Toast.makeText(getContext(),
                                                        "Only " + remaining +
                                                                " spots left. Reduce draw amount.",
                                                        Toast.LENGTH_LONG).show();
                                                return;
                                            }

                                            actuallyRunDraw(count);
                                        });
                            });
                });
    }

    /**
     * Actually runs the draw.
     * @param count
     */
    private void actuallyRunDraw(int count) {
        // 1. Shuffle a copy of current waiting entrants
        List<String> entrantsCopy = new ArrayList<>(currentWaitingEntrants);
        Collections.shuffle(entrantsCopy);

        // 2. Pick selected
        List<String> selected = entrantsCopy.subList(
                0,
                Math.min(count, entrantsCopy.size())
        );

        if (selected.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No entrants could be selected.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Everyone else becomes "non-selected" for this draw
        List<String> nonSelected = new ArrayList<>(entrantsCopy);
        nonSelected.removeAll(selected);

        // 4. Single batch to move all selected from waitingList -> selected
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        for (String userId : selected) {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("timestamp",
                    com.google.firebase.firestore.FieldValue.serverTimestamp());
            data.put("selected", true);

            var selectedRef = db.collection("events")
                    .document(eventId)
                    .collection("selected")
                    .document(userId);

            var waitingRef = db.collection("events")
                    .document(eventId)
                    .collection("waitingList")
                    .document(userId);

            batch.set(selectedRef, data);
            batch.delete(waitingRef);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (!isSafe()) return;

                    // Update local cache + UI
                    currentWaitingEntrants.removeAll(selected);
                    loadWaitingEntrants();

                    Toast.makeText(requireContext(),
                            "Selected " + selected.size() + " entrants.",
                            Toast.LENGTH_SHORT).show();

                    // 5. AUTOMATICALLY notify the non-selected entrants
                    if (!nonSelected.isEmpty()) {
                        notifyNonSelectedEntrants(nonSelected);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isSafe()) {
                        Toast.makeText(getContext(),
                                "Failed to move entrants: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Sends a notification to every waiting user. */
    private void sendNotificationToAllWaiting(String message) {
        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!isSafe()) return;

                    if (snapshot.isEmpty()) {
                        Toast.makeText(getContext(), "No waiting users to notify", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> userIds = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");
                        if (userId == null) continue;

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    if (!isSafe()) return;

                                    Boolean enabled = profile.getBoolean("notificationsEnabled");
                                    if (enabled == null) enabled = true;

                                    if (enabled) {
                                        userIds.add(userId);
                                    }
                                });
                    }

                    new android.os.Handler().postDelayed(() -> {
                        if (!isSafe()) return;

                        if (userIds.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "No users to notify (all have notifications off)",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        db.collection("events").document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {
                                    if (!isSafe()) return;

                                    String eventName = eventDoc.getString("name");
                                    if (eventName == null) eventName = "Event Update";

                                    com.example.yellow.utils.NotificationManager.sendNotification(
                                            getContext(),
                                            eventId,
                                            eventName,
                                            message,
                                            "waiting_list",
                                            userIds,
                                            new com.example.yellow.utils.NotificationManager.OnNotificationSentListener() {
                                                @Override
                                                public void onSuccess() {
                                                    if (isSafe()) {
                                                        Toast.makeText(getContext(),
                                                                "Notifications sent!",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    if (isSafe()) {
                                                        Toast.makeText(getContext(),
                                                                "Failed to send: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                });

                    }, 500);
                })
                .addOnFailureListener(e -> {
                    if (isSafe()) {
                        Toast.makeText(getContext(),
                                "Failed to fetch waiting users",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Automatically notifies all entrants who were NOT selected in the most recent draw.
     */
    private void notifyNonSelectedEntrants(List<String> nonSelectedUserIds) {
        if (!isSafe()) return;
        if (nonSelectedUserIds == null || nonSelectedUserIds.isEmpty()) return;

        // First get the event name for a nicer message
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (!isSafe()) return;

                    String eventName = eventDoc.getString("name");
                    if (eventName == null) eventName = "this event";

                    String message = "Unfortunately, you were not selected for "
                            + eventName + " this time.";

                    List<String> userIdsToNotify = new ArrayList<>();

                    // Respect per-user notification preferences
                    for (String userId : nonSelectedUserIds) {
                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    if (!isSafe()) return;

                                    Boolean enabled =
                                            profile.getBoolean("notificationsEnabled");
                                    if (enabled == null) enabled = true;

                                    if (enabled) {
                                        userIdsToNotify.add(userId);
                                    }
                                });
                    }

                    // Give the profile fetches a moment to complete
                    String finalEventName = eventName;
                    new android.os.Handler().postDelayed(() -> {
                        if (!isSafe()) return;

                        if (userIdsToNotify.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "No non-selected users to notify " +
                                            "(all notifications off?)",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        com.example.yellow.utils.NotificationManager.sendNotification(
                                getContext(),
                                eventId,
                                finalEventName,
                                message,
                                "loterry_non_selected",   // notification type/tag
                                userIdsToNotify,
                                new com.example.yellow.utils.NotificationManager
                                        .OnNotificationSentListener() {
                                    @Override
                                    public void onSuccess() {
                                        if (isSafe()) {
                                            Toast.makeText(getContext(),
                                                    "Notified non-selected entrants.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        if (isSafe()) {
                                            Toast.makeText(getContext(),
                                                    "Failed to notify non-selected: "
                                                            + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                        );
                    }, 500);
                })
                .addOnFailureListener(e -> {
                    if (isSafe()) {
                        Toast.makeText(getContext(),
                                "Failed to load event info for notifications",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Adds an entrant card to the layout */
    private void addEntrantCard(String name,
                                String email,
                                String joinDate,
                                String status) {

        if (!isSafe()) return;

        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_entrant_card, container, false);

        ((TextView) card.findViewById(R.id.tvEntrantName)).setText(name);
        ((TextView) card.findViewById(R.id.tvEntrantEmail)).setText(email);
        ((TextView) card.findViewById(R.id.tvJoinDate)).setText("Joined: " + joinDate);
        ((TextView) card.findViewById(R.id.tvStatus)).setText(status);

        int colorRes;
        switch (status.toLowerCase()) {
            case "selected": colorRes = R.color.brand_primary; break;
            case "enrolled": colorRes = R.color.green_400; break;
            case "cancelled": colorRes = R.color.danger_red; break;
            case "waiting":
            default: colorRes = R.color.gold; break;
        }

        card.findViewById(R.id.tvStatus)
                .getBackground()
                .setTint(requireContext().getColor(colorRes));

        if (isSafe()) {
            container.addView(card);
        }
    }

    /** Reusable safety check */
    private boolean isSafe() {
        return isAdded() && getContext() != null && container != null;
    }


}