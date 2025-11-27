package com.example.yellow.ui.ManageEntrants;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * shows all entrants who were selected for an event
 * lets the organizer send a notification to all selected users
 */
public class SelectedFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout container;
    private String eventId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final Set<String> selectedUserIds = new HashSet<>();

    /** inflates the layout for the selected entrants screen */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_selected_list, container, false);
    }

    /** sets up Firestore, loads entrants, and binds the send notification button */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        container = view.findViewById(R.id.selectedContainer);
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        Button btnNotify = view.findViewById(R.id.btnSendNotification);
        Button btnCancel = view.findViewById(R.id.btnCancelSelected);

        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        loadSelectedEntrants();
        btnNotify.setOnClickListener(v -> showNotificationDialog());
        btnCancel.setOnClickListener(v -> cancelSelectedEntrants());
    }

    /** loads all selected entrants in real time from Firestore */
    private void loadSelectedEntrants() {
        container.removeAllViews();
        selectedUserIds.clear(); // reset selection whenever we reload

        db.collection("events").document(eventId)
                .collection("selected")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Error loading entrants", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    container.removeAllViews();
                    selectedUserIds.clear();

                    if (snapshot == null || snapshot.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No selected entrants yet");
                        empty.setTextColor(getResources().getColor(R.color.hinty));
                        empty.setPadding(16, 24, 16, 24);
                        container.addView(empty);
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        if (userId == null || userId.isEmpty())
                            continue;

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    String name = profile.getString("fullName");
                                    String email = profile.getString("email");
                                    if (name == null) name = "Unnamed Entrant";
                                    if (email == null) email = "No email";

                                    String dateSelected = "Unknown date";
                                    Object ts = doc.get("timestamp");
                                    if (ts instanceof Timestamp) {
                                        dateSelected = dateFormat.format(((Timestamp) ts).toDate());
                                    } else if (ts instanceof Long) {
                                        dateSelected = dateFormat.format(new Date((Long) ts));
                                    }

                                    // ⬇️ pass userId into the card now
                                    addEntrantCard(userId, name, email, dateSelected, "Selected");
                                })
                                .addOnFailureListener(err ->
                                        addEntrantCard(userId,
                                                "Unknown",
                                                "Error loading profile",
                                                "N/A",
                                                "Selected"));
                    }
                });
    }


    /** shows a dialog to write a custom message or use a default one */
    private void showNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Send Notification");

        final EditText input = new EditText(getContext());
        input.setHint("Enter custom message (optional)");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String message = input.getText().toString().trim();
            if (message.isEmpty()) {
                message = "You’ve been selected! Please sign up for the event.";
            }
            sendNotificationToAllSelected(message);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /** sends a notification to all users in the selected list */
    private void sendNotificationToAllSelected(String message) {
        db.collection("events").document(eventId)
                .collection("selected")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Toast.makeText(getContext(), "No selected entrants to notify", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    java.util.List<String> userIds = new java.util.ArrayList<>();

                    // Fetch each profile and check notification settings
                    for (DocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");
                        if (userId == null) continue;

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    Boolean enabled = profile.getBoolean("notificationsEnabled");
                                    if (enabled == null) enabled = true; // default ON

                                    if (enabled) {
                                        userIds.add(userId);
                                    }
                                });
                    }

                    // Add a small delay to allow all Firestore fetches to finish
                    // before sending notifications
                    new android.os.Handler().postDelayed(() -> {

                        if (userIds.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "No users to notify (all have notifications off)", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Fetch event name
                        db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
                            String eventName = eventDoc.getString("name");
                            if (eventName == null) eventName = "Unknown Event";

                            com.example.yellow.utils.NotificationManager.sendNotification(
                                    getContext(),
                                    eventId,
                                    eventName,
                                    message,
                                    userIds,
                                    new com.example.yellow.utils.NotificationManager.OnNotificationSentListener() {
                                        @Override
                                        public void onSuccess() {
                                            Toast.makeText(getContext(), "Notification sent!", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Toast.makeText(getContext(), "Failed to send: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        });

                    }, 500); // 0.5 second delay to allow all profile lookups to complete
                })
                .addOnFailureListener(
                        e -> Toast.makeText(getContext(), "Failed to fetch users", Toast.LENGTH_SHORT).show());
    }

    /** builds and adds one entrant card to the screen */
    private boolean isSafe() {
        return isAdded() && getContext() != null && container != null;
    }

    private void addEntrantCard(String userId,
                                String name,
                                String email,
                                String joinDate,
                                String status) {

        if (!isSafe()) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View card = inflater.inflate(R.layout.item_entrant_card, container, false);

        TextView tvName = card.findViewById(R.id.tvEntrantName);
        TextView tvEmail = card.findViewById(R.id.tvEntrantEmail);
        TextView tvJoinDate = card.findViewById(R.id.tvJoinDate);
        TextView tvStatus = card.findViewById(R.id.tvStatus);
        CheckBox cbSelected = card.findViewById(R.id.checkboxSelected);

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
                colorRes = R.color.gold;
                break;
        }

        tvStatus.getBackground().setTint(requireContext().getColor(colorRes));

        // Checkbox feature only in selected state
        if ("selected".equalsIgnoreCase(status) && cbSelected != null) {
            cbSelected.setVisibility(View.VISIBLE);
            cbSelected.setChecked(selectedUserIds.contains(userId));

            cbSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isSafe()) return;

                if (isChecked) {
                    selectedUserIds.add(userId);
                } else {
                    selectedUserIds.remove(userId);
                }
            });

            // Tap anywhere toggles checkbox
            card.setOnClickListener(v -> {
                if (isSafe()) cbSelected.setChecked(!cbSelected.isChecked());
            });
        } else if (cbSelected != null) {
            cbSelected.setVisibility(View.GONE);
        }

        if (isSafe()) {
            container.addView(card);
        }
    }

    /** Called when organizer taps "Cancel Selected Entrants" */
    private void cancelSelectedEntrants() {
        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedUserIds.isEmpty()) {
            Toast.makeText(getContext(), "No entrants selected to cancel.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Cancel selected entrants")
                .setMessage("Cancel " + selectedUserIds.size() +
                        " entrant(s) and move them to the Cancelled list?")
                .setPositiveButton("Yes", (dialog, which) -> performCancellationBatch())
                .setNegativeButton("No", null)
                .show();
    }

    /** Moves all checked entrants from events/{eventId}/selected -> events/{eventId}/cancelled */
    private void performCancellationBatch() {
        DocumentReference eventRef = db.collection("events").document(eventId);
        WriteBatch batch = db.batch();

        for (String userId : selectedUserIds) {
            DocumentReference selectedRef = eventRef.collection("selected").document(userId);
            DocumentReference cancelledRef = eventRef.collection("cancelled").document(userId);

            // CancelledFragment expects userId + timestamp
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("timestamp", FieldValue.serverTimestamp());

            batch.set(cancelledRef, data);
            batch.delete(selectedRef);

            Map<String, Object> notif = new HashMap<>();
            notif.put("message", "Your selection for this event was cancelled by the organizer.");
            notif.put("eventId", eventId);
            notif.put("timestamp", FieldValue.serverTimestamp());
            notif.put("type", "entrant_cancelled");   // <-- IMPORTANT

            db.collection("profiles")
                    .document(userId)
                    .collection("notifications")
                    .add(notif);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Cancelled selected entrants.", Toast.LENGTH_SHORT).show();
                    selectedUserIds.clear();
                    // Snapshot listener on "selected" + "cancelled" will refresh both tabs automatically
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to cancel entrants: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

}