package com.example.yellow.ui.ManageEntrants;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaitingFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout container;
    private String eventId;
    private List<String> currentWaitingEntrants = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_waiting_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        container = view.findViewById(R.id.waitingContainer);
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        Button drawButton = view.findViewById(R.id.btnDraw);

        if (eventId == null) {
            Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        loadWaitingEntrants();

        drawButton.setOnClickListener(v -> showDrawDialog());
    }

    /** Loads all waiting entrants and displays them */
    private void loadWaitingEntrants() {
        container.removeAllViews();
        currentWaitingEntrants.clear();

        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No waiting entrants.");
                        empty.setTextColor(getResources().getColor(R.color.hinty));
                        container.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String name = doc.getString("userName");
                        if (name == null) name = doc.getId();
                        currentWaitingEntrants.add(doc.getId());

                        TextView tv = new TextView(getContext());
                        tv.setText(name);
                        tv.setTextColor(getResources().getColor(R.color.white));
                        tv.setPadding(0, 8, 0, 8);
                        container.addView(tv);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /** Opens dialog to ask how many users to draw */
    private void showDrawDialog() {
        if (currentWaitingEntrants.isEmpty()) {
            Toast.makeText(getContext(), "No entrants to draw from.", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter number to draw");
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});

        new AlertDialog.Builder(getContext())
                .setTitle("Run Draw")
                .setMessage("Enter how many entrants to select (max: " + currentWaitingEntrants.size() + ")")
                .setView(input)
                .setPositiveButton("Draw", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter a number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int drawCount = Integer.parseInt(value);
                    if (drawCount <= 0) {
                        Toast.makeText(getContext(), "Number must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (drawCount > currentWaitingEntrants.size()) {
                        Toast.makeText(getContext(), "Cannot draw more than " + currentWaitingEntrants.size(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    runDraw(drawCount);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /** Randomly select users from the waiting list and move them to selected list */
    private void runDraw(int count) {
        if (eventId == null) return;

        List<String> entrantsCopy = new ArrayList<>(currentWaitingEntrants);
        Collections.shuffle(entrantsCopy);
        List<String> selected = entrantsCopy.subList(0, count);

        for (String id : selected) {
            Map<String, Object> data = new HashMap<>();
            data.put("selected", true);
            data.put("timestamp", System.currentTimeMillis());

            db.collection("events").document(eventId)
                    .collection("selected")
                    .document(id)
                    .set(data);
        }

        Toast.makeText(getContext(),
                "Selected " + selected.size() + " entrants!",
                Toast.LENGTH_SHORT).show();
    }
}