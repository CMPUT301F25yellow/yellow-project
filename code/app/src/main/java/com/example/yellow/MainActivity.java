package com.example.yellow;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.yellow.ui.EventDetailsFragment;
import com.example.yellow.ui.HistoryFragment;
import com.example.yellow.ui.MyEventsFragment;
import com.example.yellow.ui.NotificationFragment;
import com.example.yellow.ui.ProfileUserFragment;
import com.example.yellow.ui.QrScanFragment;
import com.example.yellow.users.WaitingListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
//author: waylon
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private View header;
    private View scrollContent;
    private View fragmentContainer;

    private ListenerRegistration eventsListener;
    private List<Event> allEvents = new ArrayList<>();
    private LinearLayout eventsContainer;
    private String selectedDate = null;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        eventsContainer = findViewById(R.id.eventsContainer);

        // ---- Views ----
        View root = findViewById(R.id.main);
        header = findViewById(R.id.header_main);
        scrollContent = findViewById(R.id.scrollContent);
        bottomNav = findViewById(R.id.bottomNavigationView);
        fragmentContainer = findViewById(R.id.fragmentContainer);

        // ---- Header icons ----
        View iconProfile = findViewById(R.id.iconProfile);
        if (iconProfile != null)
            iconProfile.setOnClickListener(v -> openProfile());

        View iconNotifications = findViewById(R.id.iconNotifications);
        if (iconNotifications != null)
            iconNotifications.setOnClickListener(v -> openNotifications());

        View notificationDot = findViewById(R.id.notificationDot);

        // ---- Safe-area insets ----
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (header != null) {
                header.setPadding(
                        header.getPaddingLeft(),
                        bars.top + dp(12),
                        header.getPaddingRight(),
                        header.getPaddingBottom());
            }
            if (scrollContent != null) {
                scrollContent.setPadding(
                        scrollContent.getPaddingLeft(),
                        scrollContent.getPaddingTop(),
                        scrollContent.getPaddingRight(),
                        bars.bottom + dp(16));
            }
            if (bottomNav != null) {
                bottomNav.setPadding(
                        bottomNav.getPaddingLeft(),
                        bottomNav.getPaddingTop(),
                        bottomNav.getPaddingRight(),
                        0);
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
                    com.example.yellow.utils.ProfileUtils.checkProfile(this, isComplete -> {
                        if (isComplete) {
                            startActivity(new Intent(MainActivity.this, CreateEventActivity.class));
                        }
                    }, () -> openProfile());
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

        // ---- Notification Dot Listener (Dynamic) ----
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            String uid = firebaseAuth.getUid();
            if (uid != null && notificationDot != null) {
                // Remove existing listener if any to avoid duplicates
                if (notificationListener != null) {
                    notificationListener.remove();
                }

                notificationListener = FirebaseFirestore.getInstance()
                        .collection("profiles")
                        .document(uid)
                        .collection("notifications")
                        .addSnapshotListener((snapshot, e) -> {
                            if (snapshot == null)
                                return;

                            boolean hasUnread = false;
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                Boolean read = doc.getBoolean("read");
                                if (read == null || !read) {
                                    hasUnread = true;
                                    break;
                                }
                            }
                            notificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                        });
            } else {
                // User logged out
                if (notificationListener != null) {
                    notificationListener.remove();
                    notificationListener = null;
                }
                if (notificationDot != null) {
                    notificationDot.setVisibility(View.GONE);
                }
            }
        });

        // ---- Restore Home UI when back stack empties ----
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
            if (backStackCount == 0) {
                showHomeUI(true);
                if (bottomNav != null) {
                    bottomNav.getMenu().setGroupCheckable(0, true, true);
                    bottomNav.setSelectedItemId(R.id.nav_home);
                }
            } else {
                // Used to fix bug where QR Scan had no bottomNav
                String topFragmentTag = getSupportFragmentManager().getBackStackEntryAt(backStackCount - 1).getName();

                if ("QR_SCAN".equals(topFragmentTag) || "MyEvents".equals(topFragmentTag) || "History".equals(topFragmentTag)) {
                    if (bottomNav != null) {
                        bottomNav.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        // ---- Modern back handling ----
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });

        // search bar
        EditText searchBar = findViewById(R.id.searchBar);
        searchBar.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // filter by dates
        Button btnPickDate = findViewById(R.id.btnPickDate);

        btnPickDate.setOnClickListener(v -> {

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Event Date")
                    .build();

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");

            datePicker.addOnPositiveButtonClickListener(selection -> {
                // Convert timestamp → formatted date string
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                selectedDate = sdf.format(new Date(selection));
                btnPickDate.setText(selectedDate); // shows selected date

                // Re-filter with the new date
                filterEvents(searchBar.getText().toString());
            });
        });

        // clearing filters
        Button btnClearFilters = findViewById(R.id.btnClearFilters);

        btnClearFilters.setOnClickListener(v -> {
            selectedDate = null; // Removes the date filter
            searchBar.setText(""); // Clears the search bar text
            btnPickDate.setText("Availability");

            renderEvents(allEvents); // Draws all events again
        });

        boolean deepLinkHandled = handleDeepLink(getIntent());

        if (!deepLinkHandled) {
            startLiveEventsListener();
        }

        // [NEW] Register Device for Admin Identity
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user != null) {
            com.example.yellow.utils.DeviceIdentityManager.ensureDeviceDocument(this, user);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleDeepLink(intent);
    }

    // Handles deep link intents
    private boolean handleDeepLink(Intent intent) {
        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            if ("yellow".equals(data.getScheme()) && "eventdetails".equals(data.getHost())) {
                String eventId = data.getLastPathSegment();
                if (eventId != null) {
                    openEventDetails(eventId);
                    return true;
                }
            }
        }
        return false;
    }

    // Open Event Details (Fragment) from QR code with eventId
    private void openEventDetails(String eventId) {
        Bundle bundle = new Bundle();
        bundle.putString("qr_code_data", eventId);

        EventDetailsFragment eventDetailsFragment = new EventDetailsFragment();
        eventDetailsFragment.setArguments(bundle);

        openFragment(eventDetailsFragment, "EventDetails", false);
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
        if (header != null)
            header.setVisibility(View.GONE);
        if (scrollContent != null)
            scrollContent.setVisibility(View.GONE);

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
        if (header != null)
            header.setVisibility(visible);
        if (scrollContent != null)
            scrollContent.setVisibility(visible);
        if (fragmentContainer != null)
            fragmentContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        if (bottomNav != null)
            bottomNav.setVisibility(visible);
    }

    // ---------- Screens ----------

    public void openProfile() {
        if (bottomNav != null)
            bottomNav.getMenu().setGroupCheckable(0, false, true);
        openFragment(new ProfileUserFragment(), "Profile", /* keepBottomNavVisible= */false);
    }

    private void openNotifications() {
        openFragment(new NotificationFragment(), "Notifications", /* keepBottomNavVisible= */false);
    }

    private void openQrScan() {
        openFragment(new QrScanFragment(), "QR_SCAN", /* keepBottomNavVisible= */true);
    }

    private void openHistory() {
        openFragment(new HistoryFragment(), "History", /* keepBottomNavVisible= */true);
    }

    private void openMyEvents() {
        openFragment(new MyEventsFragment(), "MyEvents", /* keepBottomNavVisible= */true);
    }

    public void openWaitingRoom(String eventId) {
        WaitingListFragment fragment = new WaitingListFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        openFragment(fragment, "WAITING_ROOM", /* keepBottomNavVisible= */false);
    }

    // ---------- Live Events (auto-updating) ----------

    private void startLiveEventsListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

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
                    if (querySnapshot == null || eventsContainer == null)
                        return;

                    eventsContainer.removeAllViews();
                    allEvents.clear();

                    if (querySnapshot.isEmpty()) {
                        renderEvents(allEvents);
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event == null)
                            continue;

                        // Store event ID for the join button
                        event.setId(doc.getId());

                        allEvents.add(event);
                    }

                    // Draw all events normally (no filter yet)
                    renderEvents(allEvents);
                });
    }

    private void renderEvents(List<Event> list) {
        eventsContainer.removeAllViews();

        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No events match your search.");
            empty.setTextColor(getResources().getColor(R.color.white));
            empty.setAlpha(0.7f);
            empty.setPadding(dp(8), dp(16), dp(8), 0);
            eventsContainer.addView(empty);
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (Event event : list) {
            View card = getLayoutInflater()
                    .inflate(R.layout.item_event_card, eventsContainer, false);

            ImageView img = card.findViewById(R.id.eventImage);
            TextView title = card.findViewById(R.id.eventTitle);
            TextView details = card.findViewById(R.id.eventDetails);
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

            // Check if user is already in waiting list
            if (uid != null) {
                db.collection("events")
                        .document(event.getId())
                        .collection("waitingList")
                        .document(uid)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                joinButton.setText("View Waiting List");
                            } else {
                                joinButton.setText("Join Waiting List");
                            }
                        });
            } else {
                joinButton.setText("Join Waiting List");
            }

            joinButton.setOnClickListener(v -> {
                openWaitingRoom(event.getId());
            });

            eventsContainer.addView(card);
        }
    }

    private void filterEvents(String keyword) {
        if (keyword == null) keyword = "";
        keyword = keyword.toLowerCase().trim();

        List<Event> filtered = new ArrayList<>();

        for (Event e : allEvents) {

            // Safe values for null fields
            String name = (e.getName() != null) ? e.getName().toLowerCase() : "";
            String desc = (e.getDescription() != null) ? e.getDescription().toLowerCase() : "";

            boolean matchesKeyword =
                    keyword.isEmpty() ||
                            name.contains(keyword) ||
                            desc.contains(keyword);

            String eventDate = e.getFormattedDateAndLocation();
            if (eventDate == null) eventDate = "";

            boolean matchesDate =
                    (selectedDate == null) ||
                            eventDate.contains(selectedDate);

            if (matchesKeyword && matchesDate) {
                filtered.add(e);
            }
        }

        renderEvents(filtered);
    }
}
