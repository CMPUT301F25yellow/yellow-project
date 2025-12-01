package com.example.yellow.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

/**
 * Admin screen for viewing and removing user profiles.
 * Displays all profiles in real-time from Firestore and allows deletion.
 * 
 * This fragment provides functionality to:
 * - View all user profiles with name, email, and phone
 * - Delete user profiles (including their events and subcollections)
 * - Real-time updates when profiles are added or removed
 * 
 * Note: Deleting a profile also triggers cascade deletion of all events
 * created by that user via UserCleanupUtils.
 * 
 * @author Tabrez
 */
public class ManageProfilesFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout listContainer;
    private View scroll;
    private View spacer;
    private ListenerRegistration liveReg;

    /**
     * Creates and returns the Manage Profiles layout.
     *
     * @param inflater           Layout inflater.
     * @param container          Optional parent container.
     * @param savedInstanceState Saved state, if any.
     * @return The root view for this fragment.
     */
    /**
     * Creates and returns the Manage Profiles layout.
     *
     * @param inflater           Layout inflater
     * @param container          Optional parent container
     * @param savedInstanceState Saved state, if any
     * @return The root view for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_profiles, container, false);
    }

    /**
     * Sets up UI elements, window insets, and starts listening to profile updates.
     *
     * @param v                  The root view.
     * @param savedInstanceState Saved state, if any.
     */
    /**
     * Sets up UI elements, window insets, and starts listening to profile updates.
     *
     * @param v                  The root view
     * @param savedInstanceState Saved state, if any
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        spacer = v.findViewById(R.id.statusBarSpacer);
        View btnBack = v.findViewById(R.id.btnBack);
        scroll = v.findViewById(R.id.scroll);
        listContainer = v.findViewById(R.id.listContainer);

        // Insets: top spacer + bottom padding on scroll
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (spacer != null) {
                LayoutParams lp = spacer.getLayoutParams();
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

        if (btnBack != null) {
            btnBack.setOnClickListener(x -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        db = FirebaseFirestore.getInstance();
        startLiveProfilesListener();
    }

    /**
     * Removes Firestore listener and clears references when the view is destroyed.
     */
    /**
     * Removes Firestore listener and clears references when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (liveReg != null) {
            liveReg.remove();
            liveReg = null;
        }
        listContainer = null;
        scroll = null;
        spacer = null;
    }

    /**
     * Starts listening for real-time updates to the "profiles" collection
     * and displays them dynamically.
     */
    private void startLiveProfilesListener() {
        liveReg = db.collection("profiles")
                // .orderBy("fullName") // enable if you store a name and want sorting
                .addSnapshotListener((snap, err) -> {
                    if (!isAdded())
                        return;

                    if (err != null) {
                        Toast.makeText(getContext(), "Load failed: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap == null || listContainer == null)
                        return;

                    listContainer.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String uid = d.getId();
                        String name = safe(d.getString("fullName"));
                        String email = safe(d.getString("email"));
                        String phone = safe(d.getString("phone"));

                        View card = inflater.inflate(R.layout.manage_profile_card_admin, listContainer, false);

                        TextView tvName = card.findViewById(R.id.name);
                        TextView tvEmail = card.findViewById(R.id.email);
                        TextView tvPhone = card.findViewById(R.id.phone);
                        MaterialButton btnRemove = card.findViewById(R.id.btnDeleteUser);

                        tvName.setText(name.isEmpty() ? "(no name)" : name);
                        tvEmail.setText(email);
                        tvPhone.setText(phone.isEmpty() ? "N/A" : phone);

                        btnRemove.setOnClickListener(v -> confirmAndRemove(uid, name.isEmpty() ? uid : name));

                        listContainer.addView(card);
                    }

                    if (snap.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No profiles found.");
                        empty.setTextColor(getResources().getColor(R.color.white));
                        empty.setAlpha(0.7f);
                        empty.setPadding(8, 16, 8, 0);
                        listContainer.addView(empty);
                    }
                });
    }

    /**
     * Shows a confirmation dialog before removing a profile.
     *
     * @param uid     The user ID of the profile
     * @param display The name or fallback label of the user
     */
    private void confirmAndRemove(@NonNull String uid, @NonNull String display) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove user?")
                .setMessage("This will delete the profile for: " + display)
                .setPositiveButton("Remove", (d, w) -> removeProfileDocs(uid))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes a user's profile and role documents from Firestore.
     * Also deletes all events created by this user and their subcollections.
     * Does not delete their Firebase Auth account.
     *
     * @param uid The user ID to remove
     */
    private void removeProfileDocs(@NonNull String uid) {
        // Use cascade deletion to remove profile, events, and subcollections
        com.example.yellow.utils.UserCleanupUtils.deleteUserAndEvents(uid, db)
                .addOnSuccessListener(unused -> Toast
                        .makeText(getContext(), "Profile and events removed.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast
                        .makeText(getContext(), "Remove failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Safely trims a string or returns an empty string if null.
     *
     * @param s Input string
     * @return The trimmed string or empty string
     */
    private static String safe(@Nullable String s) {
        return s == null ? "" : s.trim();
    }
}