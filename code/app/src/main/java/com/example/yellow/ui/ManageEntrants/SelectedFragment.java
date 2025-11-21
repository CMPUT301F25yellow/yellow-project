package com.example.yellow.ui.ManageEntrants;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * shows all entrants who were selected for an event
 * lets the organizer send a notification to all selected users
 */
public class SelectedFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout container;
    private String eventId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

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

        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        loadSelectedEntrants();
        btnNotify.setOnClickListener(v -> showNotificationDialog());
    }

    /** loads all selected entrants in real time from Firestore */
    private void loadSelectedEntrants() {
        container.removeAllViews();

        db.collection("events").document(eventId)
                .collection("selected")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Error loading entrants", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    container.removeAllViews();

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
                                    if (name == null)
                                        name = "Unnamed Entrant";
                                    if (email == null)
                                        email = "No email";

                                    String dateSelected = "Unknown date";
                                    Object ts = doc.get("timestamp");
                                    if (ts instanceof Timestamp) {
                                        dateSelected = dateFormat.format(((Timestamp) ts).toDate());
                                    } else if (ts instanceof Long) {
                                        dateSelected = dateFormat.format(new Date((Long) ts));
                                    }

                                    addEntrantCard(name, email, dateSelected, "Selected");
                                })
                                .addOnFailureListener(
                                        err -> addEntrantCard("Unknown", "Error loading profile", "N/A", "Selected"));
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
                    for (DocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");
                        if (userId != null)
                            userIds.add(userId);
                    }

                    // Fetch event name
                    db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
                        String eventName = eventDoc.getString("name");
                        if (eventName == null)
                            eventName = "Unknown Event";

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
                })
                .addOnFailureListener(
                        e -> Toast.makeText(getContext(), "Failed to fetch users", Toast.LENGTH_SHORT).show());
    }

    /** builds and adds one entrant card to the screen */
    private void addEntrantCard(String name, String email, String joinDate, String status) {
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
            case "selected":
                colorRes = R.color.brand_primary;
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

        tvStatus.getBackground().setTint(getResources().getColor(colorRes));
        container.addView(card);
    }
}