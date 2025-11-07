package com.example.yellow.ui.ViewEvent;

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

/**
 * shows all entrants for an event
 * combines waiting, selected, enrolled, and cancelled lists
 * useful for organizers who want a quick overview of all participants
 */
public class EntrantsFragment extends Fragment {

    private FirebaseFirestore db;
    private String eventId;
    private LinearLayout entrantsContainer;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /** inflates the layout for the entrants list screen */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrants, container, false);
    }

    /** sets up Firestore and loads entrants for the given event */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        entrantsContainer = view.findViewById(R.id.entrantsContainer);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }

        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        loadAllEntrants();
    }

    /** loads entrants from all four subcollections under the event */
    private void loadAllEntrants() {
        entrantsContainer.removeAllViews();

        loadEntrantsFromSubcollection("waitingList", "Waiting");
        loadEntrantsFromSubcollection("selected", "Selected");
        loadEntrantsFromSubcollection("cancelled", "Cancelled");
        loadEntrantsFromSubcollection("enrolled", "Enrolled");
    }

    /**
     * loads entrants from a specific subcollection
     * pulls each user's profile and shows them on screen
     *
     * @param subcollection name of the Firestore subcollection (waitingList, selected, etc)
     * @param status label to display on each entrant card
     */
    private void loadEntrantsFromSubcollection(String subcollection, String status) {
        db.collection("events").document(eventId)
                .collection(subcollection)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        if (userId == null || userId.isEmpty()) continue;

                        // fetch from /profiles/{userId}
                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    String name = profile.getString("fullName");
                                    String email = profile.getString("email");
                                    if (name == null) name = "Unnamed Entrant";
                                    if (email == null) email = "No email";

                                    String joined = "Unknown date";
                                    Object ts = doc.get("timestamp");
                                    if (ts instanceof Timestamp) {
                                        joined = dateFormat.format(((Timestamp) ts).toDate());
                                    } else if (ts instanceof Long) {
                                        joined = dateFormat.format(new Date((Long) ts));
                                    }

                                    addEntrantCard(name, email, joined, status);
                                })
                                .addOnFailureListener(e ->
                                        addEntrantCard("Unknown", "Error loading profile", "N/A", status)
                                );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading " + status + " entrants", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * adds one entrant card to the UI
     *
     * @param name entrant full name
     * @param email entrant email
     * @param joinDate formatted date they joined or were updated
     * @param status entrant status (waiting, selected, enrolled, cancelled)
     */
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
        entrantsContainer.addView(card);
    }
}

