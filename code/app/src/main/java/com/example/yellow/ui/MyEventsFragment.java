package com.example.yellow.ui;

import android.content.Intent;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.yellow.R;
import com.example.yellow.organizers.Event;
import com.example.yellow.organizers.ViewEventActivity;
import com.example.yellow.utils.FirebaseManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * shows all events created by the logged-in organizer
 * updates live when events are added or removed
 */
//authors: waylon, will
public class MyEventsFragment extends Fragment {

    private ListenerRegistration registration;
    private LinearLayout myEventsContainer;
    private TextView tvEventCount;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /** inflates the layout for the my events screen */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_events, container, false);
    }

    /** sets up the view, loads events, and listens for live updates */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        View spacer = v.findViewById(R.id.statusBarSpacer);
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.LayoutParams lp = spacer.getLayoutParams();
            if (lp.height != bars.top) {
                lp.height = bars.top;
                spacer.setLayoutParams(lp);
            }
            return insets;
        });

        myEventsContainer = v.findViewById(R.id.myEventsContainer);
        tvEventCount = v.findViewById(R.id.tvEventCount);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to see your events", Toast.LENGTH_SHORT).show();
            return;
        }

        // [NEW] Check Profile before loading events
        com.example.yellow.utils.ProfileUtils.checkProfile(getContext(), isComplete -> {
            if (isComplete) {
                setupEventsListener(db, currentUser);
            }
        }, () -> {
            // Navigate to ProfileUserFragment
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ProfileUserFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupEventsListener(FirebaseFirestore db, FirebaseUser currentUser) {
        // remove any previous listener to avoid duplicate updates
        if (registration != null) {
            registration.remove();
        }

        // listen for real-time updates to this user’s events
        registration = db.collection("events")
                .whereEqualTo("organizerId", currentUser.getUid())
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        myEventsContainer.removeAllViews();
                        tvEventCount.setText("0 events");
                        Toast.makeText(getContext(), "You haven't created any events yet", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    myEventsContainer.removeAllViews();
                    int count = querySnapshot.size();
                    tvEventCount.setText(count + " events");

                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event == null)
                            continue;

                        View card = inflater.inflate(R.layout.item_event_card, myEventsContainer, false);

                        TextView title = card.findViewById(R.id.eventTitle);
                        TextView details = card.findViewById(R.id.eventDetails);
                        ImageView image = card.findViewById(R.id.eventImage);
                        Button button = card.findViewById(R.id.eventButton);

                        title.setText(event.getName());
                        details.setText(formatEventDetails(event));

                        if (event.getPosterImageUrl() != null && !event.getPosterImageUrl().isEmpty()) {
                            Glide.with(this)
                                    .load(event.getPosterImageUrl())
                                    .centerCrop()
                                    .placeholder(R.drawable.my_image)
                                    .into(image);
                        } else {
                            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            image.setImageResource(R.drawable.ic_image_icon);
                        }

                        button.setText("View Event");
                        button.setOnClickListener(view -> {
                            Intent intent = new Intent(getContext(), ViewEventActivity.class);
                            intent.putExtra("eventId", event.getId());
                            intent.putExtra("eventName", event.getName());
                            intent.putExtra("eventDate", formatEventDetails(event));
                            startActivity(intent);
                        });

                        myEventsContainer.addView(card);
                    }
                });
    }

    /** stops the Firestore listener when the fragment is not visible */
    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    /** loads all events created by the current user */
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
                    tvEventCount.setText("No events found");
                    return;
                }

                tvEventCount.setText(documents.size() + " events");

                for (DocumentSnapshot doc : documents) {
                    Event event = doc.toObject(Event.class);
                    if (event != null)
                        addEventCard(event);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * adds one event card to the list
     *
     * @param event the event object to display
     */
    private void addEventCard(Event event) {
        if (getContext() == null)
            return;

        CardView card = new CardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 20);
        card.setLayoutParams(cardParams);
        card.setRadius(20);
        card.setCardElevation(6);
        card.setCardBackgroundColor(getResources().getColor(R.color.surface_dark));

        LinearLayout inner = new LinearLayout(getContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(24, 24, 24, 24);

        TextView nameView = new TextView(getContext());
        nameView.setText(event.getName());
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(18);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        inner.addView(nameView);

        String dateText = "";
        if (event.getRegistrationStartDate() != null) {
            Date date = event.getRegistrationStartDate().toDate();
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

    /**
     * formats the event date and location for display
     *
     * @param event the event object
     * @return formatted string like "Nov 10, 2025 · Edmonton"
     */
    private String formatEventDetails(Event event) {
        if (event.getRegistrationStartDate() == null || event.getLocation() == null)
            return "";

        Date date = event.getRegistrationStartDate().toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(date) + " · " + event.getLocation();
    }
}
