package com.example.yellow.organizers;

import android.app.DatePickerDialog;
import android.content.ContentResolver;
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

import com.example.yellow.R;
import com.example.yellow.organizers.Event;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Screen for organizers to create a new {@link Event}.
 *
 * <p>What this Activity does, in order:</p>
 * <ol>
 *     <li>Shows inputs for name, description, location, dates, and poster image.</li>
 *     <li>Validates the inputs when the user taps "Create".</li>
 *     <li>Reads the selected poster and stores it as a Base64 data URI (kept in Firestore).</li>
 *     <li>Creates the Event document in Firestore.</li>
 *     <li>Builds a deep link like {@code yellow://event/<docId>} and generates a QR for it.</li>
 *     <li>Writes the deep link and QR image back to the same Event document.</li>
 *     <li>Finishes the Activity (returns to the previous screen).</li>
 * </ol>
 */
public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";

    /** Firebase auth handle used to find the current user. */
    private FirebaseAuth auth;

    // UI elements
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

    // Optional preview area for a generated QR (present only if added in XML)
    private MaterialCardView qrCard;
    private ImageView qrImagePreview;

    // State
    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();
    private Uri selectedPosterUri = null;

    /** Formats dates like "Jan 05, 2025". */
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /**
     * Built-in photo picker launcher.
     *
     * <p>When the user picks a photo, we remember the {@link Uri} and show it in the poster preview.</p>
     */
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedPosterUri = uri;
                    posterImageView.setImageURI(selectedPosterUri);
                    if (uploadPosterText != null) uploadPosterText.setVisibility(View.GONE);
                    if (uploadIcon != null) uploadIcon.setVisibility(View.GONE);
                } else {
                    toast("No image selected");
                }
            });

    /**
     * Fallback content picker for devices without the modern photo picker.
     *
     * <p>Works the same as {@link #photoPicker} but uses a generic file chooser.</p>
     */
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

    /**
     * Standard Activity entry point. Sets up UI, Firebase, and click handlers.
     *
     * @param savedInstanceState previously saved state (usually {@code null} for a new screen)
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("CreateEvent", "HELLO_LOGCAT_SMOKE_TEST");
        try {
            setContentView(R.layout.activity_create_event);
        } catch (Exception e) {
            Log.e(TAG, "Layout inflate failed", e);
            toast("Layout failed: " + e.getMessage());
            finish();
            return;
        }

        try {
            FirebaseApp.initializeApp(this);
            auth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase init failed", e);
            toast("Firebase setup failed");
            finish();
            return;
        }

        // Bind views
        try {
            toolbar = requireView(R.id.toolbar, "toolbar");
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());

            posterCard = requireView(R.id.posterCard, "posterCard");
            posterImageView = requireView(R.id.posterImageView, "posterImageView");
            uploadPosterText = findViewById(R.id.uploadPosterText);
            uploadIcon = findViewById(R.id.uploadIcon);

            eventNameInput = requireView(R.id.eventNameInput, "eventNameInput");
            descriptionInput = requireView(R.id.descriptionInput, "descriptionInput");
            locationInput = requireView(R.id.locationInput, "locationInput");
            startDateInput = requireView(R.id.startDateInput, "startDateInput");
            endDateInput = requireView(R.id.endDateInput, "endDateInput");
            maxEntrantsInput = requireView(R.id.maxEntrantsInput, "maxEntrantsInput");
            geolocationSwitch = requireView(R.id.geolocationSwitch, "geolocationSwitch");
            createEventButton = requireView(R.id.createEventButton, "createEventButton");

            // Optional QR preview (only if present in layout)
            qrCard = findViewById(R.id.qrCard);
            qrImagePreview = findViewById(R.id.qrImage);

        } catch (IllegalStateException badId) {
            Log.e(TAG, "Missing view ID", badId);
            toast(badId.getMessage());
            finish();
            return;
        }

        // Image selection
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

    /**
     * Finds a view by ID and throws a clear error if it is missing.
     *
     * @param id   the view ID from the XML layout
     * @param name a friendly name to include in the error message
     * @param <T>  the expected view type (e.g., {@link ImageView})
     * @return the found view (never {@code null})
     * @throws IllegalStateException if the view is not in the layout
     */
    private <T extends View> T requireView(int id, String name) {
        T v = findViewById(id);
        if (v == null) throw new IllegalStateException("Missing view in layout: " + name);
        return v;
    }

    /**
     * Opens a date picker and writes the chosen date into the correct text box.
     *
     * @param isStart {@code true} to pick the start date; {@code false} for the end date
     */
    private void showDatePicker(boolean isStart) {
        final Calendar cal = isStart ? startCal : endCal;
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dlg = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                cal.set(year, month, dayOfMonth);
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

    /**
     * Validates the form, encodes the poster image, writes the {@link Event},
     * then generates and saves the QR code/deep link for that event.
     *
     * <p>If everything succeeds, this Activity finishes and returns to the previous screen.</p>
     */
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

            FirebaseManager.getInstance().createEvent(ev, new FirebaseManager.CreateEventCallback() {

                /**
                 * Some manager implementations may call this overload. We log it for clarity.
                 *
                 * @param docId the Firestore document ID of the created event
                 */
                @Override
                public void onSuccess(String docId) {
                    Log.w("CreateEvent", "onSuccess(String) was called unexpectedly. docId=" + docId);
                }

                /**
                 * Main success path used here. We have the created {@link Event}, so we use its ID
                 * to build the deep link and QR, then update the same Firestore document.
                 *
                 * @param event the created event (should include a non-empty ID)
                 */
                @Override
                public void onSuccess(Event event) {
                    try {
                        if (event == null || event.getId() == null || event.getId().trim().isEmpty()) {
                            toast("Event created, but no ID returned");
                            finish();
                            return;
                        }

                        String docId = event.getId();

                        // 1) Build deep link
                        String deepLink = "yellow://event/" + docId;

                        // 2) Generate QR bitmap
                        Bitmap qrBmp = com.example.yellow.utils.QrUtils.makeQr(deepLink, 768);

                        // 3) Convert to Base64 PNG data URI
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        qrBmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
                        String qrBase64 = android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP);
                        String qrDataUri = "data:image/png;base64," + qrBase64;

                        // 4) Save QR info back to this event doc (null-safe patch)
                        Map<String, Object> patch = new HashMap<>();
                        if (deepLink != null && !deepLink.isEmpty()) patch.put("qrDeepLink", deepLink);
                        if (qrDataUri != null && !qrDataUri.isEmpty()) patch.put("qrImagePng", qrDataUri);

                        FirebaseManager.getInstance()
                                .updateEvent(docId, patch, new FirebaseManager.SimpleCallback() {
                                    /**
                                     * Called when the QR fields were saved successfully.
                                     * We show a toast and finish to return to the previous screen.
                                     */
                                    @Override public void onSuccess() {
                                        toast("Event created successfully!");
                                        finish();
                                    }

                                    /**
                                     * Called when saving the QR fields failed.
                                     *
                                     * @param e the error from Firestore (may be {@code null})
                                     */
                                    @Override public void onFailure(Exception e) {
                                        toast("Event made, but QR save failed: " + (e != null ? e.getMessage() : ""));
                                        finish();
                                    }
                                });

                    } catch (Exception ex) {
                        toast("Event made, but QR failed: " + ex.getMessage());
                        finish();
                    }
                }

                /**
                 * Called when the initial Event create fails.
                 *
                 * @param e the Firestore error (may be {@code null})
                 */
                @Override
                public void onFailure(Exception e) {
                    Log.e("CreateEvent", "Firestore createEvent failed", e);
                    createEventButton.setEnabled(true);
                    toast("Firestore error: " + (e != null ? e.getMessage() : "unknown"));
                }
            });


        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException", se);
            createEventButton.setEnabled(true);
            toast("No permission to read image.");
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "OutOfMemoryError", oom);
            createEventButton.setEnabled(true);
            toast("Image too large.");
        } catch (FileNotFoundException fnf) {
            Log.e(TAG, "FileNotFound", fnf);
            createEventButton.setEnabled(true);
            toast("Image not found.");
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
            createEventButton.setEnabled(true);
            toast("Error: " + e.getMessage());
        }
    }

    /**
     * Reads text from a {@link TextInputEditText} and trims whitespace.
     *
     * @param et the input field (may be {@code null})
     * @return the trimmed text, or {@code ""} if empty/null
     */
    private static String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    /**
     * Reads an image by {@link Uri} and returns it as a Base64 data URI string.
     * The image is downsampled and compressed to keep the Firestore document small.
     *
     * @param uri the content {@link Uri} of the image
     * @return a string like {@code data:image/jpeg;base64,AAAA...}
     * @throws Exception if the image cannot be opened, decoded, or processed
     */
    public String encodeImageUriToDataUri(Uri uri) throws Exception {
        final int MAX_DOC_BYTES = 900 * 1024;
        final int MAX_DIMENSION = 1280;
        final int MIN_JPEG_QUALITY = 40;

        ContentResolver cr = getContentResolver();

        // Bounds pass
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is = cr.openInputStream(uri)) {
            if (is == null) throw new FileNotFoundException("Null input stream");
            BitmapFactory.decodeStream(is, null, bounds);
        }

        int w = bounds.outWidth;
        int h = bounds.outHeight;
        if (w <= 0 || h <= 0) throw new IllegalArgumentException("Invalid image");

        int inSample = 1;
        while (w / inSample > MAX_DIMENSION || h / inSample > MAX_DIMENSION) {
            inSample *= 2;
        }

        // Decode with sampling + memory-friendly config
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = Math.max(1, inSample);
        opts.inPreferredConfig = Bitmap.Config.RGB_565;

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
            float scale = Math.min(MAX_DIMENSION / (float) bw, MAX_DIMENSION / (float) bh);
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

    /**
     * Tries to detect the MIME type of the given {@link Uri}.
     *
     * @param cr  the {@link ContentResolver} to query
     * @param uri the content {@link Uri}
     * @return a MIME type like {@code image/jpeg}; defaults to {@code image/jpeg} if unknown
     */
    private String guessMime(ContentResolver cr, Uri uri) {
        String t = cr.getType(uri);
        if (t == null || t.trim().isEmpty()) t = "image/jpeg";
        return t;
    }

    /**
     * Returns a human-readable file name for a content {@link Uri}.
     *
     * @param uri the content {@link Uri}
     * @return the display name, or {@code null} if not available
     */
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

    /**
     * Shows a short popup message (Toast) on the screen.
     *
     * @param msg the message text to show
     */
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
