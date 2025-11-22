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

/**
 * Profile screen for users to add/view/edit/delete their profile.
 */

public class ProfileUserFragment extends Fragment {

    private TextInputEditText inputFullName, inputEmail, inputPhone;
    private MaterialButton btnSave, btnDeleteProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    /**
     * Creates and returns the layout for the profile screen.
     *
     * @param inflater           the LayoutInflater used to inflate views
     * @param container          the parent view that the fragment's UI will attach
     *                           to
     * @param savedInstanceState any saved instance data
     * @return the root View for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_users, container, false);
    }

    /**
     * Sets up buttons, Firebase, and loads user profile information.
     *
     * @param v                  the root view
     * @param savedInstanceState the saved state, if any
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Views
        inputFullName = v.findViewById(R.id.inputFullName);
        inputEmail = v.findViewById(R.id.inputEmail);
        inputPhone = v.findViewById(R.id.inputPhone);
        btnSave = v.findViewById(R.id.btnSave);
        btnDeleteProfile = v.findViewById(R.id.btnDeleteProfile);

        requireActivity().getWindow().setStatusBarColor(
                ContextCompat.getColor(requireContext(), R.color.surface_dark));

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
            btnBack.setOnClickListener(b -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Make sure we have a user (anonymous is fine), then load & toggle admin button
        ensureSignedIn(() -> {
            loadProfile();
            toggleAdminButtonFromFirestore();
        });

        // Save
        btnSave.setOnClickListener(v1 -> saveProfile());

        // Optional: delete profile document
        if (btnDeleteProfile != null) {
            btnDeleteProfile.setOnClickListener(v13 -> deleteProfile());
        }
    }

    // ----- Auth -----
    /** Callback to run after sign-in. */
    private interface AfterLogin {
        void go();
    }

    /**
     * Ensures there is a signed-in user.
     * If no user is currently signed in, this method signs them in anonymously.
     *
     * @param afterLogin runs once the user is confirmed signed in
     */
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
                    if (btnSave != null)
                        btnSave.setEnabled(false);
                });
    }

    /**
     * Returns the current user's UID or null if not signed in.
     *
     * @return UID string or null.
     */
    private String uidOrNull() {
        FirebaseUser u = auth.getCurrentUser();
        return (u == null) ? null : u.getUid();
    }

    // ----- Load -----
    /** Loads profile from Firestore. */
    private void loadProfile() {
        String uid = uidOrNull();
        if (uid == null)
            return;

        db.collection("profiles")
                .document(uid)
                .get()
                .addOnSuccessListener(this::bindDocToUi)
                .addOnFailureListener(e -> toast("Load failed: " + e.getMessage()));
    }

    /**
     * Writes the Firestore document values into the input fields.
     *
     * @param doc Profile document snapshot.
     */
    private void bindDocToUi(DocumentSnapshot doc) {
        if (doc != null && doc.exists()) {
            String name = doc.getString("fullName");
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

    /**
     * Saves the current inputs to Firestore (merge).
     */
    private void saveProfile() {
        String uid = uidOrNull();
        if (uid == null) {
            toast("Please try again: not signed in.");
            return;
        }

        String name = safe(inputFullName);
        String email = safe(inputEmail);
        String phone = safe(inputPhone);

        // (Optional) quick client-side validation
        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(email) && TextUtils.isEmpty(phone)) {
            toast("Nothing to save.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("fullName", name);
        data.put("email", email);
        data.put("phone", phone);
        data.put("updatedAt", Timestamp.now());

        db.collection("profiles")
                .document(uid)
                .set(data, SetOptions.merge()) // merge so we don’t wipe other fields
                .addOnSuccessListener(unused -> toast("Profile saved"))
                .addOnFailureListener(e -> toast("Save failed: " + e.getMessage()));
    }

    /**
     * Deletes the user's profile document from Firestore and clears inputs.
     */
    private void deleteProfile() {
        String uid = uidOrNull();
        if (uid == null)
            return;

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
    /**
     * Shows or hides the Admin Dashboard button based on the user's role.
     * Listens to changes on roles/{uid}.
     */
    private void toggleAdminButtonFromFirestore() {
        View btnAdmin = getView() != null ? getView().findViewById(R.id.btnAdminDashboard) : null;
        if (btnAdmin == null)
            return;

        // [NEW] Device-based admin check
        if (com.example.yellow.utils.DeviceIdentityManager.isCurrentDeviceAdmin()) {
            btnAdmin.setVisibility(View.VISIBLE);
            btnAdmin.setOnClickListener(v -> startActivity(new android.content.Intent(
                    requireContext(), com.example.yellow.admin.AdminDashboardActivity.class)));
        } else {
            btnAdmin.setVisibility(View.GONE);
            // Try to refresh silently in case status changed recently
            com.example.yellow.utils.DeviceIdentityManager
                    .ensureDeviceDocument(getContext(),
                            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser())
                    .addOnSuccessListener(isAdmin -> {
                        if (isAdmin) {
                            btnAdmin.setVisibility(View.VISIBLE);
                            btnAdmin.setOnClickListener(v -> startActivity(new android.content.Intent(
                                    requireContext(), com.example.yellow.admin.AdminDashboardActivity.class)));
                        }
                    });
        }
    }

    // ----- Helpers -----
    /**
     * Returns trimmed text from an input or an empty string if null.
     *
     * @param et Input field.
     * @return Trimmed string (never null).
     */
    private String safe(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    /**
     * Shows a short toast message if the context is available.
     *
     * @param msg Message to display.
     */
    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}