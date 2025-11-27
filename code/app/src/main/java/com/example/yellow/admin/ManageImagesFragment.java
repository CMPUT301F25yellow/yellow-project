package com.example.yellow.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.yellow.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
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
    private RecyclerView recyclerView;
    private ManageImagesAdapter adapter;
    private View spacer; // Removed scroll view reference as RecyclerView handles scrolling
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
        recyclerView = v.findViewById(R.id.listContainer);

        // Setup RecyclerView
        adapter = new ManageImagesAdapter(getContext(), this::confirmRemovePosterImage);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

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
            // Add bottom padding to RecyclerView for navigation bar
            if (recyclerView != null) {
                int ps = recyclerView.getPaddingStart();
                int pt = recyclerView.getPaddingTop();
                int pe = recyclerView.getPaddingEnd();
                int pb = recyclerView.getPaddingBottom(); // Original bottom padding
                // We want to add bars.bottom to the original padding, but we need to be careful
                // not to keep adding it if this runs multiple times.
                // However, standard practice is just setting it.
                // Since we defined padding="8dp" in XML, we should respect that.
                // Let's just set padding bottom to 8dp + bars.bottom
                int basePadding = (int) (8 * getResources().getDisplayMetrics().density);
                recyclerView.setPaddingRelative(ps, pt, pe, basePadding + bars.bottom);
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
        recyclerView = null;
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

                    List<ManageImagesAdapter.ImageItem> items = new java.util.ArrayList<>();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String eventId = d.getId();
                        String name = safe(d.getString("name"));
                        String organizerName = safe(d.getString("organizerName"));
                        String posterUri = safe(d.getString("posterImageUrl"));

                        // Only care about events that actually HAVE a poster
                        if (!TextUtils.isEmpty(posterUri)) {
                            items.add(new ManageImagesAdapter.ImageItem(eventId, name, organizerName, posterUri));
                        }
                    }

                    if (adapter != null) {
                        adapter.setItems(items);
                    }

                    // Handle empty state if needed (optional, could show a TextView)
                    if (items.isEmpty()) {
                        // We could show a toast or toggle a "No images" view visibility
                        // For now, just clearing the list is enough as per requirement
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