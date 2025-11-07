package com.example.yellow.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide; // If not using Glide, remove this import and the Glide block below.
import com.example.yellow.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date;

/**
 * Admin screen to browse all events as a list and allow deleting an event.
 */
public class ManageEventsFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout listContainer;
    private View spacer, scroll;
    private ListenerRegistration reg;

    /**
     * Creates and returns the Manage Events layout.
     *
     * @param inflater  Layout inflater.
     * @param container Optional parent view.
     * @param savedInstanceState Saved state, if any.
     * @return The root view for this fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_events, container, false);
    }

    /**
     * Binds views, applies window insets, sets back navigation, and starts listening to events.
     *
     * @param v The fragment root view.
     * @param savedInstanceState Saved state, if any.
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        spacer        = v.findViewById(R.id.statusBarSpacer);
        scroll        = v.findViewById(R.id.scroll);
        listContainer = v.findViewById(R.id.listContainer);

        // Insets: top spacer + bottom padding
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (spacer != null) {
                ViewGroup.LayoutParams lp = spacer.getLayoutParams();
                if (lp != null && lp.height != bars.top) {
                    lp.height = bars.top;
                    spacer.setLayoutParams(lp);
                }
            }
            if (scroll != null) {
                int ps = scroll.getPaddingStart();
                int pt = scroll.getPaddingTop();
                int pe = scroll.getPaddingEnd();
                int pb = scroll.getPaddingBottom();
                scroll.setPaddingRelative(ps, pt, pe, pb + bars.bottom);
            }
            return insets;
        });

        // Back button
        View btnBack = v.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(x ->
                    requireActivity().getSupportFragmentManager().popBackStack()
            );
        }

        db = FirebaseFirestore.getInstance();
        listenForEvents();
    }

    /**
     * Cleans up the Firestore listener and view refs.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) { reg.remove(); reg = null; }
        spacer = null; scroll = null; listContainer = null;
    }

    /**
     * Subscribes to the "events" collection and rebuilds the list on changes.
     */
    private void listenForEvents() {
        reg = db.collection("events")
                // .orderBy("startDate") // optional (add Firestore index if prompted)
                .addSnapshotListener((snap, err) -> {
                    if (!isAdded()) return;
                    if (err != null || snap == null) {
                        toast("Failed to load events.");
                        return;
                    }
                    if (listContainer == null) return;

                    listContainer.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String id             = d.getId();
                        String name           = str(d, "name");
                        String organizerName  = str(d, "organizerName");
                        String posterUrl      = str(d, "posterImageUrl");
                        String dateLabel      = formatDateField(d.get("startDate"));

                        View card = inflater.inflate(R.layout.manage_event_card_admin, listContainer, false);

                        ImageView ivThumb         = card.findViewById(R.id.thumb);
                        TextView tvTitle          = card.findViewById(R.id.title);
                        TextView tvDate           = card.findViewById(R.id.date);
                        TextView tvOrganizer      = card.findViewById(R.id.organizer);
                        MaterialButton btnDelete  = card.findViewById(R.id.btnDelete);

                        tvTitle.setText(!name.isEmpty() ? name : "(untitled)");
                        tvDate.setText(!dateLabel.isEmpty() ? dateLabel : "No date");
                        tvOrganizer.setText(!organizerName.isEmpty() ? ("By " + organizerName) : "By —");

                        if (!TextUtils.isEmpty(posterUrl)) {
                            try {
                                Glide.with(this)
                                        .load(posterUrl)
                                        .placeholder(R.drawable.ic_image_icon)
                                        .error(R.drawable.ic_image_icon)
                                        .centerCrop()
                                        .into(ivThumb);
                            } catch (Throwable t) {
                                ivThumb.setImageResource(R.drawable.ic_image_icon);
                            }
                        } else {
                            ivThumb.setImageResource(R.drawable.ic_image_icon);
                        }

                        btnDelete.setOnClickListener(v ->
                                confirmDelete(id, name, posterUrl)
                        );

                        listContainer.addView(card);
                    }

                    if (snap.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No events found.");
                        empty.setTextColor(getResources().getColor(R.color.white));
                        empty.setAlpha(0.7f);
                        empty.setPadding(8, 16, 8, 0);
                        listContainer.addView(empty);
                    }
                });
    }

    /**
     * Shows a confirmation dialog before deleting the event (and poster if present).
     *
     * @param eventId  The Firestore document ID of the event.
     * @param title    The event name (used for dialog text).
     * @param posterUrl The download URL of the poster image (may be empty).
     */
    private void confirmDelete(@NonNull String eventId,
                               @NonNull String title,
                               @NonNull String posterUrl) {
        String label = title.isEmpty() ? eventId : title;
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete event?")
                .setMessage("This will permanently delete \"" + label + "\".")
                .setPositiveButton("Delete", (d, w) -> deleteEventAndPoster(eventId, posterUrl))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the event document and then attempts to delete its poster in Firebase Storage.
     *
     * @param eventId  The Firestore document ID to delete.
     * @param posterUrl The download URL of the poster image (may be empty).
     */
    private void deleteEventAndPoster(@NonNull String eventId, @NonNull String posterUrl) {
        db.collection("events").document(eventId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (!TextUtils.isEmpty(posterUrl)) {
                        try {
                            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(posterUrl);
                            ref.delete()
                                    .addOnSuccessListener(__ -> toast("Event deleted. Poster removed."))
                                    .addOnFailureListener(e -> toast("Event deleted. Poster not removed."));
                        } catch (Throwable t) {
                            toast("Event deleted. (Poster delete skipped)");
                        }
                    } else {
                        toast("Event deleted.");
                    }
                })
                .addOnFailureListener(e -> toast("Delete failed: " + e.getMessage()));
    }

    /**
     * Safely reads a string field from a document.
     *
     * @param d   The source document.
     * @param key The field name.
     * @return The trimmed string value or an empty string.
     */
    private String str(DocumentSnapshot d, String key) {
        String s = d.getString(key);
        return s == null ? "" : s.trim();
    }

    /**
     * Formats a Firestore field as a "date • time" label.
     *
     * @param raw The raw field value (Timestamp or String).
     * @return A formatted date-time label, or an empty string if not available.
     */
    private String formatDateField(Object raw) {
        if (raw instanceof Timestamp) {
            Date dt = ((Timestamp) raw).toDate();
            String day  = DateFormat.format("MMM d, yyyy", dt).toString();
            String time = DateFormat.format("h:mm a", dt).toString();
            return day + " • " + time;
        }
        if (raw instanceof String) return (String) raw;
        return "";
    }

    /**
     * Shows a short toast if the fragment is attached.
     *
     * @param m Message to display.
     */
    private void toast(String m) {
        if (!isAdded()) return;
        android.widget.Toast.makeText(getContext(), m, android.widget.Toast.LENGTH_SHORT).show();
    }
}