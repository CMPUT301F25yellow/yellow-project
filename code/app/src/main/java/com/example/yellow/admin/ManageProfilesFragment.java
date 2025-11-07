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

public class ManageProfilesFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout listContainer;
    private View scroll;
    private View spacer;
    private ListenerRegistration liveReg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_profiles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // ---- Grab views
        spacer        = v.findViewById(R.id.statusBarSpacer);
        View btnBack  = v.findViewById(R.id.btnBack);
        scroll        = v.findViewById(R.id.scroll);
        listContainer = v.findViewById(R.id.listContainer);

        // ---- Insets: top spacer + bottom padding on scroll
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
                // keep existing left/top/right paddings, add bottom inset
                int ps = scroll.getPaddingStart();
                int pt = scroll.getPaddingTop();
                int pe = scroll.getPaddingEnd();
                int pb = scroll.getPaddingBottom();
                scroll.setPaddingRelative(ps, pt, pe, pb + bars.bottom);
            }
            return insets;
        });

        // ---- Back button
        if (btnBack != null) {
            btnBack.setOnClickListener(x ->
                    requireActivity().getSupportFragmentManager().popBackStack()
            );
        }

        // ---- Firestore live list
        db = FirebaseFirestore.getInstance();
        startLiveProfilesListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (liveReg != null) { liveReg.remove(); liveReg = null; }
        listContainer = null;
        scroll = null;
        spacer = null;
    }

    private void startLiveProfilesListener() {
        liveReg = db.collection("profiles")
                //.orderBy("fullName") // uncomment if you store fullName
                .addSnapshotListener((snap, err) -> {
                    if (!isAdded()) return;

                    if (err != null) {
                        Toast.makeText(getContext(), "Load failed: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap == null || listContainer == null) return;

                    // Rebuild the list each time (simple & fine for admin)
                    listContainer.removeAllViews();

                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String uid   = d.getId();
                        String name  = safe(d.getString("fullName"));
                        String email = safe(d.getString("email"));

                        View card = inflater.inflate(R.layout.manage_profile_card_admin, listContainer, false);

                        TextView tvName  = card.findViewById(R.id.name);
                        TextView tvEmail = card.findViewById(R.id.email);
                        MaterialButton btnRemove = card.findViewById(R.id.btnDeleteUser);

                        tvName.setText(name.isEmpty() ? "(no name)" : name);
                        tvEmail.setText(email);

                        btnRemove.setOnClickListener(v -> confirmAndRemove(uid, name));

                        listContainer.addView(card);
                    }

                    // Optional: empty state
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

    private void confirmAndRemove(@NonNull String uid, @NonNull String name) {
        String who = name.isEmpty() ? uid : name;
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove user?")
                .setMessage("This will delete " + who + " from profiles and roles.")
                .setPositiveButton("Remove", (d, w) -> removeUser(uid))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeUser(@NonNull String uid) {
        WriteBatch b = db.batch();
        b.delete(db.collection("profiles").document(uid));
        b.delete(db.collection("roles").document(uid));
        // Add additional user-owned docs here if needed (events, uploads, etc.)

        b.commit()
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "User removed.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Remove failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private static String safe(@Nullable String s) {
        return s == null ? "" : s.trim();
    }
}