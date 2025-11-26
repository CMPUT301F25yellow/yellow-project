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

public class SelectedFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout container;
    private String eventId;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final Set<String> selectedUserIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup parent,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_selected_list, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        container = view.findViewById(R.id.selectedContainer);
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        Button btnNotify = view.findViewById(R.id.btnSendNotification);
        Button btnCancel = view.findViewById(R.id.btnCancelSelected);

        if (eventId == null) {
            if (isSafe())
                Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        loadSelectedEntrants();

        btnNotify.setOnClickListener(v -> {
            if (isSafe()) showNotificationDialog();
        });

        btnCancel.setOnClickListener(v -> {
            if (isSafe()) cancelSelectedEntrants();
        });
    }

    private void loadSelectedEntrants() {
        if (!isSafe()) return;

        container.removeAllViews();
        selectedUserIds.clear();

        db.collection("events").document(eventId)
                .collection("selected")
                .addSnapshotListener((snapshot, e) -> {

                    if (!isSafe()) return;

                    if (!isSafe()) return;

                    container.removeAllViews();
                    selectedUserIds.clear();

                    if (snapshot == null || snapshot.isEmpty()) {
                        if (!isSafe()) return;

                        TextView empty = new TextView(requireContext());
                        empty.setText("No selected entrants yet");
                        empty.setTextColor(requireContext().getColor(R.color.hinty));
                        empty.setPadding(16, 24, 16, 24);

                        container.addView(empty);
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot) {

                        if (!isSafe()) return;

                        String userId = doc.getString("userId");
                        if (userId == null || userId.isEmpty()) continue;

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    if (!isSafe()) return;

                                    String name = profile.getString("fullName");
                                    String email = profile.getString("email");
                                    if (name == null) name = "Unnamed Entrant";
                                    if (email == null) email = "No email";

                                    String date = extractTimestamp(doc);

                                    addEntrantCard(userId, name, email, dateSelected, "Selected");
                                })
                                .addOnFailureListener(err -> {
                                    if (!isSafe()) return;

                                    addEntrantCard(
                                            userId,
                                            "Unknown",
                                            "Error loading profile",
                                            "N/A",
                                            "Selected"
                                    );
                                });
                    }   // ← CLOSES the for-loop
                });      // ← CLOSES addSnapshotListener
    }

    private void showNotificationDialog() {
        if (!isSafe()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Send Notification");

        final EditText input = new EditText(requireContext());
        input.setHint("Enter custom message (optional)");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);

        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            if (!isSafe()) return;

            String msg = input.getText().toString().trim();
            if (msg.isEmpty())
                msg = "You’ve been selected! Please sign up for the event.";

            sendNotificationToAllSelected(msg);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void sendNotificationToAllSelected(String message) {
        db.collection("events").document(eventId)
                .collection("selected")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!isSafe()) return;

                    if (snapshot.isEmpty()) {
                        Toast.makeText(getContext(),
                                "No selected entrants to notify",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    java.util.List<String> userIds = new java.util.ArrayList<>();

                    for (DocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");
                        if (userId == null) continue;

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {

                                    if (!isSafe()) return;

                                    Boolean enabled = profile.getBoolean("notificationsEnabled");
                                    if (enabled == null) enabled = true;

                                    if (enabled) userIds.add(userId);
                                });
                    }

                    new android.os.Handler().postDelayed(() -> {

                        if (!isSafe()) return;

                        if (userIds.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "No users to notify (all notifications off)",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        db.collection("events").document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {

                                    if (!isSafe()) return;

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
                                                    if (isSafe())
                                                        Toast.makeText(getContext(),
                                                                "Notification sent!",
                                                                Toast.LENGTH_SHORT).show();
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    if (isSafe())
                                                        Toast.makeText(getContext(),
                                                                "Failed to send: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                });

                    }, 500);
                })
                .addOnFailureListener(e -> {
                    if (isSafe())
                        Toast.makeText(getContext(),
                                "Failed to fetch users",
                                Toast.LENGTH_SHORT).show();
                });
    }

    private String extractTimestamp(DocumentSnapshot doc) {
        Object ts = doc.get("timestamp");

        if (ts instanceof Timestamp)
            return dateFormat.format(((Timestamp) ts).toDate());
        else if (ts instanceof Long)
            return dateFormat.format(new Date((Long) ts));

        return "Unknown";
    }


    private void addEntrantCard(String userId,
                                String name,
                                String email,
                                String joinDate,
                                String status) {

        if (!isSafe()) return;

        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_entrant_card, container, false);

        TextView tvName = card.findViewById(R.id.tvEntrantName);
        TextView tvEmail = card.findViewById(R.id.tvEntrantEmail);
        TextView tvJoinDate = card.findViewById(R.id.tvJoinDate);
        TextView tvStatus = card.findViewById(R.id.tvStatus);
        CheckBox cb = card.findViewById(R.id.checkboxSelected);

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

        if (status.equalsIgnoreCase("selected")) {
            cb.setVisibility(View.VISIBLE);
            cb.setChecked(selectedUserIds.contains(userId));

            cb.setOnCheckedChangeListener((b, isChecked) -> {
                if (!isSafe()) return;

                if (isChecked)
                    selectedUserIds.add(userId);
                else
                    selectedUserIds.remove(userId);
            });

            card.setOnClickListener(v -> {
                if (isSafe()) cb.setChecked(!cb.isChecked());
            });
        } else {
            cb.setVisibility(View.GONE);
        }

        container.addView(card);
    }

    private void cancelSelectedEntrants() {
        if (!isSafe()) return;

        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedUserIds.isEmpty()) {
            Toast.makeText(getContext(), "No entrants selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel selected entrants")
                .setMessage("Move " + selectedUserIds.size() + " entrants to Cancelled?")
                .setPositiveButton("Yes", (d, w) -> performCancellationBatch())
                .setNegativeButton("No", null)
                .show();
    }

    private void performCancellationBatch() {
        DocumentReference eventRef = db.collection("events").document(eventId);
        WriteBatch batch = db.batch();

        for (String userId : selectedUserIds) {
            DocumentReference selectedRef =
                    eventRef.collection("selected").document(userId);
            DocumentReference cancelledRef =
                    eventRef.collection("cancelled").document(userId);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("timestamp", FieldValue.serverTimestamp());

            batch.set(cancelledRef, data);
            batch.delete(selectedRef);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (isSafe())
                        Toast.makeText(getContext(),
                                "Cancelled selected entrants.",
                                Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (isSafe())
                        Toast.makeText(getContext(),
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isSafe() {
        return isAdded() && getContext() != null && container != null;
    }
}