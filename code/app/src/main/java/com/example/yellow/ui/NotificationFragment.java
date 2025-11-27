package com.example.yellow.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.yellow.R;
import com.example.yellow.models.NotificationItem;
import com.example.yellow.ui.notifications.NotificationAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment that displays app notifications for the user.
 */
public class NotificationFragment extends Fragment {

    // 🔹 Make Firestore + adapter fields so other methods can use them
    private FirebaseFirestore db;
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.
     * @param v The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Init Firestore once
        db = FirebaseFirestore.getInstance();

        // Make status bar the same color as header
        requireActivity().getWindow().setStatusBarColor(
                ContextCompat.getColor(requireContext(), R.color.surface_dark));

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
                    requireActivity().getSupportFragmentManager().popBackStack());
        }

        // RecyclerView + adapter
        RecyclerView rv = v.findViewById(R.id.rvNotifications);
        adapter = new NotificationAdapter(); // field
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        adapter.setActionListener(new NotificationAdapter.ActionListener() {
            @Override
            public void onAccept(String eventId, String notificationId) {
                acceptSelection(eventId, notificationId);
            }

            @Override
            public void onDecline(String eventId, String notificationId) {
                declineSelection(eventId, notificationId);
            }
        });

        // ---- Mark all unread notifications as read when opening this screen ----
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("profiles")
                    .document(uid)
                    .collection("notifications")
                    .whereEqualTo("read", false)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : snapshot) {
                            batch.update(doc.getReference(), "read", true);
                        }
                        batch.commit();
                    });
        }

        // ---- Listen to notifications in real time ----
        if (uid != null) {
            db.collection("profiles")
                    .document(uid)
                    .collection("notifications")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener((value, error) -> {
                        if (error != null || value == null) return;

                        List<NotificationItem> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            NotificationItem item = doc.toObject(NotificationItem.class);
                            if (item != null) {
                                // Ensure notificationId is set (for accept/decline/delete)
                                item.setNotificationId(doc.getId());
                                list.add(item);
                            }
                        }
                        adapter.setList(list);
                    });
        }

        // ---- Clear all notifications button at the bottom ----
        View clearAllView = v.findViewById(R.id.btnClearAll);
        if (clearAllView != null) {
            clearAllView.setOnClickListener(view -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Clear all notifications?")
                        .setMessage("This will permanently delete all notifications.")
                        .setPositiveButton("Clear", (dialog, which) -> clearAllNotifications())
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    /**
     * Accepts a selection.
     * @param eventId
     * @param notificationId
     */
    private void acceptSelection(String eventId, String notificationId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // We already have db as a field
        getUserStatus(eventId, uid, status -> {

            if (status.equals("cancelled")) {
                Toast.makeText(getContext(),
                        "You have cancelled this event. You cannot rejoin.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (!status.equals("selected")) {
                Toast.makeText(getContext(),
                        "You are not eligible to sign up for this event.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Build references
            DocumentReference selectedRef = db.collection("events")
                    .document(eventId)
                    .collection("selected")
                    .document(uid);

            DocumentReference enrolledRef = db.collection("events")
                    .document(eventId)
                    .collection("enrolled")
                    .document(uid);

            DocumentReference notifRef = db.collection("profiles")
                    .document(uid)
                    .collection("notifications")
                    .document(notificationId);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", uid);
            data.put("timestamp", FieldValue.serverTimestamp());

            // Atomic move
            WriteBatch batch = db.batch();
            batch.delete(selectedRef);   // remove from selected
            batch.set(enrolledRef, data); // add to enrolled
            batch.delete(notifRef);      // remove notification

            batch.commit()
                    .addOnSuccessListener(unused -> Toast.makeText(getContext(),
                            "You are now enrolled in this event!",
                            Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(),
                            "Failed to enroll: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
        });
    }

    /**
     * Declines a selection.
     * @param eventId
     * @param notificationId
     */
    private void declineSelection(String eventId, String notificationId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        getUserStatus(eventId, uid, status -> {

            // We only allow moving:
            // selected → cancelled
            // enrolled → cancelled
            if (!status.equals("selected") && !status.equals("enrolled")) {
                Toast.makeText(getContext(),
                        "You cannot decline — current status: " + status,
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Build references for atomic batch
            DocumentReference oldRef = db.collection("events")
                    .document(eventId)
                    .collection(status) // either selected OR enrolled
                    .document(uid);

            DocumentReference cancelledRef = db.collection("events")
                    .document(eventId)
                    .collection("cancelled")
                    .document(uid);

            DocumentReference notifRef = db.collection("profiles")
                    .document(uid)
                    .collection("notifications")
                    .document(notificationId);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", uid);
            data.put("timestamp", FieldValue.serverTimestamp());

            // Atomic move
            WriteBatch batch = db.batch();
            batch.delete(oldRef);         // remove from selected OR enrolled
            batch.set(cancelledRef, data); // add to cancelled
            batch.delete(notifRef);       // remove notification

            batch.commit()
                    .addOnSuccessListener(unused -> Toast.makeText(getContext(),
                            "You have cancelled your participation.",
                            Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(),
                            "Failed to cancel: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
        });
    }

    /**
     * Gets the user's current status in the event.
     * @param eventId
     * @param uid
     * @param callback
     */
    private void getUserStatus(String eventId, String uid, StatusCallback callback) {
        // Check selected
        db.collection("events").document(eventId)
                .collection("selected").document(uid).get()
                .addOnSuccessListener(selectedDoc -> {
                    if (selectedDoc.exists()) {
                        callback.onStatus("selected");
                    } else {
                        // Check enrolled
                        db.collection("events").document(eventId)
                                .collection("enrolled").document(uid).get()
                                .addOnSuccessListener(enrolledDoc -> {
                                    if (enrolledDoc.exists()) {
                                        callback.onStatus("enrolled");
                                    } else {
                                        // Check cancelled
                                        db.collection("events").document(eventId)
                                                .collection("cancelled").document(uid).get()
                                                .addOnSuccessListener(cancelledDoc -> {
                                                    if (cancelledDoc.exists()) {
                                                        callback.onStatus("cancelled");
                                                    } else {
                                                        callback.onStatus("none");
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    interface StatusCallback {
        void onStatus(String status);
    }

    /**
     * Clears all notifications.
     */
    private void clearAllNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("profiles")
                .document(user.getUid())
                .collection("notifications")
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                // adapter will also be updated by the snapshot listener,
                                // but clearing immediately gives instant feedback
                                adapter.clear();
                                Toast.makeText(getContext(),
                                        "Notifications cleared.",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Failed to clear notifications: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                });
    }
}
