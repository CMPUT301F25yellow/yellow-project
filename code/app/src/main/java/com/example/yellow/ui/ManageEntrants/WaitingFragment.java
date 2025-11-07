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

        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        loadWaitingEntrants();

        drawButton.setOnClickListener(v -> showDrawDialog());
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

                                    if (name == null) name = "Unnamed User";
                                    if (email == null) email = "No email";

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
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});

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
                        Toast.makeText(getContext(), "Cannot draw more than " + currentWaitingEntrants.size(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    runDraw(drawCount);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Randomly select users from the waiting list and move them to selected list
     */
    private void runDraw(int count) {
        if (eventId == null) return;

        List<String> entrantsCopy = new ArrayList<>(currentWaitingEntrants);
        Collections.shuffle(entrantsCopy);
        List<String> selected = entrantsCopy.subList(0, count);

        for (String id : selected) {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", id);
            data.put("selected", true);
            data.put("timestamp", System.currentTimeMillis());

            db.collection("events").document(eventId)
                    .collection("selected")
                    .document(id)
                    .set(data);
        }

        Toast.makeText(getContext(),
                "Selected " + selected.size() + " entrants!",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Adds an entrant card to the layout
     */
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
            case "waiting":
            default:
                colorRes = R.color.gold;
                break;
        }
        tvStatus.getBackground().setTint(getResources().getColor(colorRes));

        container.addView(card);
    }
}