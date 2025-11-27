package com.example.yellow.organizers;

import static android.content.ContentValues.TAG;

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
import com.example.yellow.organizers.fragments.EventPosterUpdateFragment;
import com.example.yellow.ui.ManageEntrants.ManageEntrantsActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.ViewGroup;

public class ViewEventActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TextView tvEventName;
    private TextView tvEventDate;
    private Button btnUpdatePoster;

    private Event currentEvent; // This will hold the loaded event data
    private String eventId;
    private EventViewModel eventViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        // Match the status bar color to the header
        getWindow().setStatusBarColor(
                ContextCompat.getColor(this, R.color.surface_dark));

        // Size the spacer to the exact status bar height for a perfect top band
        View spacer = findViewById(R.id.statusBarSpacer);
        if (spacer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.LayoutParams lp = spacer.getLayoutParams();
                if (lp.height != bars.top) {
                    lp.height = bars.top;
                    spacer.setLayoutParams(lp);
                }
                return insets;
            });
        }

        // --- 1. Bind all UI components ---
        try {
            tabLayout = findViewById(R.id.tabLayout);
            viewPager = findViewById(R.id.viewPager);
            tvEventName = findViewById(R.id.tvEventName);
            tvEventDate = findViewById(R.id.tvEventDate);
            btnUpdatePoster = findViewById(R.id.btnEventSettings);
            ImageView btnBack = findViewById(R.id.btnBack);
            btnBack.setOnClickListener(v -> finish());

            if (tabLayout == null || viewPager == null) {
                throw new IllegalStateException("TabLayout or ViewPager not found. Check activity_view_event.xml IDs.");
            }

            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    // position 1 == Map tab in
                    boolean isMapTab = (position == 1);
                    // Disable swipe when on Map so gestures is easier to use
                    viewPager.setUserInputEnabled(!isMapTab);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to bind views. Check your layout file.", e);
            Toast.makeText(this, "UI Error: Layout is broken.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- 2. Standard setup ---
        setupWindowInsets();
        extractEventId();
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        setupObservers();

        // --- 3. Trigger initial data load ---
        if (eventId != null && !eventId.trim().isEmpty()) {
            eventViewModel.loadEvent(eventId);
        } else {
            Toast.makeText(this, "Error: Event ID is missing.", Toast.LENGTH_LONG).show();
            finish();
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
     * This method is responsible for setting or resetting ALL UI elements that
     * depend on the event.
     */
    private void updateUiWithEvent(Event event) {
        // Set the current event object for fragments to access
        this.currentEvent = event;

        // Allow fragments to auto-refresh the event data any time the event reloads
        Bundle result = new Bundle();
        result.putString("eventId", event.getId());
        getSupportFragmentManager().setFragmentResult("eventLoaded", result);

        // --- Populate Header ---
        tvEventName.setText(event.getName()); // Corrected from event.getName()
        if (event.getStartDate() != null) {
            android.text.format.DateFormat df = new android.text.format.DateFormat();
            tvEventDate.setText(df.format("MMM dd, yyyy", event.getStartDate().toDate()));
        }

        // --- Set up ViewPager Adapter ---
        ViewEventPageAdapter adapter = new ViewEventPageAdapter(this, event.getId());
        viewPager.setAdapter(adapter);

        // This will connect the tabs to the view pager.
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Entrants");
                    break;
                case 1:
                    tab.setText("Map");
                    break;
                case 2:
                    tab.setText("Notify");
                    break;
                case 3:
                    tab.setText("QR Code");
                    break;
            }
        }).attach(); // The .attach() call makes the tabs appear.

        // --- Set up Click Listeners ---
        btnUpdatePoster.setOnClickListener(v -> {
            EventPosterUpdateFragment dialog = EventPosterUpdateFragment.newInstance(event.getId());
            dialog.show(getSupportFragmentManager(), "EventSettingsFragment");
        });

    }

    private void extractEventId() {
        Uri data = getIntent().getData();
        eventId = null;

        if (data != null && "yellow".equalsIgnoreCase(data.getScheme()) && "event".equalsIgnoreCase(data.getHost())
                && data.getPathSegments() != null && !data.getPathSegments().isEmpty()) {
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
     * This public getter allows fragments to safely access the loaded event object.
     * It is guaranteed to be non-null when fragments are created.
     */
    public Event getEvent() {
        return currentEvent;
    }
}
