package com.example.yellow.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.yellow.models.NotificationItem;
import com.example.yellow.ui.notifications.NotificationAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import com.example.yellow.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Fragment that displays app notifications for the user.
 */
public class NotificationFragment extends Fragment {

    /**
     * Inflates the notification layout.
     *
     * @param inflater           LayoutInflater used to inflate the view.
     * @param container          Optional parent container.
     * @param savedInstanceState Previously saved state, if any.
     * @return The root view for this fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    /**
     * Sets up the UI, status bar color, and navigation behavior.
     *
     * @param v                  The root view of the fragment.
     * @param savedInstanceState Previously saved state, if any.
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Make status bar the same color as header
        requireActivity().getWindow().setStatusBarColor(
                ContextCompat.getColor(requireContext(), R.color.surface_dark)
        );

        // Size the spacer to the real status bar height
        View spacer = v.findViewById(R.id.statusBarSpacer);
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.LayoutParams lp = spacer.getLayoutParams();
            if (lp.height != bars.top) {
                lp.height = bars.top;
                spacer.setLayoutParams(lp);
            }
            return insets;
        });

        // Back -> pop to Home (MainActivity shows Home when stack empties)
        View back = v.findViewById(R.id.btnBack);
        if (back != null) {
            back.setOnClickListener(x ->
                    requireActivity().getSupportFragmentManager().popBackStack()
            );
        }

        // (Optional) set adapter later
        RecyclerView rv = v.findViewById(R.id.rvNotifications);
        NotificationAdapter adapter = new NotificationAdapter();
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter.setActionListener(new NotificationAdapter.ActionListener() {
            @Override
            public void onAccept(String eventId) {
                acceptSelection(eventId);
            }

            @Override
            public void onDecline(String eventId) {
                declineSelection(eventId);
            }
        });

        String uid = FirebaseAuth.getInstance().getUid();
        if(uid ==null)return;

        FirebaseFirestore.getInstance()
                .collection("profiles")
                .document(uid)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<NotificationItem> list = new ArrayList<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        NotificationItem item = doc.toObject(NotificationItem.class);
                        list.add(item);
                    }

                    adapter.setList(list);
                });

    }
    private void acceptSelection(String eventId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Remove user from selected
        db.collection("events").document(eventId)
                .collection("selected").document(uid)
                .delete();

        // Add to enrolled
        db.collection("events").document(eventId)
                .collection("enrolled").document(uid)
                .set(new HashMap<String, Object>() {{
                    put("userId", uid);
                    put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                }});
    }

    private void declineSelection(String eventId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Remove user from selected
        db.collection("events").document(eventId)
                .collection("selected").document(uid)
                .delete();

        // Add to cancelled
        db.collection("events").document(eventId)
                .collection("cancelled").document(uid)
                .set(new HashMap<String, Object>() {{
                    put("userId", uid);
                    put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                }});
    }
}