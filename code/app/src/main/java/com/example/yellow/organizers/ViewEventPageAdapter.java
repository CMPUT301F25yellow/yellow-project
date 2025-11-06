package com.example.yellow.organizers;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.yellow.ui.ViewEvent.EntrantsFragment;
import com.example.yellow.ui.ViewEvent.MapFragment;
import com.example.yellow.ui.ViewEvent.SettingsFragment;
import com.example.yellow.ui.ViewEvent.NotifyFragment;

public class ViewEventPageAdapter extends FragmentStateAdapter {

    public ViewEventPageAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new EntrantsFragment();
            case 1: return new MapFragment();
            case 2: return new SettingsFragment();
            case 3: return new NotifyFragment();
            default: return new EntrantsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
