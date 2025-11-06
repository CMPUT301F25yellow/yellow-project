package com.example.yellow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.yellow.organizers.CreateEventActivity;
import com.example.yellow.ui.ProfileUserFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private View header;
    private View scrollContent;
    private View fragmentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // ---- Views ----
        View root     = findViewById(R.id.main);
        header        = findViewById(R.id.header_main);
        scrollContent = findViewById(R.id.scrollContent);
        bottomNav     = findViewById(R.id.bottomNavigationView);
        fragmentContainer = findViewById(R.id.fragmentContainer);

        // ---- Header icons ----
        View iconProfile = findViewById(R.id.iconProfile);
        if (iconProfile != null) {
            iconProfile.setOnClickListener(v -> openProfileFragment());
        }

        // ---- Safe-area insets ----
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

        // ---- Bottom navigation ----
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    // TODO: show HomeFragment if you add one
                    return true;
                } else if (id == R.id.nav_history) {
                    // TODO: show HistoryFragment
                    return true;
                } else if (id == R.id.nav_create_event) {
                    startActivity(new Intent(MainActivity.this, CreateEventActivity.class));
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

        // ---- Restore Home UI when back stack empties ----
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                showHomeUI(true);
                if (bottomNav != null) {
                    bottomNav.getMenu().setGroupCheckable(0, true, true);
                    bottomNav.setSelectedItemId(R.id.nav_home);
                }
            }
        });

        // ---- Modern back handling (Android 13–16 compatible) ----
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();    // Profile -> Home
                } else {
                    finish();                                       // Exit from Home
                }
            }
        });
    }

    private int dp(int d) {
        return Math.round(getResources().getDisplayMetrics().density * d);
    }

    // ---- Open Profile as a fragment ----
    private void openProfileFragment() {
        showHomeUI(false);                           // hide header/list/nav
        if (bottomNav != null) {
            bottomNav.getMenu().setGroupCheckable(0, false, true);
        }
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.VISIBLE);
            fragmentContainer.bringToFront();
        }
        replaceInContainer(new ProfileUserFragment(), "Profile");
    }

    // ---- Toggle Home views vs fragment container ----
    private void showHomeUI(boolean show) {
        int visible = show ? View.VISIBLE : View.GONE;
        if (header != null)        header.setVisibility(visible);
        if (scrollContent != null) scrollContent.setVisibility(visible);
        if (fragmentContainer != null)
            fragmentContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        if (bottomNav != null)     bottomNav.setVisibility(visible);
    }

    private void replaceInContainer(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }
}