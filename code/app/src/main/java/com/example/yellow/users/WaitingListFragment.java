package com.example.yellow.users;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;

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
import com.example.yellow.users.WaitingUser;
import com.example.yellow.utils.LocationHelper;
import com.example.yellow.utils.ProfileUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
//author: waylon
public class WaitingListFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseUser user;
    private String eventId;
    private String userId;
    private TextView titleText;
    private TextView dateText;
    private TextView locationText;
    private ImageView bannerImage; // 🆕 poster banner
    private Event currentEvent; // To hold the loaded event details
    private LocationHelper locationHelper;

    public WaitingListFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize LocationHelper here. We pass "this" as ActivityResultCaller (the
        // Fragment),
        // and requireContext() for Toasts and FusedLocationProviderClient.
        locationHelper = new LocationHelper(
                this, // ActivityResultCaller -> the Fragment
                requireContext(), // Context
                location -> {
                    if (location != null) {
                        // Successfully got a location → save waiting user with coordinates
                        saveWaitingUser(location.getLatitude(), location.getLongitude());
                    } else {
                        // Could not get location (permission denied, GPS error, etc.)
                        Toast.makeText(
                                getContext(),
                                "Failed to get location. Cannot join this event.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

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
            lp.height = bars.top; // push content below the notch/status bar
            spacer.setLayoutParams(lp);
            return insets;
        });

        // Firestore + User
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        userId = user.getUid();
        eventId = getArguments().getString("eventId");

        // UI
        Button leaveButton = view.findViewById(R.id.leaveButton);
        ImageView backArrow = view.findViewById(R.id.backArrow);
        TextView userCount = view.findViewById(R.id.userCount);

        // Header UI
        titleText = view.findViewById(R.id.eventTitle);
        dateText = view.findViewById(R.id.eventDateTime);
        locationText = view.findViewById(R.id.eventLocation);
        bannerImage = view.findViewById(R.id.eventBanner); // 🆕

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

        // Leave waiting room
        leaveButton.setOnClickListener(v -> leaveWaitingRoom());

        // Back arrow: go back but stay in waiting list
        backArrow.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Lottery Guidelines button
        Button btnLotteryGuidelines = view.findViewById(R.id.btnLotteryGuidelines);
        if (btnLotteryGuidelines != null) {
            btnLotteryGuidelines.setOnClickListener(v -> showLotteryGuidelinesDialog());
        }

        // System back: LEAVE waiting list
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        leaveWaitingRoom();
                    }
                });
    }

    private void loadEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists())
                        return;

                    currentEvent = doc.toObject(Event.class);
                    if (currentEvent == null)
                        return;

                    // Title
                    titleText.setText(currentEvent.getName());

                    // Date WITHOUT "@ location"
                    String dateAndLoc = currentEvent.getFormattedDateAndLocation();
                    int atIndex = dateAndLoc.indexOf('@');
                    String dateOnly = (atIndex >= 0)
                            ? dateAndLoc.substring(0, atIndex).trim()
                            : dateAndLoc;
                    dateText.setText(dateOnly);

                    // Location label
                    locationText.setText(currentEvent.getLocation());

                    // 🆕 Poster image
                    if (bannerImage != null) {
                        String poster = currentEvent.getPosterImageUrl();
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
                    ProfileUtils.checkProfile(getContext(), isComplete -> {
                        if (isComplete) {
                            joinWaitingRoom(); // This will now have access to currentEvent
                        }
                    }, () -> {
                        // Navigate to profile
                        if (requireActivity() instanceof MainActivity) {
                            ((MainActivity) requireActivity()).openProfile();
                        }
                    });
                })
                .addOnFailureListener(
                        e -> Toast.makeText(getContext(), "Failed to load event info", Toast.LENGTH_SHORT).show());
    }

    // Join waiting list
    private void joinWaitingRoom() {
        if (currentEvent == null) {
            // Event details haven't loaded yet. This can happen if Firestore is slow.
            // We can wait a moment and try again, or just ask the user to wait.
            if (getContext() != null) {
                Toast.makeText(getContext(), "Connecting to event...", Toast.LENGTH_SHORT).show();
            }
            // A more robust solution might use a listener or retry, but for now we'll just
            // wait for user action.
            return;
        }
        // 🔥 CHECK 1 — Is user already enrolled?
        db.collection("events").document(eventId)
                .collection("enrolled").document(userId)
                .get()
                .addOnSuccessListener(enrolledSnap -> {
                    if (enrolledSnap.exists()) {
                        Toast.makeText(getContext(),
                                "You are already enrolled in this event.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 🔥 CHECK 2 — Is user selected (won the lottery)?
                    db.collection("events").document(eventId)
                            .collection("selected").document(userId)
                            .get()
                            .addOnSuccessListener(selectedSnap -> {
                                if (selectedSnap.exists()) {
                                    Toast.makeText(getContext(),
                                            "You have already been selected for this event.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                // 🔥 CHECK 3 — Is user cancelled? (Optional)
                                db.collection("events").document(eventId)
                                        .collection("cancelled").document(userId)
                                        .get()
                                        .addOnSuccessListener(cancelSnap -> {
                                            if (cancelSnap.exists()) {
                                                Toast.makeText(getContext(),
                                                        "You cannot rejoin the waiting list.",
                                                        Toast.LENGTH_LONG).show();
                                                return;
                                            }

                                            // 🔥 PASS — Now continue to your existing logic
                                            continueJoinWaitingRoom();
                                        });
                            });
                });
    }
    private void continueJoinWaitingRoom() {
        DocumentReference ref = db.collection("events").document(eventId)
                .collection("waitingList").document(userId);

        ref.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Toast.makeText(getContext(),
                        "You're already on the waiting list for this event",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // (your existing code for checking max waiting list size)
            db.collection("events").document(eventId)
                    .collection("waitingList")
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        int currentSize = snapshot.size();
                        int maxSize = currentEvent.getMaxEntrants();

                        //blank = unlimited
                        if (maxSize <= 0) {
                            // skip capacity check entirely
                            if (currentEvent.isRequireGeolocation()) {
                                Toast.makeText(getContext(),
                                        "Location is required, getting your position...",
                                        Toast.LENGTH_SHORT).show();
                                locationHelper.getCurrentLocation();
                            } else {
                                saveWaitingUser(null, null);
                            }
                            return;
                        }

                        if (currentSize >= maxSize) {
                            Toast.makeText(getContext(),
                                    "The waiting list is full (max " + maxSize + ")",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (currentEvent.isRequireGeolocation()) {
                            Toast.makeText(getContext(),
                                    "Location is required, getting your position...",
                                    Toast.LENGTH_SHORT).show();
                            locationHelper.getCurrentLocation();
                        } else {
                            saveWaitingUser(null, null);
                        }
                    });
        });
    }
    /**
     * Helper method to create and save the WaitingUser object to Firestore.
     * 
     * @param latitude  The user's latitude, or null if not provided.
     * @param longitude The user's longitude, or null if not provided.
     */
    private void saveWaitingUser(@Nullable Double latitude, @Nullable Double longitude) {
        DocumentReference ref = db.collection("events").document(eventId).collection("waitingList").document(userId);

        WaitingUser entry = new WaitingUser(userId, eventId);

        // Set name from Firebase profile
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            entry.setName(user.getDisplayName());
        } else {
            entry.setName("Anonymous User");
        }

        // Set location data if it was provided
        if (latitude != null && longitude != null) {
            entry.setLatitude(latitude);
            entry.setLongitude(longitude);
        }

        ref.set(entry).addOnSuccessListener(unused -> {
            db.collection("events").document(eventId).update("waitlisted", FieldValue.increment(1));
            if (getContext() != null) {
                Toast.makeText(getContext(), "Successfully joined waiting list!", Toast.LENGTH_SHORT).show();
            }

        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: Could not join waiting room.", Toast.LENGTH_SHORT).show();
            }
        });
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

    /**
     * Shows a dialog with lottery criteria and guidelines
     */
    private void showLotteryGuidelinesDialog() {
        if (getContext() == null)
            return;

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_lottery_guidelines, null);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Got it", (d, which) -> d.dismiss())
                .create();

        // Set the background color of the dialog window to match the card
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.surface_dark);
        }

        dialog.show();
    }
}