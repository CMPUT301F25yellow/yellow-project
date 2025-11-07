package com.example.yellow.ui.ManageEntrants;

import android.os.Bundle;
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

public class ManageEntrantsActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_entrants);

        // Handle top insets (for camera notch)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        // Header setup
        ImageView backBtn = findViewById(R.id.btnBack);
        TextView title = findViewById(R.id.tvEventTitle);
        backBtn.setOnClickListener(v -> finish());

        // Get event info
        String eventId = getIntent().getStringExtra("eventId");
        String eventName = getIntent().getStringExtra("eventName");
        if (eventName != null) title.setText(eventName);

        // Tabs setup
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ManageEntrantsPageAdapter adapter = new ManageEntrantsPageAdapter(this, eventId);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Waiting"); break;
                case 1: tab.setText("Selected"); break;
                case 2: tab.setText("Cancelled"); break;
                case 3: tab.setText("Enrolled"); break;
            }
        }).attach();
    }
}
