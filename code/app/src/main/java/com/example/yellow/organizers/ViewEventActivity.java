package com.example.yellow.organizers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.yellow.R;
import com.example.yellow.ui.ManageEntrants.ManageEntrantsActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.net.Uri;
import android.widget.Toast;
import android.util.Log;

public class ViewEventActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager; //entrants fragment, mapfragment, settingsfragment, notifyfragment, qrfragment
    private Event currentEvent;

    private String eventId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        Uri data = getIntent().getData();
        eventId = null;

        // Case 1 — Deep link "yellow://event/<id>"
        /*
        checks if intent contains a uri
        verifies its yellow
        verifies host is event
        takes last part of path which is actual event id
         */
        if (data != null
                && "yellow".equalsIgnoreCase(data.getScheme())
                && "event".equalsIgnoreCase(data.getHost())
                && data.getPathSegments() != null
                && !data.getPathSegments().isEmpty()) {

            eventId = data.getPathSegments()
                    .get(data.getPathSegments().size() - 1);
        }

        String passedId = getIntent().getStringExtra("eventId"); //extract eventid from normal intent

        if (eventId == null) {
            eventId = passedId;
        }

        //DEBUGGING using toast
        Log.d("DeepLink", "FINAL eventId = " + eventId);
        Toast.makeText(this, "eventId=" + eventId, Toast.LENGTH_SHORT).show();

        //load event from firestore
        if (eventId != null && !eventId.trim().isEmpty()) {
            loadEventAndRender(eventId);
        }


        // ---- Temporary QR popup for testing ----
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
                Log.e("DeepLink", "QR error", t);
                Toast.makeText(this,
                        "QR error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });


        // ---- Insets padding ----
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, topInset, 0, 0);
            v.setBackgroundResource(R.color.surface_dark);
            return insets;
        });


        // ---- Header ----
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvEventName = findViewById(R.id.tvEventName);
        TextView tvEventDate = findViewById(R.id.tvEventDate);

        btnBack.setOnClickListener(v -> finish());

        String eventName = getIntent().getStringExtra("eventName");
        String eventDate = getIntent().getStringExtra("eventDate");

        if (eventName != null) tvEventName.setText(eventName);
        if (eventDate != null) tvEventDate.setText(eventDate);


        // ---- Tabs + Pager ----
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ViewEventPageAdapter adapter = new ViewEventPageAdapter(this, eventId);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Entrants"); break;
                case 1: tab.setText("Map"); break;
                case 2: tab.setText("Settings"); break;
                case 3: tab.setText("Notify"); break;
                case 4: tab.setText("QR Code"); break;
            }
        }).attach();

        Button manageBtn = findViewById(R.id.btnManageEntrants);

        manageBtn.setOnClickListener(v -> {
            Intent i = new Intent(ViewEventActivity.this, ManageEntrantsActivity.class);
            i.putExtra("eventId", eventId);
            i.putExtra("eventName", tvEventName.getText().toString());
            startActivity(i);
        });
    }


    // -----------------------------------------------------------------
    // Load event + notify fragments
    // -----------------------------------------------------------------
    private void loadEventAndRender(String eventId) {

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(snap -> {

                    if (!snap.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_LONG).show();
                        return;
                    }

                    currentEvent = snap.toObject(Event.class);

                    // Notify QrFragment & others
                    getSupportFragmentManager().setFragmentResult(
                            "eventLoaded",
                            new Bundle()
                    );

                    if (currentEvent == null) {
                        Toast.makeText(this, "Malformed event", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Event ev = currentEvent;

                    // Title + Desc
                    TextView title = findViewById(R.id.eventTitle);
                    TextView desc  = findViewById(R.id.descriptionInput);
                    if (title != null) title.setText(ev.getName());
                    if (desc  != null) desc.setText(ev.getDescription());

                    // Poster (Base64)
                    ImageView poster = findViewById(R.id.posterImageView);
                    setImageFromDataUri(poster, ev.getPosterUrl());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Load failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void setImageFromDataUri(@Nullable ImageView view,
                                     @Nullable String dataUri) {
        if (view == null || dataUri == null || dataUri.isEmpty()) return;
        try {
            String base64 = dataUri.startsWith("data:")
                    ? dataUri.substring(dataUri.indexOf(',') + 1)
                    : dataUri;

            byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            android.graphics.Bitmap bmp =
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            if (bmp != null) view.setImageBitmap(bmp);
        } catch (Exception ignored) {}
    }

    public Event getEvent() {
        return currentEvent;
    }
}
