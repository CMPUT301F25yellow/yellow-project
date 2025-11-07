package com.example.yellow.organizers;

import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yellow.utils.FirebaseManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.example.yellow.organizers.Event;
import com.example.yellow.R;


/**
 * Firestore-only event creation with inline Base64 poster (no Firebase Storage).
 * Adds robust guards to avoid "app keeps stopping".
 */
public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";

    // Firebase
    private FirebaseAuth auth;

    // UI (IDs must match activity_create_event.xml)
    private MaterialToolbar toolbar;
    private MaterialCardView posterCard;
    private ImageView posterImageView;
    private TextView uploadPosterText;
    private ImageView uploadIcon;

    private TextInputEditText eventNameInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText locationInput;

    private TextInputEditText startDateInput;
    private TextInputEditText endDateInput;

    private TextInputEditText maxEntrantsInput;
    private SwitchMaterial geolocationSwitch;

    private MaterialButton createEventButton;

    // State
    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();
    private Uri selectedPosterUri = null;

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    // Photo Picker (preferred)
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedPosterUri = uri;
                    // Avoid Glide dependency; use setImageURI to remove one crash variable
                    posterImageView.setImageURI(selectedPosterUri);
                    if (uploadPosterText != null) uploadPosterText.setVisibility(View.GONE);
                    if (uploadIcon != null) uploadIcon.setVisibility(View.GONE);
                } else {
                    toast("No image selected");
                }
            });

    // Fallback: GetContent (works on all API levels)
    private final ActivityResultLauncher<String> contentPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPosterUri = uri;
                    posterImageView.setImageURI(selectedPosterUri);
                    if (uploadPosterText != null) uploadPosterText.setVisibility(View.GONE);
                    if (uploadIcon != null) uploadIcon.setVisibility(View.GONE);
                } else {
                    toast("No image selected");
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_create_event);
        } catch (Exception e) {
            // Wrong layout name will crash here
            Log.e(TAG, "Layout inflate failed", e);
            toast("Layout inflate failed: " + e.getMessage());
            finish();
            return;
        }

        // Firebase
        try {
            FirebaseApp.initializeApp(this);
            auth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase init failed", e);
            toast("Firebase init failed: " + e.getMessage());
            finish();
            return;
        }

        // Bind UI safely
        try {
            toolbar = requireView(R.id.toolbar, "toolbar");
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());

            posterCard = requireView(R.id.posterCard, "posterCard");
            posterImageView = requireView(R.id.posterImageView, "posterImageView");
            uploadPosterText = findViewById(R.id.uploadPosterText); // optional
            uploadIcon = findViewById(R.id.uploadIcon);             // optional

            eventNameInput = requireView(R.id.eventNameInput, "eventNameInput");
            descriptionInput = requireView(R.id.descriptionInput, "descriptionInput");
            locationInput = requireView(R.id.locationInput, "locationInput");

            startDateInput = requireView(R.id.startDateInput, "startDateInput");
            endDateInput = requireView(R.id.endDateInput, "endDateInput");

            maxEntrantsInput = requireView(R.id.maxEntrantsInput, "maxEntrantsInput");
            geolocationSwitch = requireView(R.id.geolocationSwitch, "geolocationSwitch");

            createEventButton = requireView(R.id.createEventButton, "createEventButton");
        } catch (IllegalStateException badId) {
            Log.e(TAG, "Missing view ID", badId);
            toast(badId.getMessage());
            finish();
            return;
        }

        // Image picking (Photo Picker if available, else GetContent)
        View.OnClickListener pickImage = v -> {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
                photoPicker.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            } else {
                contentPicker.launch("image/*");
            }
        };
        posterCard.setOnClickListener(pickImage);
        posterImageView.setOnClickListener(pickImage);

        // Date pickers
        startDateInput.setOnClickListener(v -> showDatePicker(true));
        endDateInput.setOnClickListener(v -> showDatePicker(false));

        // Defaults
        startDateInput.setText(dateFmt.format(startCal.getTime()));
        endCal.setTimeInMillis(startCal.getTimeInMillis());
        endDateInput.setText(dateFmt.format(endCal.getTime()));

        createEventButton.setOnClickListener(v -> onCreateEvent());
    }

    private <T extends View> T requireView(int id, String name) {
        T v = findViewById(id);
        if (v == null) throw new IllegalStateException("Missing view in layout: " + name);
        return v;
    }

    private void showDatePicker(boolean isStart) {
        final Calendar cal = isStart ? startCal : endCal;
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dlg = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                if (isStart) {
                    startDateInput.setText(dateFmt.format(cal.getTime()));
                    if (endCal.before(startCal)) {
                        endCal.setTimeInMillis(startCal.getTimeInMillis());
                        endDateInput.setText(dateFmt.format(endCal.getTime()));
                    }
                } else {
                    endDateInput.setText(dateFmt.format(cal.getTime()));
                }
            }
        }, y, m, d);
        dlg.show();
    }

    private void onCreateEvent() {
        try {
            String name = textOf(eventNameInput);
            String desc = textOf(descriptionInput);
            String loc  = textOf(locationInput);
            String maxStr = textOf(maxEntrantsInput);
            boolean requireGeo = geolocationSwitch.isChecked();

            if (TextUtils.isEmpty(name)) {
                eventNameInput.setError("Required");
                eventNameInput.requestFocus();
                return;
            }
            if (selectedPosterUri == null) {
                toast("Please select a poster image");
                return;
            }
            if (endCal.before(startCal)) {
                toast("End date cannot be before start date");
                return;
            }

            int maxEntrants = 0;
            if (!TextUtils.isEmpty(maxStr)) {
                try {
                    maxEntrants = Integer.parseInt(maxStr);
                    if (maxEntrants < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    maxEntrantsInput.setError("Invalid number");
                    maxEntrantsInput.requestFocus();
                    return;
                }
            }

            createEventButton.setEnabled(false);

            // Encode image -> data URI (Firestore safe)
            String dataUri = encodeImageUriToDataUri(selectedPosterUri);
            if (dataUri == null) {
                createEventButton.setEnabled(true);
                toast("Could not read image");
                return;
            }

            String organizer = (auth.getCurrentUser() != null)
                    ? auth.getCurrentUser().getUid()
                    : "anonymous";

            Event ev = new Event();
            ev.setName(name);
            ev.setDescription(desc);
            ev.setLocation(loc);
            ev.setPosterImageUrl(dataUri);
            ev.setOrganizerId(organizer);
            if (auth.getCurrentUser() != null && auth.getCurrentUser().getDisplayName() != null) {
                ev.setOrganizerName(auth.getCurrentUser().getDisplayName());
            }
            ev.setStartDate(new Timestamp(startCal.getTime()));
            Calendar endCopy = (Calendar) endCal.clone();
            endCopy.set(Calendar.HOUR_OF_DAY, 23);
            endCopy.set(Calendar.MINUTE, 59);
            endCopy.set(Calendar.SECOND, 59);
            endCopy.set(Calendar.MILLISECOND, 999);
            ev.setEndDate(new Timestamp(endCopy.getTime()));
            ev.setMaxEntrants(maxEntrants);
            ev.setRequireGeolocation(requireGeo);
            ev.setCreatedAt(Timestamp.now());

            // Firestore write via your FirebaseManager
            FirebaseManager.getInstance().createEvent(ev, new FirebaseManager.CreateEventCallback() {
                @Override public void onSuccess(String docId) {
                    toast("Event created!");
                    finish();
                }

                @Override
                public void onSuccess(Event event) {
                    toast("Event created!");
                    finish();
                }

                @Override public void onFailure(Exception e) {
                    Log.e(TAG, "Firestore createEvent failed", e);
                    createEventButton.setEnabled(true);
                    toast("Firestore error: " + (e != null ? e.getMessage() : "unknown"));
                }
            });

        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException", se);
            createEventButton.setEnabled(true);
            toast("No permission to read image. Try another source.");
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "OutOfMemoryError", oom);
            createEventButton.setEnabled(true);
            toast("Image too large. Pick a smaller one.");
        } catch (FileNotFoundException fnf) {
            Log.e(TAG, "FileNotFound", fnf);
            createEventButton.setEnabled(true);
            toast("Image not found. Reselect it.");
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
            createEventButton.setEnabled(true);
            toast("Error: " + e.getMessage());
        }
    }

    private static String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    // --- Robust inline Base64 encoder with memory guards and byte budget ---
    private String encodeImageUriToDataUri(Uri uri) throws Exception {
        final int MAX_DOC_BYTES = 900 * 1024; // keep doc under 1 MiB
        final int MAX_DIMENSION = 1280;      // cap long edge
        final int MIN_JPEG_QUALITY = 40;

        ContentResolver cr = getContentResolver();

        // Pass 1: bounds
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is = cr.openInputStream(uri)) {
            if (is == null) throw new FileNotFoundException("Null input stream");
            BitmapFactory.decodeStream(is, null, bounds);
        }

        int w = bounds.outWidth;
        int h = bounds.outHeight;
        if (w <= 0 || h <= 0) throw new IllegalArgumentException("Unsupported or corrupt image");

        int inSample = 1;
        while (w / inSample > MAX_DIMENSION || h / inSample > MAX_DIMENSION) {
            inSample *= 2;
        }

        // Pass 2: decode with sample + low-memory config
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = Math.max(1, inSample);
        opts.inPreferredConfig = Bitmap.Config.RGB_565; // reduce memory vs ARGB_8888

        Bitmap bitmap;
        try (InputStream is2 = cr.openInputStream(uri)) {
            if (is2 == null) throw new FileNotFoundException("Null input stream");
            bitmap = BitmapFactory.decodeStream(is2, null, opts);
        }
        if (bitmap == null) throw new IllegalArgumentException("Unable to decode image");

        // Extra scale if still too large
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();
        if (bw > MAX_DIMENSION || bh > MAX_DIMENSION) {
            float scale = Math.min(
                    MAX_DIMENSION / (float) bw,
                    MAX_DIMENSION / (float) bh
            );
            int nw = Math.max(1, Math.round(bw * scale));
            int nh = Math.max(1, Math.round(bh * scale));
            bitmap = Bitmap.createScaledBitmap(bitmap, nw, nh, true);
        }

        // Compress loop
        int quality = 85;
        byte[] jpg;
        while (true) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
            jpg = bos.toByteArray();
            if (jpg.length <= MAX_DOC_BYTES || quality <= MIN_JPEG_QUALITY) break;
            quality -= 5;
        }

        String b64 = android.util.Base64.encodeToString(jpg, android.util.Base64.NO_WRAP);
        String mime = guessMime(cr, uri);
        return "data:" + mime + ";base64," + b64;
    }

    private String guessMime(ContentResolver cr, Uri uri) {
        String t = cr.getType(uri);
        if (t == null || t.trim().isEmpty()) t = "image/jpeg";
        return t;
    }

    @SuppressWarnings("unused")
    private String getDisplayName(Uri uri) {
        String name = null;
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        }
        return name;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
