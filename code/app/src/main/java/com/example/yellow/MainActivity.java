package com.example.yellow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.yellow.organizers.CreateEventActivity;
import com.example.yellow.users.WaitingListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Views
        View root          = findViewById(R.id.main);
        View header        = findViewById(R.id.header_main);      // header container
        View scrollContent = findViewById(R.id.scrollContent);    // your ScrollView (id in XML)
        bottomNav          = findViewById(R.id.bottomNavigationView);

        // NEW: header icons
        View iconProfile = findViewById(R.id.iconProfile);
        // (Optional) if you want to wire notifications later:
        // View iconNotifications = findViewById(R.id.iconNotifications);

        // Open ProfileActivity on profile icon tap
        if (iconProfile != null) {
            iconProfile.setOnClickListener(v -> {
                Intent i = new Intent(MainActivity.this, ProfileActivity.class);
                // If you need admin view: i.putExtra("role", "admin");
                startActivity(i);
            });
        }
        View iconWaitingRoom = findViewById(R.id.iconWaitingRoom);
        //opens waiting room
        if (iconWaitingRoom != null) {
            iconWaitingRoom.setOnClickListener(v -> {

                // Hide the ScrollView content
                scrollContent.setVisibility(View.GONE);

                // Show the fragment container
                View fragmentContainer = findViewById(R.id.fragment_container);
                fragmentContainer.setVisibility(View.VISIBLE);

                // Load fragment
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new WaitingListFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }
        // Window insets: keep header safe, keep content above nav, nav flush to bottom
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (header != null) {
                header.setPadding(
                        header.getPaddingLeft(),
                        bars.top + dp(12),
                        header.getPaddingRight(),
                        header.getPaddingBottom()
                );
            }

            if (scrollContent != null) {
                scrollContent.setPadding(
                        scrollContent.getPaddingLeft(),
                        scrollContent.getPaddingTop(),
                        scrollContent.getPaddingRight(),
                        bars.bottom + dp(16)
                );
            }

            if (bottomNav != null) {
                bottomNav.setPadding(
                        bottomNav.getPaddingLeft(),
                        bottomNav.getPaddingTop(),
                        bottomNav.getPaddingRight(),
                        0
                );
            }
            return insets;
        });

        // Bottom nav selection (keep as-is; hook up fragments when ready)
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // TODO: show HomeFragment
                return true;
            } else if (id == R.id.nav_history) {
                // TODO: show HistoryFragment
                return true;
            } else if (id == R.id.nav_create_event) {
                Intent intent = new Intent(MainActivity.this, CreateEventActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_my_events) {
                // TODO: show MyEventsFragment
                return true;
            } else if (id == R.id.nav_scan) {
                // TODO: show ScannerFragment
                return true;
            }
            return false;
        });

    }

    public void onBackPressedDispatcher() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {

            // Remove fragment
            getSupportFragmentManager().popBackStack();

            // Show home content
            findViewById(R.id.scrollContent).setVisibility(View.VISIBLE);
            findViewById(R.id.fragment_container).setVisibility(View.GONE);

        } else {
            super.getOnBackPressedDispatcher();
        }
    }

    private int dp(int d) {
        return Math.round(getResources().getDisplayMetrics().density * d);
    }
}