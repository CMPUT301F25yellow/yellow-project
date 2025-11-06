package com.example.yellow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.yellow.organizers.CreateEventActivity;
import com.example.yellow.ui.HistoryFragment;
import com.example.yellow.ui.MyEventsFragment;
import com.example.yellow.ui.NotificationFragment;
import com.example.yellow.ui.ProfileUserFragment;
import com.example.yellow.ui.QrScanFragment;
import com.example.yellow.users.WaitingListFragment;
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
        View root          = findViewById(R.id.main);
        header             = findViewById(R.id.header_main);
        scrollContent      = findViewById(R.id.scrollContent);
        bottomNav          = findViewById(R.id.bottomNavigationView);
        fragmentContainer  = findViewById(R.id.fragmentContainer);


        // ---- Header icons ----
        View iconProfile = findViewById(R.id.iconProfile);
        if (iconProfile != null) iconProfile.setOnClickListener(v -> openProfile());

        View iconNotifications = findViewById(R.id.iconNotifications);
        if (iconNotifications != null) iconNotifications.setOnClickListener(v -> openNotifications());

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
                    showHomeUI(true);
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    return true;
                } else if (id == R.id.nav_history) {
                    openHistory();
                    return true;
                } else if (id == R.id.nav_create_event) {
                    startActivity(new Intent(MainActivity.this, CreateEventActivity.class));
                    return true;
                } else if (id == R.id.nav_my_events) {
                    openMyEvents();
                    return true;
                } else if (id == R.id.nav_scan) {
                    openQrScan();
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
            @Override public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });

        //joins waiting room
        Button join1 = findViewById(R.id.join_waiting_room_1);
        if (join1 != null) {
            join1.setOnClickListener(v -> {
                String eventId = "4AHZ0xJWF2kEsxQTpcpN"; // <-- real event ID
                openWaitingRoom(eventId);
            });
        }
    }

    // ---------- Helpers ----------

    private int dp(int d) {
        return Math.round(getResources().getDisplayMetrics().density * d);
    }

    /** Centralized open method: choose if bottom nav stays visible. */
    private void openFragment(Fragment fragment, String tag, boolean keepBottomNavVisible) {
        // Hide header + scroll
        if (header != null) header.setVisibility(View.GONE);
        if (scrollContent != null) scrollContent.setVisibility(View.GONE);

        // Fragment container visible
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.VISIBLE);
            fragmentContainer.bringToFront();
        }

        // Decide bottom nav visibility per screen
        if (bottomNav != null) {
            bottomNav.setVisibility(keepBottomNavVisible ? View.VISIBLE : View.GONE);
        }

        // Navigate
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    public void showHomeUI(boolean show) {
        int visible = show ? View.VISIBLE : View.GONE;
        if (header != null) header.setVisibility(visible);
        if (scrollContent != null) scrollContent.setVisibility(visible);
        if (fragmentContainer != null) fragmentContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        if (bottomNav != null) bottomNav.setVisibility(visible);
    }

    // ---------- Screens ----------

    // Profile: no bottom nav
    private void openProfile() {
        // Optional: prevent item checks while in a full-screen fragment
        if (bottomNav != null) bottomNav.getMenu().setGroupCheckable(0, false, true);
        openFragment(new ProfileUserFragment(), "Profile", /*keepBottomNavVisible=*/false);
    }

    // Notifications: no bottom nav
    private void openNotifications() {
        openFragment(new NotificationFragment(), "Notifications", /*keepBottomNavVisible=*/false);
    }

    // QR Scan: keep bottom nav
    private void openQrScan() {
        openFragment(new QrScanFragment(), "QR_SCAN", /*keepBottomNavVisible=*/true);
    }

    // History: keep bottom nav
    private void openHistory() {
        openFragment(new HistoryFragment(), "History", /*keepBottomNavVisible=*/true);
    }

    // My Events: keep bottom nav
    private void openMyEvents() {
        openFragment(new MyEventsFragment(), "MyEvents", /*keepBottomNavVisible=*/true);
    // ------Join waiting list---------

    //waiting list
    public void openWaitingRoom(String eventId) {
        WaitingListFragment fragment = new WaitingListFragment();

        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);

        // full screen → hide everything including bottom nav
        openFragment(fragment, "WAITING_ROOM", /*keepBottomNavVisible=*/false);
    }
}