package com.example.yellow.ui.ViewEvent;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class EntrantsFragment extends Fragment {

    private FirebaseFirestore db;
    private String eventId;
    private LinearLayout entrantsContainer;

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

        entrantsContainer = view.findViewById(R.id.entrantsContainer);
        db = FirebaseFirestore.getInstance();

        // Get eventId from arguments
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }

        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        loadEntrants();
    }

    private void loadEntrants() {
        entrantsContainer.removeAllViews();

        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        TextView emptyText = new TextView(getContext());
                        emptyText.setText("No entrants yet.");
                        emptyText.setTextColor(Color.GRAY);
                        emptyText.setPadding(8, 16, 8, 16);
                        entrantsContainer.addView(emptyText);
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        addEntrantView(userId);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading entrants: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addEntrantView(String userId) {
        // Create a simple TextView for each entrant
        TextView nameView = new TextView(getContext());
        nameView.setText("User: " + userId);
        nameView.setTextSize(16);
        nameView.setTextColor(getResources().getColor(R.color.white));
        nameView.setPadding(16, 16, 16, 16);

        // Add a divider
        View divider = new View(getContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.DKGRAY);

        entrantsContainer.addView(nameView);
        entrantsContainer.addView(divider);
    }
}
