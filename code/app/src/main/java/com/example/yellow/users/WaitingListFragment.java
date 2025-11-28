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

public class WaitingListFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseUser user;
    private String eventId;
    private String userId;

    private TextView titleText;
    private TextView dateText;
    private TextView locationText;
    private ImageView bannerImage;
    private TextView userCountText;

    private Event currentEvent;
    private LocationHelper locationHelper;

    public WaitingListFragment() {
    }

    public static WaitingListFragment newInstance(String eventId) {
        WaitingListFragment fragment = new WaitingListFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_waiting_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // status bar spacer
        View spacer = view.findViewById(R.id.statusBarSpacer);
        if (spacer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(view, (v2, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                ViewGroup.LayoutParams lp = spacer.getLayoutParams();
                lp.height = bars.top; // push content below the status bar
                spacer.setLayoutParams(lp);
                return insets;
            });
        }

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
        userCountText = view.findViewById(R.id.userCount);

        // Header UI
        titleText = view.findViewById(R.id.eventTitle);
        dateText = view.findViewById(R.id.eventDateTime);
        locationText = view.findViewById(R.id.eventLocation);
        bannerImage = view.findViewById(R.id.eventBanner);

        // Initialize LocationHelper with your actual API (Consumer<Location>)
        locationHelper = new LocationHelper(
                this,                    // ActivityResultCaller -> Fragment
                requireContext(),        // Context
                location -> {
                    if (location != null) {
                        // Successfully got a location → save waiting user with coordinates
                        saveWaitingUser(location.getLatitude(), location.getLongitude());
                    } else {
                        // Could not get location (permission denied, GPS error, etc.)
                        Toast.makeText(
                                getContext(),
                                "Failed to get location. Cannot join this event.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

        // Back arrow: leave waiting room
        backArrow.setOnClickListener(v -> leaveWaitingRoom());

        // Leave button: leave waiting room
        leaveButton.setOnClickListener(v -> leaveWaitingRoom());

        // System back: leave waiting room
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        leaveWaitingRoom();
                    }
                });

        // Real-time waiting list count
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && userCountText != null) {
                        userCountText.setText(String.valueOf(snapshot.size()));
                    }
                });

        loadEventDetails();
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
                    if (currentEvent.getStartDate() != null) {
                        java.text.SimpleDateFormat fmt =
                                new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                        String dateStr = fmt.format(currentEvent.getStartDate().toDate());
                        dateText.setText(dateStr);
                    }

                    // Location
                    locationText.setText(currentEvent.getLocation());

                    // Poster banner
                    String poster = currentEvent.getPosterImageUrl();
                    if (poster != null && !poster.isEmpty()) {
                        Glide.with(this)
                                .load(poster)
                                .centerCrop()
                                .into(bannerImage);
                    } else {
                        bannerImage.setImageResource(R.drawable.ic_image_icon);
                    }

                    // After loading event, ensure user has a profile, then join
                    ProfileUtils.checkProfile(
                            getContext(),
                            isComplete -> {
                                if (isComplete) {
                                    joinWaitingRoom(); // This will now have access to currentEvent
                                }
                            },
                            () -> {
                                // Navigate to profile
                                if (requireActivity() instanceof MainActivity) {
                                    ((MainActivity) requireActivity()).openProfile();
                                }
                            });
                })
                .addOnFailureListener(
                        e -> Toast.makeText(getContext(),
                                "Failed to load event info", Toast.LENGTH_SHORT).show());
    }

    // Join waiting list
    private void joinWaitingRoom() {
        if (currentEvent == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(),
                        "Connecting to event...", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        DocumentReference ref = db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .document(userId);

        ref.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "You're already on the waiting list for this event",
                            Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // Check if geolocation is required
            if (currentEvent.isRequireGeolocation()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Location is required, getting your position...",
                            Toast.LENGTH_SHORT).show();
                }

                if (locationHelper != null) {
                    locationHelper.getCurrentLocation();
                } else {
                    Toast.makeText(getContext(),
                            "Location helper not initialized. Cannot join this event.",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                // CASE 2: location NOT required
                saveWaitingUser(null, null);
            }
        });
    }

    /**
     * Helper method to create and save the WaitingUser object to Firestore,
     * while enforcing the event's maxEntrants (waiting list capacity).
     *
     * @param latitude  The user's latitude, or null if not provided.
     * @param longitude The user's longitude, or null if not provided.
     */
    private void saveWaitingUser(@Nullable Double latitude, @Nullable Double longitude) {
        if (eventId == null || userId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(),
                        "Missing event or user information.",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // References for event (for capacity) and this user's waiting-list entry
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference ref = eventRef.collection("waitingList").document(userId);

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

        // Check the event's maxEntrants against current waitlisted count
        eventRef.get()
                .addOnSuccessListener(eventDoc -> {
                    if (getContext() == null) return;

                    if (!eventDoc.exists()) {
                        Toast.makeText(getContext(),
                                "Event no longer exists.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Long maxEntrantsLong = eventDoc.getLong("maxEntrants");
                    int maxEntrants = (maxEntrantsLong != null)
                            ? maxEntrantsLong.intValue()
                            : 0;

                    if (maxEntrants > 0) {
                        Long waitlistedLong = eventDoc.getLong("waitlisted");
                        long waitlisted = (waitlistedLong != null) ? waitlistedLong : 0L;

                        if (waitlisted >= maxEntrants) {
                            Toast.makeText(getContext(),
                                    "The waiting list for this event is full.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    // Capacity OK – save entry and increment waitlisted counter
                    ref.set(entry)
                            .addOnSuccessListener(unused -> {
                                eventRef.update("waitlisted", FieldValue.increment(1));
                                if (getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "Successfully joined waiting list!",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "Error: Could not join waiting room.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "Error: Could not check event capacity.",
                                Toast.LENGTH_SHORT).show();
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
                            "You left the waiting room",
                            Toast.LENGTH_SHORT).show();

                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).showHomeUI(true);
                    }

                    requireActivity().getSupportFragmentManager().popBackStack();
                });
    }

    private void showLotteryGuidelinesDialog() {
        if (getContext() == null)
            return;

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_lottery_guidelines, null);

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
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
