package com.example.yellow.users;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.yellow.MainActivity;
import com.example.yellow.R;
import com.example.yellow.organizers.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class WaitingListFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseUser user;

    private String eventId;
    private String userId;
    private TextView titleText;
    private TextView dateText;
    private TextView locationText;
    private ImageView bannerImage;   // 🆕 poster banner

    public WaitingListFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_waiting_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // status bar spacer
        View spacer = view.findViewById(R.id.statusBarSpacer);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v2, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.LayoutParams lp = spacer.getLayoutParams();
            lp.height = bars.top;  // push content below the notch/status bar
            spacer.setLayoutParams(lp);
            return insets;
        });

        // Firestore + User
        db   = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        userId  = user.getUid();
        eventId = getArguments().getString("eventId");

        // UI
        Button   leaveButton = view.findViewById(R.id.leaveButton);
        ImageView backArrow  = view.findViewById(R.id.backArrow);
        TextView userCount   = view.findViewById(R.id.userCount);

        // Header UI
        titleText    = view.findViewById(R.id.eventTitle);
        dateText     = view.findViewById(R.id.eventDateTime);
        locationText = view.findViewById(R.id.eventLocation);
        bannerImage  = view.findViewById(R.id.eventBanner);  // 🆕

        loadEventDetails();

        // Real-time waiting list count
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null) {
                        userCount.setText(String.valueOf(snapshot.size()));
                    }
                });

        // Auto-join waiting list
        joinWaitingRoom();

        // Leave waiting room
        leaveButton.setOnClickListener(v -> leaveWaitingRoom());

        // Back arrow: go back but stay in waiting list
        backArrow.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // System back: LEAVE waiting list
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        leaveWaitingRoom();
                    }
                }
        );
    }

    private void loadEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Event event = doc.toObject(Event.class);
                    if (event == null) return;

                    // Title
                    titleText.setText(event.getName());

                    // Date WITHOUT "@ location"
                    String dateAndLoc = event.getFormattedDateAndLocation();
                    int atIndex = dateAndLoc.indexOf('@');
                    String dateOnly = (atIndex >= 0)
                            ? dateAndLoc.substring(0, atIndex).trim()
                            : dateAndLoc;
                    dateText.setText(dateOnly);

                    // Location label
                    locationText.setText(event.getLocation());

                    // 🆕 Poster image
                    if (bannerImage != null) {
                        String poster = event.getPosterImageUrl();
                        if (poster != null && !poster.isEmpty()) {
                            Glide.with(this)
                                    .load(poster)
                                    .centerCrop()
                                    .placeholder(R.drawable.ic_image_icon)
                                    .error(R.drawable.ic_image_icon)
                                    .into(bannerImage);
                        } else {
                            bannerImage.setImageResource(R.drawable.ic_image_icon);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load event info", Toast.LENGTH_SHORT).show()
                );
    }

    // Join waiting list
    private void joinWaitingRoom() {
        DocumentReference ref = db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .document(userId);

        ref.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                WaitingUser entry = new WaitingUser(userId, eventId);

                ref.set(entry).addOnSuccessListener(unused -> {
                    db.collection("events")
                            .document(eventId)
                            .update("waitlisted", FieldValue.increment(1));

                    Toast.makeText(getContext(),
                            "Joined waiting room", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Waiting user model
    public static class WaitingUser {
        public String userId;
        public String eventId;
        public Object timestamp = FieldValue.serverTimestamp();

        public WaitingUser() {}

        public WaitingUser(String userId, String eventId) {
            this.userId = userId;
            this.eventId = eventId;
        }
    }

    // Leave waiting room
    private void leaveWaitingRoom() {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> {

                    db.collection("events")
                            .document(eventId)
                            .update("waitlisted", FieldValue.increment(-1));

                    Toast.makeText(getContext(),
                            "You left the waiting room", Toast.LENGTH_SHORT).show();

                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).showHomeUI(true);
                    }

                    requireActivity().getSupportFragmentManager().popBackStack();
                });
    }
}