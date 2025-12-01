package com.example.yellow.ui.ManageEntrants;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter for managing entrants of an event.
 * @author Waylon Wang - waylon1
 */
public class ManageEntrantsPageAdapter extends FragmentStateAdapter {

    private final String eventId;

    /**
     * Constructor for the adapter.
     * @param fa
     * @param eventId
     */
    public ManageEntrantsPageAdapter(@NonNull FragmentActivity fa, String eventId) {
        super(fa);
        this.eventId = eventId;
    }

    /**
     * Creates a fragment for a given position.
     * @param position: the position of the fragment
     * @return the fragment
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0: fragment = new WaitingFragment(); break;
            case 1: fragment = new SelectedFragment(); break;
            case 2: fragment = new CancelledFragment(); break;
            case 3: fragment = new EnrolledFragment(); break;
            default: fragment = new WaitingFragment(); break;
        }

        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Returns the number of tabs.
     * @return the number of tabs
     */
    @Override
    public int getItemCount() {
        return 4;
    }
}