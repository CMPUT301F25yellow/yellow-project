package com.example.yellow.admin;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Date;

/**
 * Admin screen to browse all notification logs.
 */
public class ManageNotificationLogFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout listContainer;
    private View spacer, scroll;
    private ListenerRegistration reg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_notificationlog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        spacer = v.findViewById(R.id.statusBarSpacer);
        scroll = v.findViewById(R.id.scroll);
        listContainer = v.findViewById(R.id.listContainer);

        // Status Bar Color
        requireActivity().getWindow().setStatusBarColor(
                ContextCompat.getColor(requireContext(), R.color.surface_dark));

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (spacer != null) {
                ViewGroup.LayoutParams lp = spacer.getLayoutParams();
                if (lp != null && lp.height != bars.top) {
                    lp.height = bars.top;
                    spacer.setLayoutParams(lp);
                }
            }
            if (scroll != null) {
                scroll.setPaddingRelative(
                        scroll.getPaddingStart(),
                        scroll.getPaddingTop(),
                        scroll.getPaddingEnd(),
                        scroll.getPaddingBottom() + bars.bottom);
            }
            return insets;
        });

        // Back button
        View btnBack = v.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(x -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        db = FirebaseFirestore.getInstance();
        listenForLogs();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) {
            reg.remove();
            reg = null;
        }
        spacer = null;
        scroll = null;
        listContainer = null;
    }

    private void listenForLogs() {
        reg = db.collection("notification_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (!isAdded())
                        return;
                    if (err != null || snap == null) {
                        Toast.makeText(getContext(), "Failed to load logs.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (listContainer == null)
                        return;

                    listContainer.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String eventName = d.getString("eventName");
                        String message = d.getString("message");
                        Long recipientCount = d.getLong("recipientCount");
                        Timestamp timestamp = d.getTimestamp("timestamp");

                        View card = inflater.inflate(R.layout.manage_notification_log_card_admin, listContainer, false);

                        TextView tvEventName = card.findViewById(R.id.eventName);
                        TextView tvTimestamp = card.findViewById(R.id.timestamp);
                        TextView tvMessage = card.findViewById(R.id.message);
                        TextView tvRecipientCount = card.findViewById(R.id.recipientCount);
                        TextView tvOrganizerName = card.findViewById(R.id.organizerName);
                        View btnViewRecipients = card.findViewById(R.id.btnViewRecipients);

                        String organizerName = d.getString("organizerName");
                        tvEventName.setText("Event - " + (eventName != null ? eventName : "Unknown Event"));
                        tvMessage.setText(message != null ? message : "");
                        tvRecipientCount
                                .setText("Sent to " + (recipientCount != null ? recipientCount : 0) + " recipients");
                        tvOrganizerName.setText("Sent by: " + (organizerName != null ? organizerName : "Unknown"));

                        if (timestamp != null) {
                            Date date = timestamp.toDate();
                            String day = DateFormat.format("MMM d, yyyy", date).toString();
                            String time = DateFormat.format("h:mm a", date).toString();
                            tvTimestamp.setText(day + " • " + time);
                        } else {
                            tvTimestamp.setText("");
                        }

                        btnViewRecipients.setOnClickListener(v1 -> showRecipientsDialog(d));

                        listContainer.addView(card);
                    }

                    if (snap.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No notifications found.");
                        empty.setTextColor(getResources().getColor(R.color.white));
                        empty.setAlpha(0.7f);
                        empty.setPadding(8, 16, 8, 0);
                        listContainer.addView(empty);
                    }
                });
    }

    private void showRecipientsDialog(DocumentSnapshot doc) {
        java.util.List<String> recipientNames = (java.util.List<String>) doc.get("recipientNames");
        Long count = doc.getLong("recipientCount");
        int totalCount = count != null ? count.intValue() : 0;

        if (recipientNames == null || recipientNames.isEmpty()) {
            // Fallback for old logs or empty lists
            java.util.List<String> recipientIds = (java.util.List<String>) doc.get("recipientIds");
            if (recipientIds != null && !recipientIds.isEmpty()) {
                // If we have IDs but no names (old logs), show IDs
                StringBuilder sb = new StringBuilder();
                int limit = 10;
                for (int i = 0; i < Math.min(recipientIds.size(), limit); i++) {
                    sb.append("• User ").append(recipientIds.get(i)).append("\n");
                }
                if (recipientIds.size() > limit) {
                    sb.append("\n...and ").append(recipientIds.size() - limit).append(" more.");
                }
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                        .setTitle("Recipients (IDs only)")
                        .setMessage(sb.toString())
                        .setPositiveButton("Close", null)
                        .show();
                return;
            }

            Toast.makeText(getContext(), "No recipients recorded.", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                getContext());
        builder.setTitle("Recipients");

        StringBuilder sb = new StringBuilder();
        for (String name : recipientNames) {
            sb.append("• ").append(name).append("\n");
        }

        if (totalCount > recipientNames.size()) {
            sb.append("\n...and ").append(totalCount - recipientNames.size()).append(" more.");
        }

        builder.setMessage(sb.toString());
        builder.setPositiveButton("Close", null);
        builder.show();
    }
}
