package com.example.yellow.ui.ManageEntrants;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
//author: waylon
public class EnrolledFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout container;
    private String eventId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_enrolled_entrants, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        container = view.findViewById(R.id.enrolledContainer);
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        View btn = view.findViewById(R.id.btnSendNotification);
        if (btn != null) btn.setVisibility(View.GONE);

        if (eventId == null) {
            if (isSafe()) Toast.makeText(getContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        loadEnrolledEntrants();

        Button btnNotify = view.findViewById(R.id.btnNotifyEnrolled);
        btnNotify.setOnClickListener(v -> showNotificationDialog());

        Button btnExport = view.findViewById(R.id.btnExportCSV);
        btnExport.setOnClickListener(v -> exportEnrolledCSV());
    }

    //export to csv
    private void exportEnrolledCSV() {
        db.collection("events").document(eventId)
                .collection("enrolled")
                .get()
                .addOnSuccessListener(enrolledSnapshot -> {

                    if (!isSafe()) return;

                    if (enrolledSnapshot.isEmpty()) {
                        if (isSafe()) Toast.makeText(getContext(), "No enrolled entrants", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    java.util.List<Map<String, String>> entrants = new java.util.ArrayList<>();
                    java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);

                    for (DocumentSnapshot doc : enrolledSnapshot) {
                        String userId = doc.getString("userId");
                        String ts = extractTimestamp(doc);

                        if (userId == null) {
                            if (counter.incrementAndGet() == enrolledSnapshot.size() && isSafe()) {
                                writeToDownloadsAndShare(entrants);
                            }
                            continue;
                        }

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    if (!isSafe()) return;

                                    String name = profile.getString("fullName");
                                    String email = profile.getString("email");

                                    Map<String, String> row = new HashMap<>();
                                    row.put("name", name == null ? "" : name);
                                    row.put("email", email == null ? "" : email);
                                    row.put("userId", userId);
                                    row.put("timestamp", ts);
                                    entrants.add(row);

                                    if (counter.incrementAndGet() == enrolledSnapshot.size() && isSafe()) {
                                        writeToDownloadsAndShare(entrants);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (counter.incrementAndGet() == enrolledSnapshot.size() && isSafe()) {
                                        writeToDownloadsAndShare(entrants);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (isSafe())
                        Toast.makeText(getContext(), "Failed to load entrants", Toast.LENGTH_SHORT).show();
                });
    }

    private void writeToDownloadsAndShare(java.util.List<Map<String, String>> rows) {
        if (!isSafe()) return;

        if (rows.isEmpty()) {
            Toast.makeText(getContext(), "Nothing to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Name,Email,UserId,EnrolledAt\n");

        for (Map<String, String> r : rows) {
            csv.append(r.get("name").replace(",", " "))
                    .append(",")
                    .append(r.get("email"))
                    .append(",")
                    .append(r.get("userId"))
                    .append(",")
                    .append(r.get("timestamp"))
                    .append("\n");
        }

        saveToDownloadsAndShare(csv.toString());
    }

    private void saveToDownloadsAndShare(String csvContent) {
        if (!isSafe()) return;

        try {
            String fileName = "enrolled_export_" + eventId + ".csv";

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            Uri uri = requireContext().getContentResolver().insert(collection, values);

            if (uri == null) {
                if (isSafe()) Toast.makeText(getContext(), "Failed to create file", Toast.LENGTH_SHORT).show();
                return;
            }

            try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
                outputStream.write(csvContent.getBytes());
            }

            if (isSafe()) Toast.makeText(getContext(), "CSV saved to Downloads!", Toast.LENGTH_LONG).show();
            if (isSafe()) shareCSVUri(uri);

        } catch (Exception e) {
            if (isSafe()) Toast.makeText(getContext(), "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCSVUri(Uri uri) {
        if (!isSafe()) return;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Share CSV"));
    }

    private void loadEnrolledEntrants() {
        if (!isSafe()) return;

        container.removeAllViews();

        db.collection("events").document(eventId)
                .collection("enrolled")
                .addSnapshotListener((snapshot, e) -> {

                    if (!isSafe()) return;

                    container.removeAllViews();

                    if (e != null || snapshot == null || snapshot.isEmpty()) {

                        if (!isSafe()) return;

                        TextView empty = new TextView(requireContext());
                        empty.setText("No Enrolled entrants");
                        empty.setTextColor(requireContext().getColor(R.color.hinty));
                        empty.setPadding(16, 24, 16, 24);

                        container.addView(empty);
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot) {

                        if (!isSafe()) return;

                        String userId = doc.getString("userId");

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    if (!isSafe()) return;

                                    String name = profile.getString("fullName");
                                    String email = profile.getString("email");
                                    String date = extractTimestamp(doc);

                                    addEntrantCard(name, email, date, "Enrolled");
                                });
                    }
                });
    }

    private void showNotificationDialog() {
        if (!isSafe()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Notify Enrolled Entrants");

        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Enter message");
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            if (!isSafe()) return;

            String message = input.getText().toString().trim();
            if (message.isEmpty()) message = "Event update from organizer.";
            sendNotificationToEnrolled(message);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void sendNotificationToEnrolled(String message) {
        db.collection("events").document(eventId)
                .collection("enrolled")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!isSafe()) return;

                    if (snapshot.isEmpty()) {
                        Toast.makeText(getContext(),
                                "No enrolled entrants to notify", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    java.util.List<String> userIds = new java.util.ArrayList<>();

                    for (DocumentSnapshot doc : snapshot) {
                        String userId = doc.getString("userId");
                        if (userId == null) continue;

                        db.collection("profiles").document(userId)
                                .get()
                                .addOnSuccessListener(profile -> {
                                    if (!isSafe()) return;

                                    Boolean enabled = profile.getBoolean("notificationsEnabled");
                                    if (enabled == null) enabled = true;

                                    if (enabled) userIds.add(userId);
                                });
                    }

                    new android.os.Handler().postDelayed(() -> {

                        if (!isSafe()) return;

                        if (userIds.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "No users to notify (all have notifications off)", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        db.collection("events").document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {

                                    if (!isSafe()) return;

                                    String eventName = eventDoc.getString("name");
                                    if (eventName == null) eventName = "Unknown Event";

                                    com.example.yellow.utils.NotificationManager.sendNotification(
                                            getContext(),
                                            eventId,
                                            eventName,
                                            message,
                                            userIds,
                                            new com.example.yellow.utils.NotificationManager.OnNotificationSentListener() {
                                                @Override
                                                public void onSuccess() {
                                                    if (isSafe())
                                                        Toast.makeText(getContext(),
                                                                "Notification sent!", Toast.LENGTH_SHORT).show();
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    if (isSafe())
                                                        Toast.makeText(getContext(),
                                                                "Failed to send: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                });

                    }, 500);
                })
                .addOnFailureListener(e -> {
                    if (isSafe())
                        Toast.makeText(getContext(),
                                "Failed to fetch enrolled users", Toast.LENGTH_SHORT).show();
                });
    }

    private String extractTimestamp(DocumentSnapshot doc) {
        Object ts = doc.get("timestamp");

        if (ts instanceof Timestamp) {
            return dateFormat.format(((Timestamp) ts).toDate());
        } else if (ts instanceof Long) {
            return dateFormat.format(new Date((Long) ts));
        }

        return "Unknown";
    }

    private void addEntrantCard(String name, String email, String date, String status) {
        if (!isSafe()) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View card = inflater.inflate(R.layout.item_entrant_card, container, false);

        ((TextView) card.findViewById(R.id.tvEntrantName)).setText(name);
        ((TextView) card.findViewById(R.id.tvEntrantEmail)).setText(email);
        ((TextView) card.findViewById(R.id.tvJoinDate)).setText("Updated: " + date);
        ((TextView) card.findViewById(R.id.tvStatus)).setText(status);

        card.findViewById(R.id.tvStatus)
                .getBackground()
                .setTint(requireContext().getColor(R.color.green_400));

        container.addView(card);
    }
    private boolean isRunningInTest() {
        try {
            ApplicationInfo appInfo = requireContext().getPackageManager()
                    .getApplicationInfo(requireContext().getPackageName(),
                            PackageManager.GET_META_DATA);

            Bundle meta = appInfo.metaData;
            return meta != null && meta.getBoolean("is_test_environment", false);
        } catch (Exception e) {
            return false;
        }
    }
    private boolean isSafe() {
        return isAdded() && getContext() != null && container != null;
    }
}