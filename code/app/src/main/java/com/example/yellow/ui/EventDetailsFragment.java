package com.example.yellow.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.yellow.R;
import com.example.yellow.organizers.Event;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Fragment that handles the event details screen layout and appearance.
 * Provides Event details based on the provided event ID
 *
 * @author Marcus Lau mdlau
 */
public class EventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private ListenerRegistration waitingListListener;
    private OnJoinWaitlistClickListener listener;

    /**
     * An interface for the Join Waitlist button.
     * This allows decouples the EventDetailsFragment from the MainActivity
     * such that the button can be tested in isolation.
     *
     */
    public interface OnJoinWaitlistClickListener {
        void onJoinWaitlistClicked(String eventId);
    }

    /**
     * Called when the fragment is first attached to its context.
     * Attaches the listener to the activity if it implements OnJoinWaitlistClickListener
     * otherwise throws an exception.
     * @param context
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnJoinWaitlistClickListener) {
            listener = (OnJoinWaitlistClickListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnJoinWaitlistClickListener");
        }
    }

    // Setter for testing purposes
    public void setOnJoinWaitlistClickListener(OnJoinWaitlistClickListener listener) {
        this.listener = listener;
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation).
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored.
     * Sets up the necessary views, listeners, and data.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Handle status bar insets
        View spacer = view.findViewById(R.id.statusBarSpacer);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.LayoutParams lp = spacer.getLayoutParams();
            lp.height = bars.top;
            spacer.setLayoutParams(lp);
            return insets;
        });

        // Firestore
        db = FirebaseFirestore.getInstance();

        // Find views from the new layout
        ImageView backArrow = view.findViewById(R.id.backArrow);
        TextView headerTitle = view.findViewById(R.id.headerTitle);
        ImageView eventBanner = view.findViewById(R.id.eventBanner);
        TextView eventTitle = view.findViewById(R.id.eventTitle);
        TextView eventOrganizer = view.findViewById(R.id.eventOrganizer);
        TextView eventDateTime = view.findViewById(R.id.eventDateTime);
        TextView eventLocation = view.findViewById(R.id.eventLocation);
        LinearLayout geolocationRow = view.findViewById(R.id.geolocationRow);
        TextView eventGeolocation = view.findViewById(R.id.eventGeolocation);
        TextView eventMaxEntrants = view.findViewById(R.id.eventMaxEntrants);
        TextView eventEntrants = view.findViewById(R.id.eventEntrants);
        TextView eventDescription = view.findViewById(R.id.eventDescription);
        Button joinEventButton = view.findViewById(R.id.joinEventButton);
//        SwitchMaterial notificationSwitch = view.findViewById(R.id.notificationSwitch); // Not yet implemented

        // Set up the back navigation
        backArrow.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Lottery Guidelines button
        Button btnLotteryGuidelines = view.findViewById(R.id.btnLotteryGuidelines);
        if (btnLotteryGuidelines != null) {
            btnLotteryGuidelines.setOnClickListener(v -> showLotteryGuidelinesDialog());
        }

        // Get event ID from arguments and fetch data
        if (getArguments() != null) {
            String eventId = getArguments().getString("qr_code_data");
//            String qrData = getArguments().getString("qr_code_data");
//            String eventId = com.example.yellow.utils.QrUtils.getEventIdFromUri(qrData);

            if (eventId != null) {
                DocumentReference eventRef = db.collection("events").document(eventId);
                eventRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event == null) return;

                        // Populate the views
                        headerTitle.setText(event.getName());
                        eventTitle.setText(event.getName());
                        eventOrganizer.setText(event.getOrganizerName()); // Set the organizer name (NOT WORKING)
                        eventLocation.setText(event.getLocation());
                        eventDescription.setText(event.getDescription());

                        // Geolocation
                        if (event.isRequireGeolocation()) {
                            geolocationRow.setVisibility(View.VISIBLE);
                            eventGeolocation.setText("Geolocation is required for check-in");
                        } else {
                            geolocationRow.setVisibility(View.GONE);
                        }

                        // Max Entrants
                        if (event.getMaxEntrants() > 0) {
                            eventMaxEntrants.setText("Up to " + event.getMaxEntrants() + " entrants");
                        } else {
                            eventMaxEntrants.setText("Unlimited entrants");
                        }

                        // Live count taken from WaitingListFragment
                        waitingListListener = db.collection("events")
                                .document(eventId)
                                .collection("waitingList")
                                .addSnapshotListener((snapshot, e) -> {
                                    if (e != null) {
                                        Log.w("EventDetailsFragment", "Listen failed.", e);
                                        return;
                                    }
                                    if (snapshot != null) {
                                        String numEntrants = "Currently " + snapshot.size() + " entrant(s)";
                                        eventEntrants.setText(numEntrants);
                                    }
                                });

                        // Format and display date range
                        if (event.getRegistrationStartDate() != null) {
                            SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd", Locale.getDefault());
                            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

                            if (event.getRegistrationEndDate() != null && !event.getRegistrationStartDate().equals(event.getRegistrationEndDate())) {
                                String startDate = monthDayFormat.format(event.getRegistrationStartDate().toDate()) + ", " + yearFormat.format(event.getRegistrationStartDate().toDate());
                                String endDate = monthDayFormat.format(event.getRegistrationEndDate().toDate()) + ", " + yearFormat.format(event.getRegistrationEndDate().toDate());
                                eventDateTime.setText(String.format("%s - %s ",startDate, endDate));

                            } else {
                                eventDateTime.setText(monthDayFormat.format(event.getRegistrationStartDate().toDate()));
                            }
                        }

                        // Load poster image with Glide
                        if (event.getPosterImageUrl() != null && !event.getPosterImageUrl().isEmpty()) {
                            Glide.with(this).load(event.getPosterImageUrl()).into(eventBanner);
                        } else {
                            eventBanner.setImageResource(R.drawable.ic_image_icon); // Default image
                        }

                        // Set up the Join Event button
                        joinEventButton.setOnClickListener(v -> {
//                            if (getActivity() instanceof MainActivity) {
//                                ((MainActivity) getActivity()).openWaitingRoom(eventId);
//                            }
                            if (listener != null) {
                                listener.onJoinWaitlistClicked(eventId);
                            }
                        });

                    } else {
                        Log.d("EventDetailsFragment", "No such document: " + eventId);
                        headerTitle.setText("Event Not Found");
                    }
                }).addOnFailureListener(e -> {
                    Log.e("EventDetailsFragment", "Error fetching event", e);
                    headerTitle.setText("Error loading event");
                });
            }
        }
    }

    /**
     * Shows a dialog with lottery criteria and guidelines.
     * Taken from WaitingListFragment
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

    /**
     * Called when the fragment is no longer in use.
     * Destroys the listener when the view is destroyed to prevent memory leaks and crashes
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Detach the listener when the view is destroyed to prevent memory leaks and crashes
        if (waitingListListener != null) {
            waitingListListener.remove();
        }
    }

    /**
     * Called when the fragment is no longer attached to its activity.
     * Sets the listener to null to prevent memory leaks and crashes
     */
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null; // Set the listener to null to prevent memory leaks and crashes
    }
}
