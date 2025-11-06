package com.example.yellow.users;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.yellow.MainActivity;
import com.example.yellow.R;
import com.example.yellow.organizers.CreateEventActivity;

public class WaitingListFragment extends Fragment {

    public WaitingListFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_waiting_room, container, false);

        View leaveButton = view.findViewById(R.id.leaveButton);

        leaveButton.setOnClickListener(v -> {

            // Tell MainActivity to restore UI
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).restoreHomeUI();
            }

            // Pop fragment
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }
}

