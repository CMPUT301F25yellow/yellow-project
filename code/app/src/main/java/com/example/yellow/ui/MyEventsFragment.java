package com.example.yellow.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.yellow.R;
import com.example.yellow.organizers.Event;
import com.example.yellow.utils.FirebaseManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyEventsFragment extends Fragment {

    private LinearLayout myEventsContainer;
    private TextView tvEventCount;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        myEventsContainer = v.findViewById(R.id.myEventsContainer);
        tvEventCount = v.findViewById(R.id.tvEventCount);

        // Clear container before reloading
        myEventsContainer.removeAllViews();

        // Initialize Firebase
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to see your events", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load events created by this user
        db.collection("events")
                .whereEqualTo("organizerId", currentUser.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    tvEventCount.setText(count + " events");

                    if (count == 0) {
                        Toast.makeText(getContext(), "You haven't created any events yet", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event == null) continue;

                        // Inflate your home-style card layout
                        View card = inflater.inflate(R.layout.item_event_card, myEventsContainer, false);

                        TextView title = card.findViewById(R.id.eventTitle);
                        TextView details = card.findViewById(R.id.eventDetails);
                        ImageView image = card.findViewById(R.id.eventImage);
                        Button button = card.findViewById(R.id.eventButton);

                        title.setText(event.getName());
                        details.setText(formatEventDetails(event));

                        // Load image if available
                        if (event.getPosterImageUrl() != null && !event.getPosterImageUrl().isEmpty()) {
                            Glide.with(this)
                                    .load(event.getPosterImageUrl())
                                    .centerCrop()
                                    .placeholder(R.drawable.my_image)
                                    .into(image);
                        } else {
                            image.setImageResource(R.drawable.my_image);
                        }

                        // Simple button functionality (customize later)
                        button.setOnClickListener(view ->
                                Toast.makeText(getContext(),
                                        "Clicked: " + event.getName(),
                                        Toast.LENGTH_SHORT).show());

                        // Add to container
                        myEventsContainer.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMyEvents() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseManager.getInstance().getEventsByOrganizer(userId, new FirebaseManager.GetEventsCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documents) {
                myEventsContainer.removeAllViews();

                if (documents.isEmpty()) {
                    tvEventCount.setText("No events found.");
                    return;
                }

                tvEventCount.setText(documents.size() + " events");

                for (DocumentSnapshot doc : documents) {
                    Event event = doc.toObject(Event.class);
                    if (event != null) addEventCard(event);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addEventCard(Event event) {
        if (getContext() == null) return;

        // Card container
        CardView card = new CardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 20);
        card.setLayoutParams(cardParams);
        card.setRadius(20);
        card.setCardElevation(6);
        card.setCardBackgroundColor(getResources().getColor(R.color.surface_dark));

        // Inner layout
        LinearLayout inner = new LinearLayout(getContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(24, 24, 24, 24);

        // Event name
        TextView nameView = new TextView(getContext());
        nameView.setText(event.getName());
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(18);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        inner.addView(nameView);

        // Date and location
        String dateText = "";
        if (event.getStartDate() != null) {
            Date date = event.getStartDate().toDate();
            dateText = dateFormat.format(date);
        }
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            dateText += " · " + event.getLocation();
        }

        TextView infoView = new TextView(getContext());
        infoView.setText(dateText);
        infoView.setTextColor(getResources().getColor(R.color.hinty));
        infoView.setTextSize(14);
        inner.addView(infoView);

        card.addView(inner);
        myEventsContainer.addView(card);
    }

    private String formatEventDetails(Event event) {
        if (event.getStartDate() == null || event.getLocation() == null)
            return "";

        Date date = event.getStartDate().toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(date) + " · " + event.getLocation();
    }
}