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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        currentWaitingEntrants.clear();

        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!isSafe()) return;

                    if (snapshot.isEmpty()) {
                        TextView empty = new TextView(requireContext());
                        empty.setText("No waiting entrants.");
                        empty.setTextColor(getResources().getColor(R.color.hinty));
                        container.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");
                        currentWaitingEntrants.add(userId);

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profileDoc -> {
                                    if (!isSafe()) return;

                                    String name = profileDoc.getString("fullName");
                                    String email = profileDoc.getString("email");
                                    String joinDate = "Unknown date";

                                    if (doc.getTimestamp("timestamp") != null) {
                                        joinDate = dateFormat.format(
                                                doc.getTimestamp("timestamp").toDate()
                                        );
                                    }

                                    if (name == null) name = "Unnamed User";
                                    if (email == null) email = "No email";

                                    addEntrantCard(name, email, joinDate, "Waiting");
                                })
                                .addOnFailureListener(e -> {
                                    if (!isSafe()) return;
                                    addEntrantCard("Unknown User",
                                            "Error loading email",
                                            "N/A",
                                            "Waiting");
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (isSafe()) {
                        Toast.makeText(getContext(),
                                "Failed to load entrants",
                                Toast.LENGTH_SHORT).show();
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
                .setMessage("Enter how many entrants to select (max: "
                        + currentWaitingEntrants.size() + ")")
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

                    int drawCount = Integer.parseInt(value);
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

                    String message =
                            "You are on the waiting list for: " + eventName;
                    sendNotificationToAllWaiting(message);
                })
                .addOnFailureListener(e -> {
                    if (isSafe()) {
                        Toast.makeText(getContext(),
                                "Failed to load event info",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Randomly select users from the waiting list and move them to selected list,
     * and notify the non-selected entrants via in-app notifications.
     */
    private void runDraw(int count) {
        if (!isSafe()) return;

        if (currentWaitingEntrants.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No entrants in waiting list.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Shuffle and pick winners
        List<String> entrantsCopy = new ArrayList<>(currentWaitingEntrants);
        Collections.shuffle(entrantsCopy);
        List<String> selected =
                entrantsCopy.subList(0, Math.min(count, entrantsCopy.size()));

        // Determine which entrants were NOT selected in this draw
        Set<String> notSelected = new HashSet<>(currentWaitingEntrants);
        notSelected.removeAll(selected);

        for (String userId : selected) {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("timestamp",
                    com.google.firebase.firestore.FieldValue.serverTimestamp());
            data.put("selected", true);

            com.google.firebase.firestore.WriteBatch batch = db.batch();

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

            batch.commit()
                    .addOnSuccessListener(unused -> {
                        if (!isSafe()) return;

                        currentWaitingEntrants.remove(userId);
                        loadWaitingEntrants();
                    })
                    .addOnFailureListener(e -> {
                        if (isSafe()) {
                            Toast.makeText(getContext(),
                                    "Failed to move entrant: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        // Notify the losing entrants (still on waiting list) in-app
        notifyNotSelectedEntrants(notSelected);

        if (isSafe()) {
            Toast.makeText(requireContext(),
                    "Selected " + selected.size() + " entrants.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** Sends a notification to every waiting user. */
    private void sendNotificationToAllWaiting(String message) {
        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!isSafe()) return;

                    if (snapshot.isEmpty()) {
                        Toast.makeText(getContext(),
                                "No waiting users to notify",
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

                                    Boolean enabled =
                                            profile.getBoolean("notificationsEnabled");
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
                                            new com.example.yellow.utils.NotificationManager
                                                    .OnNotificationSentListener() {
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
                                                                "Failed to send: "
                                                                        + e.getMessage(),
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
     * Creates in-app notifications for all entrants who were NOT selected
     * in the lottery draw. These will appear in the entrant's Notifications tab.
     *
     * @param loserIds userIds of entrants who were not selected
     */
    private void notifyNotSelectedEntrants(Set<String> loserIds) {
        if (loserIds == null || loserIds.isEmpty()) {
            return;
        }

        for (String userId : loserIds) {
            if (userId == null || userId.isEmpty()) continue;

            Map<String, Object> notif = new HashMap<>();
            notif.put("message",
                    "You were not selected in the lottery for this event.");
            notif.put("eventId", eventId);
            notif.put("timestamp",
                    com.google.firebase.firestore.FieldValue.serverTimestamp());
            notif.put("type", "lottery_not_selected");

            db.collection("profiles")
                    .document(userId)
                    .collection("notifications")
                    .add(notif)
                    .addOnFailureListener(e -> {
                        // Fail silently (could log if you want)
                    });
        }
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
        ((TextView) card.findViewById(R.id.tvJoinDate))
                .setText("Joined: " + joinDate);
        ((TextView) card.findViewById(R.id.tvStatus)).setText(status);

        int colorRes;
        switch (status.toLowerCase()) {
            case "selected":
                colorRes = R.color.brand_primary;
                break;
            case "enrolled":
                colorRes = R.color.green_400;
                break;
            case "cancelled":
                colorRes = R.color.danger_red;
                break;
            case "waiting":
            default:
                colorRes = R.color.gold;
                break;
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
