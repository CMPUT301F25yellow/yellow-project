package com.example.yellow.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
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

    // 🔹 Make db + adapter fields so we can reuse them (e.g. in clearAllNotifications)
    private FirebaseFirestore db;
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

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
        adapter = new NotificationAdapter();
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

        // ---- Listen for notifications in real time ----
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
                                // If you store notificationId, set it here:
                                item.setNotificationId(doc.getId());
                                list.add(item);
                            }
                        }
                        adapter.setList(list);
                    });
        }

        // ---- Clear all notifications button ----
        View btnClearAll = v.findViewById(R.id.btnClearAll);
        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(view ->
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Clear all notifications?")
                            .setMessage("This will permanently delete all notifications.")
                            .setPositiveButton("Clear", (dialog, which) -> clearAllNotifications())
                            .setNegativeButton("Cancel", null)
                            .show()
            );
        }
    }

    private void acceptSelection(String eventId, String notificationId) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference cancelledRef = eventRef.collection("cancelled").document(uid);

        // First check: user is not cancelled already
        cancelledRef.get().addOnSuccessListener(cancelDoc -> {

            if (cancelDoc.exists()) {
                Toast.makeText(getContext(),
                        "You cannot enroll because you previously cancelled.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Now check capacity before running the transaction
            eventRef.get().addOnSuccessListener(eventDoc -> {
                Long enrolled = eventDoc.getLong("enrolled");
                Long max = eventDoc.getLong("maxEntrants");

                if (enrolled == null) enrolled = 0L;
                if (max == null || max <= 0) max = Long.MAX_VALUE;  // Unlimited capacity

                if (enrolled >= max) {
                    Toast.makeText(getContext(), "Event is full!", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.runTransaction(transaction -> {

                            DocumentSnapshot freshDoc = transaction.get(eventRef);
                            Long tEnrolled = freshDoc.getLong("enrolled");
                            Long tMax = freshDoc.getLong("maxEntrants");

                            if (tEnrolled == null) tEnrolled = 0L;
                            if (tMax == null || tMax <= 0) tMax = Long.MAX_VALUE;

                            if (tEnrolled >= tMax) {
                                throw new FirebaseFirestoreException(
                                        "Event is full",
                                        FirebaseFirestoreException.Code.ABORTED
                                );
                            }

                            // Remove from selected
                            transaction.delete(eventRef.collection("selected").document(uid));

                            // Add to enrolled
                            transaction.set(
                                    eventRef.collection("enrolled").document(uid),
                                    new HashMap<String, Object>() {{
                                        put("userId", uid);
                                        put("timestamp", FieldValue.serverTimestamp());
                                    }}
                            );

                            // Increment counter
                            transaction.update(eventRef, "enrolled", tEnrolled + 1);

                            // Remove notification
                            DocumentReference notifRef = db.collection("profiles")
                                    .document(uid)
                                    .collection("notifications")
                                    .document(notificationId);
                            transaction.delete(notifRef);

                            return null;

                        }).addOnSuccessListener(v ->
                                Toast.makeText(getContext(), "You’ve successfully enrolled!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Event is full!", Toast.LENGTH_SHORT).show());

            });

        });
    }

    private void declineSelection(String eventId, String notificationId) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference eventRef = db.collection("events").document(eventId);

        getNextWaitlistedUser(eventId, (nextUserId, nextDoc) -> {

            db.runTransaction(transaction -> {

                DocumentReference selectedRef =
                        eventRef.collection("selected").document(uid);

                DocumentReference enrolledRef =
                        eventRef.collection("enrolled").document(uid);

                DocumentReference cancelledRef =
                        eventRef.collection("cancelled").document(uid);

                // ALWAYS delete user from selected
                transaction.delete(selectedRef);

                // delete from enrolled if they were there
                transaction.delete(enrolledRef);

                // Add to cancelled
                Map<String, Object> cancelData = new HashMap<>();
                cancelData.put("userId", uid);
                cancelData.put("timestamp", FieldValue.serverTimestamp());
                transaction.set(cancelledRef, cancelData);

                // Promote next user if any
                if (nextUserId != null && nextDoc != null) {

                    // Remove them from waiting list
                    transaction.delete(nextDoc.getReference());

                    // Add to selected
                    DocumentReference promotedRef =
                            eventRef.collection("selected").document(nextUserId);

                    Map<String, Object> promoteData = new HashMap<>();
                    promoteData.put("userId", nextUserId);
                    promoteData.put("timestamp", FieldValue.serverTimestamp());
                    transaction.set(promotedRef, promoteData);
                }

                return null;

            }).addOnSuccessListener(v -> {

                if (nextUserId != null) {
                    sendRejoinNotification(eventId, nextUserId);
                }

                removeNotification(notificationId);

                Toast.makeText(getContext(),
                        "You have declined the selection.",
                        Toast.LENGTH_SHORT).show();

            }).addOnFailureListener(e -> {

                Toast.makeText(getContext(),
                        "Failed to process decline: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();

            });
        });
    }

    private void sendRejoinNotification(String eventId, String nextUserId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("message", "A spot opened up! Tap to rejoin.");
        data.put("timestamp", FieldValue.serverTimestamp());
        data.put("read", false);

        db.collection("profiles")
                .document(nextUserId)
                .collection("notifications")
                .add(data);
    }

    private void removeNotification(String notificationId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("profiles")
                .document(uid)
                .collection("notifications")
                .document(notificationId)
                .delete();
    }

    private void getNextWaitlistedUser(String eventId, NextUserCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events").document(eventId)
                .collection("waitingList")
                .orderBy("timestamp")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onResult(null, null);
                    } else {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        String userId = doc.getString("userId");
                        callback.onResult(userId, doc);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null, null));
    }

    public interface NextUserCallback {
        void onResult(String nextUserId, DocumentSnapshot docSnapshot);
    }

    // 🔹 Clear all notifications for the current user
    private void clearAllNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("profiles")
                .document(uid)
                .collection("notifications")
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                // Clear list locally for instant UI feedback
                                adapter.setList(new ArrayList<>());
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
