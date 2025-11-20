package com.example.yellow.organizers;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.yellow.ui.QrFragmentAfterCreateEvent;
import com.example.yellow.ui.ViewEvent.EntrantsFragment;
import com.example.yellow.ui.ViewEvent.MapFragment;
import com.example.yellow.ui.ViewEvent.SettingsFragment;
import com.example.yellow.ui.ViewEvent.NotifyFragment;

public class ViewEventPageAdapter extends FragmentStateAdapter {

    private final String eventId;

    public ViewEventPageAdapter(@NonNull FragmentActivity fa, String eventId) {
        super(fa);
        this.eventId = eventId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                EntrantsFragment entrantsFragment = new EntrantsFragment();
                Bundle args = new Bundle();
                args.putString("eventId", eventId);
                entrantsFragment.setArguments(args);
                return entrantsFragment;
            case 1: return new MapFragment();
            case 2: return new NotifyFragment();
            case 3: return new QrFragmentAfterCreateEvent();
            default: return new EntrantsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
