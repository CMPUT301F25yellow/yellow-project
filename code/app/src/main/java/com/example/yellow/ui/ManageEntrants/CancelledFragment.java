package com.example.yellow.ui.ManageEntrants;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

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

public class CancelledFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout container;
    private String eventId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Reuse SAME layout as Selected tab
        return inflater.inflate(R.layout.fragment_cancelled_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        container = view.findViewById(R.id.cancelledContainer);
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }
        loadCancelledEntrants();

        Button btnNotify = view.findViewById(R.id.btnNotifyCancelled);
        btnNotify.setOnClickListener(v -> showNotificationDialog());
    }

    private void loadCancelledEntrants() {
        container.removeAllViews();

        db.collection("events").document(eventId)
                .collection("cancelled")
                .addSnapshotListener((snapshot, e) -> {
                    container.removeAllViews();

                    if (e != null || snapshot == null || snapshot.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No cancelled entrants");
                        empty.setTextColor(getResources().getColor(R.color.hinty));
                        empty.setPadding(16, 24, 16, 24);
                        container.addView(empty);
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    String name = profile.getString("fullName");
                                    String email = profile.getString("email");
                                    String date = extractTimestamp(doc);

                                    addEntrantCard(name, email, date, "Cancelled");
                                });
                    }
                });

    }

    private void showNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Notify Cancelled Entrants");

        final android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Enter message");
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String message = input.getText().toString().trim();
            if (message.isEmpty()) {
                message = "Event update from organizer.";
            }
            sendNotificationToCancelled(message);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void sendNotificationToCancelled(String message) {
        db.collection("events").document(eventId)
                .collection("cancelled")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        Toast.makeText(getContext(),
                                "No cancelled entrants to notify", Toast.LENGTH_SHORT).show();
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

                    // Delay for async Firestore profile lookups
                    new android.os.Handler().postDelayed(() -> {

                        if (userIds.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "No users to notify (all have notifications off)",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Fetch event name
                        db.collection("events").document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {
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
                                                    Toast.makeText(getContext(),
                                                            "Notification sent!",
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

                    }, 500); // half-second delay to allow profile fetches to finish
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to fetch cancelled entrants",
                                Toast.LENGTH_SHORT).show());
    }

    private String extractTimestamp(DocumentSnapshot doc) {
        Object ts = doc.get("timestamp");

        if (ts instanceof Timestamp) {
            return dateFormat.format(((Timestamp) ts).toDate());
        } else if (ts instanceof Long) {
            return dateFormat.format(new Date((Long) ts));
        }

        return "Unknown";
    }

    private void addEntrantCard(String name, String email, String date, String status) {
        View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_entrant_card, container, false);

        ((TextView) card.findViewById(R.id.tvEntrantName)).setText(name);
        ((TextView) card.findViewById(R.id.tvEntrantEmail)).setText(email);
        ((TextView) card.findViewById(R.id.tvJoinDate)).setText("Updated: " + date);
        ((TextView) card.findViewById(R.id.tvStatus)).setText(status);

        // STATUS COLOR = red
        card.findViewById(R.id.tvStatus)
                .getBackground()
                .setTint(getResources().getColor(R.color.danger_red));

        container.addView(card);
    }
}
