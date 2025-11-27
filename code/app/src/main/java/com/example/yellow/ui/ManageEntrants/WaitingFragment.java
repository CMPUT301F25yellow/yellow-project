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
import com.google.firebase.firestore.FieldValue;

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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

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
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        Button drawButton = view.findViewById(R.id.btnDraw);
        Button notifyButton = view.findViewById(R.id.btnNotifyAll);

        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        loadWaitingEntrants();

        drawButton.setOnClickListener(v -> showDrawDialog());
        notifyButton.setOnClickListener(v -> showNotificationDialog());

    }

    /**
     * Loads all waiting entrants and displays their profile info
     */
    private void loadWaitingEntrants() {
        container.removeAllViews();
        currentWaitingEntrants.clear();

        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No waiting entrants.");
                        empty.setTextColor(getResources().getColor(R.color.hinty));
                        container.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");
                        currentWaitingEntrants.add(userId);

                        // Fetch user profile from /profiles/{userId}
                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profileDoc -> {
                                    String name = profileDoc.getString("fullName");
                                    String email = profileDoc.getString("email");
                                    String joinDate = "Unknown date";
                                    if (doc.getTimestamp("timestamp") != null) {
                                        joinDate = dateFormat.format(doc.getTimestamp("timestamp").toDate());
                                    }

                                    if (name == null)
                                        name = "Unnamed User";
                                    if (email == null)
                                        email = "No email";

                                    addEntrantCard(name, email, joinDate, "Waiting");
                                })
                                .addOnFailureListener(e -> {
                                    addEntrantCard("Unknown User", "Error loading email", "N/A", "Waiting");
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Opens dialog to ask how many users to draw
     */
    private void showDrawDialog() {
        if (currentWaitingEntrants.isEmpty()) {
            Toast.makeText(getContext(), "No entrants to draw from.", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter number to draw");
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(3) });

        new AlertDialog.Builder(getContext())
                .setTitle("Run Draw")
                .setMessage("Enter how many entrants to select (max: " + currentWaitingEntrants.size() + ")")
                .setView(input)
                .setPositiveButton("Draw", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter a number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int drawCount = Integer.parseInt(value);
                    if (drawCount <= 0) {
                        Toast.makeText(getContext(), "Number must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (drawCount > currentWaitingEntrants.size()) {
                        Toast.makeText(getContext(), "Cannot draw more than " + currentWaitingEntrants.size(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    runDraw(drawCount);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showNotificationDialog() {
        // Fetch event name first, then send notification
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    String eventName = eventDoc.getString("name");
                    if (eventName == null)
                        eventName = "this event";

                    String message = "You are on the waiting list for: " + eventName;
                    sendNotificationToAllWaiting(message);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load event info", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Randomly select users from the waiting list and move them to selected list
     */
    private void runDraw(int count) {
        if (eventId == null)
            return;

        if (currentWaitingEntrants.isEmpty()) {
            Toast.makeText(getContext(), "No entrants in waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        // shuffle and pick winners
        List<String> entrantsCopy = new ArrayList<>(currentWaitingEntrants);
        Collections.shuffle(entrantsCopy);
        List<String> selected = entrantsCopy.subList(0, Math.min(count, entrantsCopy.size()));

        // Determine which entrants were *not* selected in this draw
        Set<String> notSelected = new HashSet<>(currentWaitingEntrants);
        notSelected.removeAll(selected);

        for (String userId : selected) {

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
            data.put("selected", true);

            com.google.firebase.firestore.WriteBatch batch = db.batch();

            // destination
            var selectedRef = db.collection("events")
                    .document(eventId)
                    .collection("selected")
                    .document(userId);

            // source (to remove)
            var waitingRef = db.collection("events")
                    .document(eventId)
                    .collection("waitingList")
                    .document(userId);

            // add to selected
            batch.set(selectedRef, data);

            // remove from waiting
            batch.delete(waitingRef);

            // commit atomic move
            batch.commit()
                    .addOnSuccessListener(unused -> {
                        currentWaitingEntrants.remove(userId);
                        loadWaitingEntrants(); // refresh UI
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(),
                            "Failed to move entrant: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());
        }

        Toast.makeText(getContext(),
                "Selected " + selected.size() + " entrants.",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Sends a notification to every waiting user.
     */
    private void sendNotificationToAllWaiting(String message) {
        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Toast.makeText(getContext(), "No waiting users to notify", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    java.util.List<String> userIds = new java.util.ArrayList<>();

                    // Fetch profile settings for each user
                    for (DocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");
                        if (userId == null)
                            continue;

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    Boolean enabled = profile.getBoolean("notificationsEnabled");
                                    if (enabled == null)
                                        enabled = true;

                                    if (enabled) {
                                        userIds.add(userId);
                                    }
                                });
                    }

                    // Delay slightly to allow profile fetches to complete
                    new android.os.Handler().postDelayed(() -> {

                        if (userIds.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "No users to notify (all have notifications off)", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Fetch event name for the notification title
                        db.collection("events").document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {
                                    String eventName = eventDoc.getString("name");
                                    if (eventName == null)
                                        eventName = "Event Update";

                                    com.example.yellow.utils.NotificationManager.sendNotification(
                                            getContext(),
                                            eventId,
                                            eventName,
                                            message,
                                            "waiting_list", // Use custom card layout
                                            userIds,
                                            new com.example.yellow.utils.NotificationManager.OnNotificationSentListener() {
                                                @Override
                                                public void onSuccess() {
                                                    Toast.makeText(getContext(),
                                                            "Notifications sent!",
                                                            Toast.LENGTH_SHORT).show();
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    Toast.makeText(getContext(),
                                                            "Failed to send: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                });

                    }, 500); // 0.5 sec delay for profile reads
                })
                .addOnFailureListener(
                        e -> Toast.makeText(getContext(), "Failed to fetch waiting users", Toast.LENGTH_SHORT).show());
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
            notif.put("message", "You were not selected in the lottery for this event.");
            notif.put("eventId", eventId);
            notif.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
            notif.put("type", "lottery_not_selected");

            // Store under the user's profile-specific notifications subcollection
            db.collection("profiles")
                    .document(userId)
                    .collection("notifications")
                    .add(notif)
                    .addOnFailureListener(e -> {
                        // Fail silently (add later)
                    });
        }
    }

    /**
     * Adds an entrant card to the layout
     */
    private boolean isSafe() {
        return isAdded() && getContext() != null && container != null;
    }

    private void addEntrantCard(String name,
            String email,
            String joinDate,
            String status) {

        if (!isSafe())
            return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View card = inflater.inflate(R.layout.item_entrant_card, container, false);

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

        tvStatus.getBackground().setTint(requireContext().getColor(colorRes));

        if (isSafe()) {
            container.addView(card);
        }
    }
}