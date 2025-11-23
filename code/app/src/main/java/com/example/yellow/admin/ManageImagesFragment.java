package com.example.yellow.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin screen to browse uploaded poster images and remove them from events.
 *
 * Implements:
 * - US 03.03.01: As an administrator, I want to be able to remove images.
 * - US 03.06.01: As an administrator, I want to be able to browse images
 * that are uploaded so I can remove them if necessary.
 *
 * NOTE: This fragment now ONLY deals with poster images
 * (posterImageUrl/posterUrl),
 * not QR images.
 */
public class ManageImagesFragment extends Fragment {

    private FirebaseFirestore db;
    private GridLayout listContainer;
    private View spacer, scroll;
    private ListenerRegistration reg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Layout should define:
        // - @id/statusBarSpacer
        // - @id/btnBack
        // - @id/scroll
        // - @id/listContainer
        return inflater.inflate(R.layout.fragment_manage_images, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        spacer = v.findViewById(R.id.statusBarSpacer);
        scroll = v.findViewById(R.id.scroll);
        listContainer = v.findViewById(R.id.listContainer);

        // Insets: top spacer + bottom padding (same pattern as ManageEventsFragment)
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
            btnBack.setOnClickListener(x -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        db = FirebaseFirestore.getInstance();
        listenForImages();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) {
            reg.remove();
            reg = null;
        }
        spacer = null;
        scroll = null;
        listContainer = null;
    }

    /**
     * Listens to the "events" collection and builds a list of all
     * POSTER images attached to events (posterImageUrl).
     * QR fields are ignored.
     */
    private void listenForImages() {
        reg = db.collection("events")
                .addSnapshotListener((snap, err) -> {
                    if (!isAdded())
                        return;
                    if (err != null || snap == null) {
                        toast("Failed to load images.");
                        return;
                    }
                    if (listContainer == null)
                        return;

                    listContainer.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    int count = 0;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String eventId = d.getId();
                        String name = safe(d.getString("name"));
                        String organizerName = safe(d.getString("organizerName"));
                        String posterUri = safe(d.getString("posterImageUrl"));

                        // Only care about events that actually HAVE a poster
                        if (TextUtils.isEmpty(posterUri)) {
                            continue;
                        }

                        count++;

                        View card = inflater.inflate(
                                R.layout.manage_images_card_admin, listContainer, false);

                        // Set grid layout params (width 0, weight 1)
                        // We cast the params because inflate returns the parent's param type
                        // (GridLayout.LayoutParams)
                        GridLayout.LayoutParams params = (GridLayout.LayoutParams) card.getLayoutParams();
                        params.width = 0;
                        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                        params.setMargins(8, 8, 8, 8);
                        card.setLayoutParams(params);

                        ImageView ivThumb = card.findViewById(R.id.posterThumb);
                        TextView tvTitle = card.findViewById(R.id.title);
                        TextView tvUploaderName = card.findViewById(R.id.tvUploaderName);
                        View btnDelete = card.findViewById(R.id.btnDeleteImage); // Now an ImageView/View

                        tvTitle.setText("Event : " + (!name.isEmpty() ? name : "(untitled)"));
                        tvUploaderName.setText("by " + (!organizerName.isEmpty() ? organizerName : "Unknown"));

                        try {
                            Glide.with(this)
                                    .load(posterUri)
                                    .placeholder(R.drawable.ic_image_icon)
                                    .error(R.drawable.ic_image_icon)
                                    .centerCrop()
                                    .into(ivThumb);
                        } catch (Throwable t) {
                            ivThumb.setImageResource(R.drawable.ic_image_icon);
                        }

                        btnDelete.setOnClickListener(v -> confirmRemovePosterImage(eventId, name));

                        listContainer.addView(card);
                    }

                    if (count == 0) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No poster images found.");
                        empty.setTextColor(getResources().getColor(R.color.white));
                        empty.setAlpha(0.7f);
                        empty.setPadding(8, 16, 8, 0);
                        listContainer.addView(empty);
                    }
                });
    }

    /**
     * Ask admin before removing the POSTER image from the event doc.
     */
    private void confirmRemovePosterImage(@NonNull String eventId,
            @NonNull String eventName) {
        String label = eventName.isEmpty() ? eventId : eventName;

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove poster image?")
                .setMessage("This will remove the poster image for \"" + label + "\".")
                .setPositiveButton("Remove", (d, w) -> removePosterImageFromEvent(eventId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Clears only the poster fields from the event document.
     *
     * - posterImageUrl: Base64 or URL for the poster
     * - posterUrl : extra field if you also store a download URL
     */
    private void removePosterImageFromEvent(@NonNull String eventId) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("posterImageUrl", null);
        patch.put("posterUrl", null);

        db.collection("events").document(eventId)
                .update(patch)
                .addOnSuccessListener(unused -> toast("Poster image removed."))
                .addOnFailureListener(e -> toast("Remove failed: " + (e != null ? e.getMessage() : "unknown error")));
    }

    /** Null-safe trim helper. */
    private static String safe(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    /** Short toast helper that checks attachment. */
    private void toast(String m) {
        if (!isAdded())
            return;
        android.widget.Toast.makeText(getContext(), m, android.widget.Toast.LENGTH_SHORT).show();
    }
}