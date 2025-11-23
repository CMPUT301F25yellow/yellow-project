package com.example.yellow.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.yellow.R;
import com.example.yellow.organizers.ViewEventActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity that provides access to admin tools and management features.
 * Only users with the "admin" role in Firestore can access this dashboard.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    /**
     * Called when the activity is created.
     * Verifies whether the signed-in user has admin privileges.
     *
     * @param savedInstanceState Previously saved state, if any.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // ---- Step 1: Verify admin role ----
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Access denied: not signed in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("roles").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && "admin".equals(doc.getString("role"))) {
                        setupAdminUI(); // Load dashboard
                    } else {
                        Toast.makeText(this, "Access denied: not an admin.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to verify role.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Sets up the admin interface, including buttons and layout adjustments.
     */
    private void setupAdminUI() {
        // ---- Step 2: Inflate layout ----
        setContentView(R.layout.activity_admin_dashboard);

        // Match dark header with system bar color
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.surface_dark));

        // ---- Step 3: Insets for top (status bar) & bottom (nav bar) ----
        final View root = findViewById(R.id.adminRoot);
        final View spacer = findViewById(R.id.statusBarSpacer);
        final View scroll = findViewById(R.id.scroll);

        // Keep original padding values to add insets cleanly
        final int padStart = (scroll != null) ? scroll.getPaddingStart() : 0;
        final int padTop = (scroll != null) ? scroll.getPaddingTop() : 0;
        final int padEnd = (scroll != null) ? scroll.getPaddingEnd() : 0;
        final int padBottom = (scroll != null) ? scroll.getPaddingBottom() : 0;

        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                // Top spacer: exact status bar height
                if (spacer != null) {
                    ViewGroup.LayoutParams lp = spacer.getLayoutParams();
                    if (lp != null && lp.height != bars.top) {
                        lp.height = bars.top;
                        spacer.setLayoutParams(lp);
                    }
                }

                // Add bottom nav bar padding to scroll
                if (scroll != null) {
                    scroll.setPaddingRelative(padStart, padTop, padEnd, padBottom + bars.bottom);
                }

                return insets;
            });

            // Force insets dispatch immediately
            ViewCompat.requestApplyInsets(root);
        }

        // ---- Step 4: Back button ----
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        // ---- Step 5: Buttons ----
        MaterialButton btnViewEvents = findViewById(R.id.btnViewEvents);
        MaterialButton btnViewProfiles = findViewById(R.id.btnViewProfiles);
        MaterialButton btnViewImages = findViewById(R.id.btnViewImages);
        MaterialButton btnViewLogs = findViewById(R.id.btnViewLogs);

        // Open Manage Events
        if (btnViewEvents != null) {
            btnViewEvents.setOnClickListener(v -> {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(android.R.id.content, new ManageEventsFragment()) // replaces entire screen
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Open Manage Profile
        if (btnViewProfiles != null) {
            btnViewProfiles.setOnClickListener(v -> getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new ManageProfilesFragment())
                    .addToBackStack(null)
                    .commit());
        }

        if (btnViewImages != null) {
            btnViewImages.setOnClickListener(v -> getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new ManageImagesFragment())
                    .addToBackStack(null)
                    .commit());
        }

        if (btnViewLogs != null) {
            btnViewLogs.setOnClickListener(v -> getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new ManageNotificationLogFragment())
                    .addToBackStack(null)
                    .commit());
        }
    }
}