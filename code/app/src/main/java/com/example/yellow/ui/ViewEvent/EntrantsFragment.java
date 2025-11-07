package com.example.yellow.ui.ViewEvent;

import android.content.Intent;
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EntrantsFragment extends Fragment {

    private FirebaseFirestore db;
    private String eventId;
    private LinearLayout entrantsContainer;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrants, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton manageButton = view.findViewById(R.id.manageEntrantsButton);
        entrantsContainer = view.findViewById(R.id.entrantsContainer);
        db = FirebaseFirestore.getInstance();

        // Retrieve eventId from arguments
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }

        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        loadEntrants();

        manageButton.setOnClickListener(v -> {
            if (getActivity() == null) return;

            Intent intent = new Intent(getActivity(), com.example.yellow.ui.ManageEntrants.ManageEntrantsActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
    }

    private void loadEntrants() {
        entrantsContainer.removeAllViews();

        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No entrants yet.");
                        empty.setTextColor(getResources().getColor(R.color.hinty));
                        entrantsContainer.addView(empty);
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        Date joinedAt = doc.getDate("timestamp");

                        if (userId != null) {
                            // Fetch profile info for each user
                            db.collection("profiles").document(userId)
                                    .get()
                                    .addOnSuccessListener(profileDoc -> {
                                        String name = profileDoc.getString("fullName");
                                        String email = profileDoc.getString("email");
                                        String joinDate = (joinedAt != null)
                                                ? dateFormat.format(joinedAt)
                                                : "Unknown date";

                                        if (name == null) name = "Unnamed User";
                                        if (email == null) email = "No email";

                                        addEntrantCard(name, email, joinDate, "Waiting");
                                    })
                                    .addOnFailureListener(e -> {
                                        addEntrantCard("Unknown User", "Error loading email", "N/A", "Waiting");
                                    });
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading entrants: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void addEntrantCard(String name, String email, String joinDate, String status) {
        View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_entrant_card, entrantsContainer, false);

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
            case "selected": colorRes = R.color.brand_primary; break;
            case "enrolled": colorRes = R.color.green_400; break;
            case "cancelled": colorRes = R.color.danger_red; break;
            case "waiting":
            default:
                colorRes = R.color.gold;
                break;
        }

        tvStatus.getBackground().setTint(getResources().getColor(colorRes));
        entrantsContainer.addView(card);
    }
}