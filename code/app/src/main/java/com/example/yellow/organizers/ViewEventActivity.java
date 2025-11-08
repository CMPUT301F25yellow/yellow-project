package com.example.yellow.organizers;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.example.yellow.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.net.Uri;
import android.widget.Toast;
import android.util.Log;

/**
 * Shows an Event screen with tabs (Entrants, Map, Settings, Notify)
 *
 * <p>This Activity can be opened in two ways:</p>
 * <ul>
 *   <li>From inside the app using an explicit Intent (with extras like {@code eventId}).</li>
 *   <li>From a deep link like {@code yellow://event/<eventId>}.</li>
 * </ul>
 *
 * <p>When launched via deep link, it tries to read the event ID from the URI and
 * (if available) load the event to populate the UI. It also shows a temporary QR dialog
 * for debugging (to confirm the deep link)</p>
 */
public class ViewEventActivity extends AppCompatActivity {

    /** Tab bar at the top of the screen. */
    private TabLayout tabLayout;
    /** ViewPager that hosts tab pages. */
    private ViewPager2 viewPager;

    /**
     * Standard Activity entry point
     *
     * <p>What this method sets up:</p>
     * <ol>
     *   <li>Parses a deep link to get {@code eventId} if present</li>
     *   <li>Shows a quick QR dialog (debug helper) for the parsed ID</li>
     *   <li>Applies window inset padding so content stays below the status bar</li>
     *   <li>Initializes header views (back button, title, date) from Intent extras</li>
     *   <li>Sets up the {@link ViewPager2} with the {@link TabLayout}</li>
     * </ol>
     *
     * @param savedInstanceState previous state if the Activity was recreated; usually {@code null}
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        // ---- Deep link parsing----
        Uri data = getIntent().getData();
        String eventId = null;
        if (data != null
                && "yellow".equalsIgnoreCase(data.getScheme())
                && "event".equalsIgnoreCase(data.getHost())
                && data.getPathSegments() != null
                && !data.getPathSegments().isEmpty()) {
            eventId = data.getPathSegments().get(data.getPathSegments().size() - 1);
        }
        if (eventId != null && !eventId.trim().isEmpty()) {
            loadEventAndRender(eventId);
        }
        Log.d("DeepLink", "eventId = " + eventId);
        Toast.makeText(this, "eventId=" + String.valueOf(eventId), Toast.LENGTH_SHORT).show();

        // ---- Force-show QR in a dialog (For testing) ----
        String finalEventId = eventId;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            try {
                if (finalEventId != null && !finalEventId.trim().isEmpty()) {
                    String deepLink = "yellow://event/" + finalEventId;

                    android.graphics.Bitmap bmp =
                            com.example.yellow.utils.QrUtils.makeQr(deepLink, 768);

                    android.widget.ImageView iv = new android.widget.ImageView(this);
                    iv.setAdjustViewBounds(true);
                    iv.setPadding(48, 48, 48, 48);
                    iv.setImageBitmap(bmp);

                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("QR for " + finalEventId)
                            .setView(iv)
                            .setPositiveButton("OK", null)
                            .show();
                }
            } catch (Throwable t) {
                android.util.Log.e("DeepLink", "QR error", t);
                Toast.makeText(this,
                        "QR error: " + t.getClass().getSimpleName() + ": " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });

        // Apply system bar inset padding to the root view
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, topInset, 0, 0);
            v.setBackgroundResource(R.color.surface_dark);
            return insets;
        });

        // Setup header
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvEventName = findViewById(R.id.tvEventName);
        TextView tvEventDate = findViewById(R.id.tvEventDate);

        // Get event info passed from intent
        String eventName = getIntent().getStringExtra("eventName");
        String eventDate = getIntent().getStringExtra("eventDate");

        if (eventName != null) tvEventName.setText(eventName);
        if (eventDate != null) tvEventDate.setText(eventDate);

        // Back button action
        btnBack.setOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;

            // Add padding at the top to push content below the camera notch/status bar
            v.setPadding(0, topInset, 0, 0);

            // make background match dark theme
            v.setBackgroundResource(R.color.surface_dark);

            return insets;
        });

        // Tabs + pager
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        eventId = getIntent().getStringExtra("eventId");
        ViewEventPageAdapter adapter = new ViewEventPageAdapter(this, eventId);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Entrants"); break;
                case 1: tab.setText("Map"); break;
                case 2: tab.setText("Settings"); break;
                case 3: tab.setText("Notify"); break;
            }
        }).attach();
    }

    /**
     * Loads the event document from Firestore and updates UI parts with its data
     *
     * <p>This currently sets the title, description, poster, and QR image (if present
     * in the layout). If the document is missing or malformed, it shows a toast</p>
     *
     * @param eventId the Firestore document ID for the event (may be {@code null})
     */
    private void loadEventAndRender(String eventId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        android.widget.Toast.makeText(this, "Event not found", android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Map to your Event model
                    Event ev = snap.toObject(Event.class);
                    if (ev == null) {
                        android.widget.Toast.makeText(this, "Malformed event", android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Title
                    android.widget.TextView title = findViewById(R.id.eventTitle);
                    android.widget.TextView desc  = findViewById(R.id.descriptionInput);
                    if (title != null) title.setText(ev.getName());
                    if (desc  != null) desc.setText(ev.getDescription());

                    // Poster: if stored Base64 data URI, decode and show
                    android.widget.ImageView poster = findViewById(R.id.posterImageView);
                    setImageFromDataUri(poster, ev.getPosterUrl());

                    // QR: decode Base64 data URI and show
                    android.widget.ImageView qrView = findViewById(R.id.qrImage);
                    setImageFromDataUri(qrView, ev.getQrImagePng());

                })
                .addOnFailureListener(e ->
                        android.widget.Toast.makeText(this, "Load failed: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show()
                );
    }

    /**
     * Shows a Base64-encoded image (data URI) in an {@link ImageView}
     *
     * <p>A "data URI" looks like {@code data:image/png;base64,AAAA...}. This method:
     * </p>
     * <ol>
     *   <li>Does nothing if the view or string is null/empty (safe to call)</li>
     *   <li>Strips the {@code data:...} header and decodes the Base64 bytes</li>
     *   <li>Creates a {@link android.graphics.Bitmap} and sets it on the view</li>
     * </ol>
     *
     * @param view    the target {@link ImageView} (can be {@code null})
     * @param dataUri the Base64 data URI string (can be {@code null} or empty)
     */
    private void setImageFromDataUri(@androidx.annotation.Nullable android.widget.ImageView view,
                                     @androidx.annotation.Nullable String dataUri) {
        if (view == null || dataUri == null || dataUri.isEmpty()) return;
        try {
            String base64 = dataUri.startsWith("data:") ? dataUri.substring(dataUri.indexOf(',') + 1) : dataUri;
            byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp != null) view.setImageBitmap(bmp);
        } catch (Exception ignore) { /* no-op */ }
    }

}
