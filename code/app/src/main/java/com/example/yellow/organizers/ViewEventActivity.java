package com.example.yellow.organizers;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.example.yellow.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ViewEventActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, topInset, 0, 0);
            v.setBackgroundResource(R.color.surface_dark);
            return insets;
        });

// Setup header
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvEventName = findViewById(R.id.tvEventName);
        TextView tvEventDate = findViewById(R.id.tvEventDate);

// Get event info passed from intent
        String eventName = getIntent().getStringExtra("eventName");
        String eventDate = getIntent().getStringExtra("eventDate");

        if (eventName != null) tvEventName.setText(eventName);
        if (eventDate != null) tvEventDate.setText(eventDate);

// Back button action
        btnBack.setOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;

            // Add padding at the top to push content below the camera notch/status bar
            v.setPadding(0, topInset, 0, 0);

            // Optional: make background match your dark theme
            v.setBackgroundResource(R.color.surface_dark);

            return insets;
        });


        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ViewEventPageAdapter adapter = new ViewEventPageAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Entrants"); break;
                case 1: tab.setText("Map"); break;
                case 2: tab.setText("Settings"); break;
                case 3: tab.setText("Notify"); break;
            }
        }).attach();
    }
}
