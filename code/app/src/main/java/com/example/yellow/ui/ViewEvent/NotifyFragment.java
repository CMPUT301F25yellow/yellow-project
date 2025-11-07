package com.example.yellow.ui.ViewEvent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;

public class NotifyFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_notify, container, false);

        Button btnNotifySelected = v.findViewById(R.id.btnNotifySelected);
        Button btnNotifyNotSelected = v.findViewById(R.id.btnNotifyNotSelected);

        btnNotifySelected.setOnClickListener(view ->
                Toast.makeText(getContext(), "Notified selected entrants!", Toast.LENGTH_SHORT).show());

        btnNotifyNotSelected.setOnClickListener(view ->
                Toast.makeText(getContext(), "Notified unselected entrants!", Toast.LENGTH_SHORT).show());

        return v;
    }
}
