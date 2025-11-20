package com.example.yellow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.example.yellow.organizers.CreateEventActivity;
import com.example.yellow.organizers.Event;
import com.example.yellow.ui.HistoryFragment;
import com.example.yellow.ui.MyEventsFragment;
import com.example.yellow.ui.NotificationFragment;
import com.example.yellow.ui.ProfileUserFragment;
import com.example.yellow.ui.QrScanFragment;
import com.example.yellow.users.WaitingListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private View header;
    private View scrollContent;
    private View fragmentContainer;

    // NEW: listener handle for live events
    private ListenerRegistration eventsListener;

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

        // ---- Modern back handling ----
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });

        // Start live events feed
        startLiveEventsListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Firestore listener to avoid leaks
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }
    }

    // ---------- Helpers ----------

    private int dp(int d) {
        return Math.round(getResources().getDisplayMetrics().density * d);
    }

    /** Centralized open method: choose if bottom nav stays visible. */
    private void openFragment(Fragment fragment, String tag, boolean keepBottomNavVisible) {
        if (header != null) header.setVisibility(View.GONE);
        if (scrollContent != null) scrollContent.setVisibility(View.GONE);

        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.VISIBLE);
            fragmentContainer.bringToFront();
        }

        if (bottomNav != null) {
            bottomNav.setVisibility(keepBottomNavVisible ? View.VISIBLE : View.GONE);
        }

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

    private void openProfile() {
        if (bottomNav != null) bottomNav.getMenu().setGroupCheckable(0, false, true);
        openFragment(new ProfileUserFragment(), "Profile", /*keepBottomNavVisible=*/false);
    }

    private void openNotifications() {
        openFragment(new NotificationFragment(), "Notifications", /*keepBottomNavVisible=*/false);
    }

    private void openQrScan() {
        openFragment(new QrScanFragment(), "QR_SCAN", /*keepBottomNavVisible=*/true);
    }

    private void openHistory() {
        openFragment(new HistoryFragment(), "History", /*keepBottomNavVisible=*/true);
    }

    private void openMyEvents() {
        openFragment(new MyEventsFragment(), "MyEvents", /*keepBottomNavVisible=*/true);
    }

    public void openWaitingRoom(String eventId) {
        WaitingListFragment fragment = new WaitingListFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        openFragment(fragment, "WAITING_ROOM", /*keepBottomNavVisible=*/false);
    }

    // ---------- Live Events (auto-updating) ----------

    private void startLiveEventsListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        LinearLayout eventsContainer = findViewById(R.id.eventsContainer);

        // Remove any existing listener before attaching a new one
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }

        eventsListener = db.collection("events")
                // .orderBy("startTime") // uncomment if you store a sortable field
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Listen failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (querySnapshot == null || eventsContainer == null) return;

                    eventsContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No events yet.");
                        empty.setTextColor(getResources().getColor(R.color.white));
                        empty.setAlpha(0.7f);
                        empty.setPadding(dp(8), dp(16), dp(8), 0);
                        eventsContainer.addView(empty);
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event == null) continue;

                        View card = getLayoutInflater()
                                .inflate(R.layout.item_event_card, eventsContainer, false);

                        ImageView img     = card.findViewById(R.id.eventImage);
                        TextView title    = card.findViewById(R.id.eventTitle);
                        TextView details  = card.findViewById(R.id.eventDetails);
                        Button joinButton = card.findViewById(R.id.eventButton);

                        title.setText(event.getName());
                        details.setText(event.getFormattedDateAndLocation());

                        if (event.getPosterImageUrl() != null && !event.getPosterImageUrl().isEmpty()) {
                            Glide.with(MainActivity.this)
                                    .load(event.getPosterImageUrl())
                                    .into(img);
                        } else {
                            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            img.setImageResource(R.drawable.ic_image_icon);
                        }

                        joinButton.setOnClickListener(v -> {
                            String eventId = doc.getId();
                            openWaitingRoom(eventId);
                        });

                        eventsContainer.addView(card);
                    }
                });
    }
}