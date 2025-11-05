package com.example.yellow;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.yellow.ui.ProfileUserFragment;
import com.example.yellow.profile.ProfileAdminFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_host);

        // Toolbar back
        MaterialToolbar toolbar = findViewById(R.id.profileToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Decide which profile to show
        String role = getIntent().getStringExtra("role");
        Fragment fragment = ("admin".equalsIgnoreCase(role))
                ? new ProfileAdminFragment()
                : new ProfileUserFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.profileContainer, fragment)
                .commit();

        // Bottom nav (simple wiring; tailor as needed)
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        for (int i = 0; i < bottomNav.getMenu().size(); i++) {
            bottomNav.getMenu().getItem(i).setChecked(false);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish(); // go back to MainActivity
                return true;
            } else if (id == R.id.nav_history) {
                // TODO
                return true;
            } else if (id == R.id.nav_create_event) {
                // TODO
                return true;
            } else if (id == R.id.nav_my_events) {
                // TODO
                return true;
            } else if (id == R.id.nav_scan) {
                // TODO
                return true;
            }
            return false;
        });
    }
}