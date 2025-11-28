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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WaitingFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout container;
    private String eventId;
    private final List<String> currentWaitingEntrants = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_waiting_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        container = view.findViewById(R.id.waitingContainer);
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        if (eventId == null) {
            if (isSafe()) {
                Toast.makeText(getContext(),
                        "Missing event ID", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Button btnRunDraw = view.findViewById(R.id.btnDraw);
        Button btnNotify = view.findViewById(R.id.btnNotifyAll);

        btnRunDraw.setOnClickListener(v -> showDrawDialog());
        btnNotify.setOnClickListener(v -> showNotificationDialog());

        loadWaitingEntrants();
    }

    private boolean isSafe() {
        return getContext() != null && isAdded();
    }

    private void loadWaitingEntrants() {
        if (!isSafe()) return;

        container.removeAllViews();
        currentWaitingEntrants.clear();

        db.collection("events").document(eventId)
                .collection("waitingList")
                .addSnapshotListener((snapshot, e) -> {

                    if (!isSafe()) return;

                    container.removeAllViews();
                    currentWaitingEntrants.clear();

                    if (e != null || snapshot == null || snapshot.isEmpty()) {
                        TextView empty = new TextView(requireContext());
                        empty.setText("No entrants waiting yet");
                        empty.setTextColor(requireContext().getColor(R.color.light_grey_text));
                        container.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");
                        if (userId == null) continue;

                        currentWaitingEntrants.add(userId);

                        String joinDate = extractTimestamp(doc);
                        String name = doc.getString("name");
                        if (name == null) name = "Unknown";

                        String email = doc.getString("email");
                        if (email == null) email = "No email";

                        addEntrantCard(userId, name, email, joinDate, "Waiting");
                    }
                });
    }

    private String extractTimestamp(DocumentSnapshot doc) {
        com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
        if (ts == null) {
            return "Unknown date";
        }
        return dateFormat.format(ts.toDate());
    }

    private void addEntrantCard(
            String userId,
            String name,
            String email,
            String joinDate,
            String status
    ) {
        if (!isSafe()) return;

        View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_entrant_card, container, false);

        TextView tvName = card.findViewById(R.id.tvEntrantName);
        TextView tvEmail = card.findViewById(R.id.tvEntrantEmail);
        TextView tvJoinDate = card.findViewById(R.id.tvJoinDate);
        TextView tvStatus = card.findViewById(R.id.tvStatus);

        tvName.setText(name);
        tvEmail.setText(email);
        tvJoinDate.setText("Joined: " + joinDate);
        tvStatus.setText(status);

        int colorRes;
        switch (status.toLowerCase()) {
            case "waiting":
                colorRes = R.color.hinty;
                break;
            case "selected":
                colorRes = R.color.gold;
                break;
            case "enrolled":
                colorRes = R.color.green_400;
                break;
            case "cancelled":
                colorRes = R.color.danger_red;
                break;
            default:
                colorRes = R.color.hinty;
                break;
        }

        tvStatus.getBackground().setTint(requireContext().getColor(colorRes));
        container.addView(card);
    }

    /** Dialog to ask how many users to draw */
    private void showDrawDialog() {
        if (!isSafe()) return;

        if (currentWaitingEntrants.isEmpty()) {
            Toast.makeText(getContext(),
                    "No entrants to draw from.",
                    Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(requireContext(),
                                "Please enter a number",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int drawCount;
                    try {
                        drawCount = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(),
                                "Invalid number",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (drawCount <= 0) {
                        Toast.makeText(requireContext(),
                                "Number must be greater than 0",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (drawCount > currentWaitingEntrants.size()) {
                        Toast.makeText(requireContext(),
                                "Cannot draw more than " + currentWaitingEntrants.size(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Apply participant capacity (maxParticipants) before running the draw
                    runDrawWithCapacity(drawCount);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Checks the event's maxParticipants and the current number of selected + enrolled
     * entrants, then runs the draw with an adjusted count that does not exceed the
     * remaining participant capacity.
     */
    private void runDrawWithCapacity(int requestedCount) {
        if (!isSafe()) return;

        if (eventId == null) {
            Toast.makeText(getContext(),
                    "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch event to read maxParticipants
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (!isSafe()) return;

                    Long maxParticipantsLong = eventDoc.getLong("maxParticipants");
                    int maxParticipants = (maxParticipantsLong != null)
                            ? maxParticipantsLong.intValue()
                            : 0;

                    // If no participant cap is set (0 = unlimited), just run the draw as requested
                    if (maxParticipants <= 0) {
                        runDraw(requestedCount);
                        return;
                    }

                    // Count currently selected entrants
                    FirebaseFirestore.getInstance()
                            .collection("events")
                            .document(eventId)
                            .collection("selected")
                            .get()
                            .addOnSuccessListener(selectedSnap -> {
                                if (!isSafe()) return;

                                final int selectedCount = selectedSnap.size();

                                // Count currently enrolled entrants
                                FirebaseFirestore.getInstance()
                                        .collection("events")
                                        .document(eventId)
                                        .collection("enrolled")
                                        .get()
                                        .addOnSuccessListener(enrolledSnap -> {
                                            if (!isSafe()) return;

                                            int enrolledCount = enrolledSnap.size();
                                            int currentParticipants = selectedCount + enrolledCount;
                                            int remainingSlots = maxParticipants - currentParticipants;

                                            if (remainingSlots <= 0) {
                                                Toast.makeText(getContext(),
                                                        "Participant limit reached. No more entrants can be selected.",
                                                        Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            int actualCount = Math.min(requestedCount, remainingSlots);
                                            if (actualCount < requestedCount) {
                                                Toast.makeText(getContext(),
                                                        "Only " + actualCount + " entrants can be selected due to participant limit.",
                                                        Toast.LENGTH_SHORT).show();
                                            }

                                            runDraw(actualCount);
                                        })
                                        .addOnFailureListener(e -> {
                                            if (isSafe()) {
                                                Toast.makeText(getContext(),
                                                        "Failed to check enrolled entrants.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (isSafe()) {
                                    Toast.makeText(getContext(),
                                            "Failed to check selected entrants.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (isSafe()) {
                        Toast.makeText(getContext(),
                                "Failed to load event capacity.",
                                Toast.LENGTH_SHORT).show();
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
            data.put("timestamp", FieldValue.serverTimestamp());
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
                                "Failed to move entrants to selected list.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** With crash protection */
    private void showNotificationDialog() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (!isSafe()) return;

                    String eventName = eventDoc.getString("name");
                    if (eventName == null) eventName = "Event Update";

                    final EditText input = new EditText(requireContext());
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setHint("Enter your message");

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Notify Waiting Entrants")
                            .setMessage("Message to send to all waiting entrants for " + eventName)
                            .setView(input)
                            .setPositiveButton("Send", (dialog, which) -> {
                                String msg = input.getText().toString().trim();
                                if (msg.isEmpty()) {
                                    Toast.makeText(getContext(),
                                            "Message cannot be empty",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                sendNotificationToAllWaiting(msg);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .show();
                })
                .addOnFailureListener(e -> {
                    if (isSafe()) {
                        Toast.makeText(getContext(),
                                "Unable to load event info", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendNotificationToAllWaiting(String message) {
        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!isSafe()) return;

                    if (snapshot.isEmpty()) {
                        Toast.makeText(getContext(),
                                "No waiting entrants to notify",
                                Toast.LENGTH_SHORT).show();
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
                                                                "Notifications sent.",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    if (isSafe()) {
                                                        Toast.makeText(getContext(),
                                                                "Failed to send notifications: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }
                                    );
                                })
                                .addOnFailureListener(e -> {
                                    if (isSafe()) {
                                        Toast.makeText(getContext(),
                                                "Failed to load event name", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }, 1500);
                })
                .addOnFailureListener(e -> {
                    if (isSafe()) {
                        Toast.makeText(getContext(),
                                "Failed to load waiting entrants", Toast.LENGTH_SHORT).show();
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
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (!isSafe()) return;

                    String eventName = eventDoc.getString("name");
                    if (eventName == null) eventName = "this event";

                    String message = "Unfortunately, you were not selected in the latest draw for " + eventName + ".";

                    List<String> enabledUserIds = new ArrayList<>();

                    for (String userId : nonSelectedUserIds) {
                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    if (!isSafe()) return;

                                    Boolean enabled = profile.getBoolean("notificationsEnabled");
                                    if (enabled == null) enabled = true;

                                    if (enabled) {
                                        enabledUserIds.add(userId);
                                    }
                                });
                    }

                    String finalEventName = eventName;
                    new android.os.Handler().postDelayed(() -> {
                        if (!isSafe()) return;

                        if (enabledUserIds.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "No non-selected users to notify (all have notifications off)",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        com.example.yellow.utils.NotificationManager.sendNotification(
                                getContext(),
                                eventId,
                                finalEventName,
                                message,
                                "waiting_list_non_selected",
                                enabledUserIds,
                                new com.example.yellow.utils.NotificationManager.OnNotificationSentListener() {
                                    @Override
                                    public void onSuccess() {
                                        if (isSafe()) {
                                            Toast.makeText(getContext(),
                                                    "Non-selected entrants notified.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        if (isSafe()) {
                                            Toast.makeText(getContext(),
                                                    "Failed to notify non-selected entrants: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                        );
                    }, 1500);
                })
                .addOnFailureListener(e -> {
                    if (isSafe()) {
                        Toast.makeText(getContext(),
                                "Failed to load event for notifications",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
