package com.example.yellow.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private MaterialButton btnSave, btnCancel, btnDeleteProfile;

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
        // ^ If your fragment layout file is different, change this to that file (NOT the host layout).
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Views
        inputFullName = v.findViewById(R.id.inputFullName);
        inputEmail    = v.findViewById(R.id.inputEmail);
        inputPhone    = v.findViewById(R.id.inputPhone);
        btnSave       = v.findViewById(R.id.btnSave);
        btnCancel     = v.findViewById(R.id.btnCancel);
        btnDeleteProfile     = v.findViewById(R.id.btnDeleteProfile);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // Make sure we have a user (anonymous is fine), then load
        ensureSignedIn(() -> loadProfile());

        // Save
        btnSave.setOnClickListener(v1 -> saveProfile());

        // Cancel (just reload from server; or you can clear fields)
        btnCancel.setOnClickListener(v12 -> loadProfile());

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