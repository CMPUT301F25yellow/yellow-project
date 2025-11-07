package com.example.yellow.ui.ManageEntrants;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SelectedFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout container;
    private String eventId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_selected_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        container = view.findViewById(R.id.selectedContainer);
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        loadSelectedEntrants();
    }

    /**
     * Load all selected entrants from Firestore and display them
     */
    private void loadSelectedEntrants() {
        container.removeAllViews();

        db.collection("events").document(eventId)
                .collection("selected")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No selected entrants yet.");
                        empty.setTextColor(getResources().getColor(R.color.hinty));
                        empty.setPadding(16, 24, 16, 24);
                        container.addView(empty);
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        if (userId == null || userId.isEmpty()) {
                            continue;
                        }

                        // Fetch profile info for each selected user
                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    String name = profile.getString("fullName");
                                    String email = profile.getString("email");
                                    if (name == null) name = "Unnamed Entrant";
                                    if (email == null) email = "No email";

                                    // Handle timestamp (works for both Firestore Timestamp and Long)
                                    Object ts = doc.get("timestamp");
                                    String dateSelected = "Unknown date";
                                    if (ts instanceof Timestamp) {
                                        dateSelected = dateFormat.format(((Timestamp) ts).toDate());
                                    } else if (ts instanceof Long) {
                                        dateSelected = dateFormat.format(new Date((Long) ts));
                                    }

                                    addEntrantCard(name, email, dateSelected, "Selected");
                                })
                                .addOnFailureListener(e -> {
                                    addEntrantCard("Unknown", "Error loading profile", "N/A", "Selected");
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load selected entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Inflate and populate a single entrant card
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
        tvJoinDate.setText("Selected: " + joinDate);
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