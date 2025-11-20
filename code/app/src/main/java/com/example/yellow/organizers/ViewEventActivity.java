package com.example.yellow.organizers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.yellow.R;
import com.example.yellow.organizers.fragments.EventSettingsFragment;
import com.example.yellow.ui.ManageEntrants.ManageEntrantsActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewEventActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Event currentEvent; // This will hold the loaded event data
    private String eventId;
    private EventViewModel eventViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        // Set up:
        viewPager = findViewById(R.id.viewPager);
        extractEventId();

        setupWindowInsets();
        setupHeader();

        // ---- Event ID Extraction (No changes needed) ----
        extractEventId();

        // Initialize the ViewModel
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);

        // Set up the observer
        setupObservers();

        // ---- Load Event Data ----
        if (eventId != null && !eventId.trim().isEmpty()) {
            // This method now handles setting up the pager AFTER data is loaded.
            eventViewModel.loadEvent(eventId);
        } else {
            Toast.makeText(this, "Error: Event ID is missing.", Toast.LENGTH_LONG).show();
            finish(); // Exit if no ID is found
        }
    }

    private void setupObservers() {
        // This is the magic. This block will run whenever the event data changes.
        eventViewModel.getEvent().observe(this, event -> {
            if (event == null) {
                // This will be triggered if the event fails to load or is not found.
                Toast.makeText(this, "Failed to load event data.", Toast.LENGTH_LONG).show();
                // Optionally finish();
                return;
            }

            // Data is ready, set up the entire UI
            updateUiWithEvent(event);
        });
    }

    /**
     * This method is responsible for setting or resetting ALL UI elements that depend on the event.
     */
    private void updateUiWithEvent(Event event) {
        // --- Populate Header ---
        TextView tvEventName = findViewById(R.id.tvEventName);
        TextView tvEventDate = findViewById(R.id.tvEventDate);
        tvEventName.setText(event.getName());
        // You can format the date here if you want

        // --- Set up ViewPager Adapter ---
        // This re-configures the pager system, which is safe.
        ViewEventPageAdapter adapter = new ViewEventPageAdapter(this, event.getId());
        viewPager.setAdapter(adapter);
        // ... new TabLayoutMediator(tabLayout, viewPager, ...).attach(); ...

        // --- Set up Settings Button ---
        ImageView btnSettings = findViewById(R.id.btnEventSettings);
        btnSettings.setOnClickListener(v -> {
            EventSettingsFragment dialog = EventSettingsFragment.newInstance(event.getId());
            dialog.show(getSupportFragmentManager(), "EventSettingsFragment");
        });

        // --- Set up Manage Entrants Button ---
        Button manageBtn = findViewById(R.id.btnManageEntrants);
        manageBtn.setOnClickListener(v -> {
            Intent i = new Intent(ViewEventActivity.this, ManageEntrantsActivity.class);
            i.putExtra("eventId", event.getId());
            i.putExtra("eventName", event.getName());
            startActivity(i);
        });
    }
    private void extractEventId() {
        Uri data = getIntent().getData();
        eventId = null;

        if (data != null && "yellow".equalsIgnoreCase(data.getScheme()) && "event".equalsIgnoreCase(data.getHost()) && data.getPathSegments() != null && !data.getPathSegments().isEmpty()) {
            eventId = data.getPathSegments().get(data.getPathSegments().size() - 1);
        }

        if (eventId == null) {
            eventId = getIntent().getStringExtra("eventId");
        }

        Log.d("ViewEventActivity", "Final Event ID: " + eventId);
    }

    private void setupHeader() {
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvEventName = findViewById(R.id.tvEventName);
        TextView tvEventDate = findViewById(R.id.tvEventDate);

        btnBack.setOnClickListener(v -> finish());

        // We will populate these from the loaded event object later
    }

    private void setupWindowInsets() {
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });
    }

    /**
     *
     * @param eventIdToLoad
     */
    private void loadEventAndSetupPager(String eventIdToLoad) {
        FirebaseFirestore.getInstance().collection("events").document(eventIdToLoad).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    // Store the loaded event object. This is now guaranteed to be non-null for fragments.
                    currentEvent = snap.toObject(Event.class);

                    if (currentEvent == null) {
                        Toast.makeText(this, "Error: Malformed event data.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    // Populate the header with the loaded data
                    TextView tvEventName = findViewById(R.id.tvEventName);
                    TextView tvEventDate = findViewById(R.id.tvEventDate);
                    tvEventName.setText(currentEvent.getName());

                    ImageView btnSettings = findViewById(R.id.btnEventSettings);
                    btnSettings.setOnClickListener(v -> {
                        // Launch the dialog fragment
                        EventSettingsFragment settingsFragment = EventSettingsFragment.newInstance(eventIdToLoad);
                        settingsFragment.show(getSupportFragmentManager(), "EventSettingFragment");
                    });

                    if (currentEvent.getStartDate() != null) {
                        // Simple date formatting, you can make this more complex
                        android.text.format.DateFormat df = new android.text.format.DateFormat();
                        tvEventDate.setText(df.format("MMM dd, yyyy", currentEvent.getStartDate().toDate()));
                    }


                    // Set up the TabLayout and ViewPager
                    tabLayout = findViewById(R.id.tabLayout);
                    viewPager = findViewById(R.id.viewPager);

                    ViewEventPageAdapter adapter = new ViewEventPageAdapter(this, eventIdToLoad);
                    viewPager.setAdapter(adapter);

                    new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                        switch (position) {
                            case 0: tab.setText("Entrants"); break;
                            case 1: tab.setText("Map"); break;
                            case 2: tab.setText("Notify"); break;
                            case 3: tab.setText("QR Code"); break;
                        }
                    }).attach();

                    // Set up the Manage Entrants button
                    Button manageBtn = findViewById(R.id.btnManageEntrants);
                    manageBtn.setOnClickListener(v -> {
                        Intent i = new Intent(ViewEventActivity.this, ManageEntrantsActivity.class);
                        i.putExtra("eventId", eventIdToLoad);
                        i.putExtra("eventName", currentEvent.getName());
                        startActivity(i);
                    });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("ViewEventActivity", "Firestore load failed", e);
                    finish();
                });
    }

    /**
     * This public getter allows fragments to safely access the loaded event object.
     * It is guaranteed to be non-null when fragments are created.
     */
    public Event getEvent() {
        return currentEvent;
    }
}
