package com.example.yellow.ui;

import android.content.Intent; // <-- ADDED
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileUserFragment extends Fragment {

    private TextInputEditText inputFullName, inputEmail, inputPhone;
    private MaterialButton btnSave, btnDeleteProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_profile_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Views
        inputFullName = v.findViewById(R.id.inputFullName);
        inputEmail    = v.findViewById(R.id.inputEmail);
        inputPhone    = v.findViewById(R.id.inputPhone);
        btnSave       = v.findViewById(R.id.btnSave);
        btnDeleteProfile = v.findViewById(R.id.btnDeleteProfile);

        requireActivity().getWindow().setStatusBarColor(
                ContextCompat.getColor(requireContext(), R.color.surface_dark)
        );

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

        View btnBack = v.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(b ->
                    requireActivity().getSupportFragmentManager().popBackStack()
            );
        }

        // Firebase
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // Make sure we have a user (anonymous is fine), then load & toggle admin button
        ensureSignedIn(() -> {
            loadProfile();
            toggleAdminButtonFromFirestore();   // <-- ADDED
        });

        // Save
        btnSave.setOnClickListener(v1 -> saveProfile());

        // Optional: delete profile document
        if (btnDeleteProfile != null) {
            btnDeleteProfile.setOnClickListener(v13 -> deleteProfile());
        }
    }

    // ----- Auth -----
    private interface AfterLogin { void go(); }

    private void ensureSignedIn(@NonNull AfterLogin afterLogin) {
        FirebaseUser cur = auth.getCurrentUser();
        if (cur != null) {
            afterLogin.go();
            return;
        }
        // Anonymous sign-in
        auth.signInAnonymously()
                .addOnSuccessListener(r -> afterLogin.go())
                .addOnFailureListener(e -> {
                    toast("Auth failed: " + e.getMessage());
                    // Disable Save to avoid writing without a UID
                    if (btnSave != null) btnSave.setEnabled(false);
                });
    }

    private String uidOrNull() {
        FirebaseUser u = auth.getCurrentUser();
        return (u == null) ? null : u.getUid();
    }

    // ----- Load -----
    private void loadProfile() {
        String uid = uidOrNull();
        if (uid == null) return;

        db.collection("profiles")
                .document(uid)
                .get()
                .addOnSuccessListener(this::bindDocToUi)
                .addOnFailureListener(e -> toast("Load failed: " + e.getMessage()));
    }

    private void bindDocToUi(DocumentSnapshot doc) {
        if (doc != null && doc.exists()) {
            String name  = doc.getString("fullName");
            String email = doc.getString("email");
            String phone = doc.getString("phone");

            inputFullName.setText(name != null ? name : "");
            inputEmail.setText(email != null ? email : "");
            inputPhone.setText(phone != null ? phone : "");
        } else {
            // New user: clear fields
            inputFullName.setText("");
            inputEmail.setText("");
            inputPhone.setText("");
        }
    }

    // ----- Save -----
    private void saveProfile() {
        String uid = uidOrNull();
        if (uid == null) {
            toast("Please try again: not signed in.");
            return;
        }

        String name  = safe(inputFullName);
        String email = safe(inputEmail);
        String phone = safe(inputPhone);

        // (Optional) quick client-side validation
        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(email) && TextUtils.isEmpty(phone)) {
            toast("Nothing to save.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("fullName",  name);
        data.put("email",     email);
        data.put("phone",     phone);
        data.put("updatedAt", Timestamp.now());

        db.collection("profiles")
                .document(uid)
                .set(data, SetOptions.merge())  // merge so we don’t wipe other fields
                .addOnSuccessListener(unused -> toast("Profile saved"))
                .addOnFailureListener(e -> toast("Save failed: " + e.getMessage()));
    }

    private void deleteProfile() {
        String uid = uidOrNull();
        if (uid == null) return;

        db.collection("profiles")
                .document(uid)
                .delete()
                .addOnSuccessListener(unused -> {
                    toast("Profile deleted");
                    inputFullName.setText("");
                    inputEmail.setText("");
                    inputPhone.setText("");
                })
                .addOnFailureListener(e -> toast("Delete failed: " + e.getMessage()));
    }

    // ----- Admin toggle (NEW) -----
    private void toggleAdminButtonFromFirestore() {
        View btnAdmin = getView() != null ? getView().findViewById(R.id.btnAdminDashboard) : null;
        if (btnAdmin == null) return;

        String uid = uidOrNull();
        if (uid == null) { btnAdmin.setVisibility(View.GONE); return; }

        // Start hidden until we confirm admin
        btnAdmin.setVisibility(View.GONE);

        db.collection("roles").document(uid)
                .addSnapshotListener((doc, err) -> {
                    if (err != null) {
                        android.util.Log.e("ADMIN", "roles read error: " + err.getMessage(), err);
                        // Do NOT force-hide again on error; just keep current state
                        return;
                    }
                    if (doc == null || !doc.exists()) {
                        android.util.Log.d("ADMIN", "roles/" + uid + " missing");
                        btnAdmin.setVisibility(View.GONE);
                        btnAdmin.setOnClickListener(null);
                        return;
                    }
                    String role = doc.getString("role");
                    android.util.Log.d("ADMIN", "role=" + role);
                    boolean isAdmin = "admin".equals(role);
                    btnAdmin.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    if (isAdmin) {
                        btnAdmin.setOnClickListener(v ->
                                startActivity(new android.content.Intent(
                                        requireContext(), com.example.yellow.admin.AdminDashboardActivity.class)));
                    } else {
                        btnAdmin.setOnClickListener(null);
                    }
                });
    }

    // ----- Helpers -----
    private String safe(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}