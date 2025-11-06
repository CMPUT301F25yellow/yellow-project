package com.example.yellow.organizers;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.example.yellow.R;
import com.example.yellow.utils.FirebaseManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CreateEventActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    private FirebaseFirestore db;

    private FirebaseStorage storage;

    // UI Components
    private MaterialToolbar toolbar;
    private ImageView posterImageView;
    private TextView uploadPosterText;
    private ImageView uploadIcon;
    private MaterialCardView posterCard;
    private TextInputEditText eventNameInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText locationInput;
    private TextInputEditText startDateInput;
    private TextInputEditText endDateInput;
    private TextInputEditText maxEntrantsInput;
    private SwitchMaterial geolocationSwitch;
    private MaterialButton createEventButton;

    // Data
    private Uri selectedImageUri;
    private Date startDate;
    private Date endDate;
    private SimpleDateFormat dateFormatter;
    private ProgressDialog progressDialog;
    private FirebaseManager firebaseManager;


    // Image Picker
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;

            // Add top padding and match background color to your theme
            v.setPadding(0, topInset, 0, 0);
            v.setBackgroundColor(getColor(R.color.surface_dark));

            return insets;
        });


        initializeViews();
        initializeData();
        setupListeners();
        setupImagePicker();
    }

    /**
     * Initializes the UI components.
     */
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        posterImageView = findViewById(R.id.posterImageView);
        uploadPosterText = findViewById(R.id.uploadPosterText);
        uploadIcon = findViewById(R.id.uploadIcon);
        posterCard = findViewById(R.id.posterCard);
        eventNameInput = findViewById(R.id.eventNameInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        locationInput = findViewById(R.id.locationInput);
        startDateInput = findViewById(R.id.startDateInput);
        endDateInput = findViewById(R.id.endDateInput);
        maxEntrantsInput = findViewById(R.id.maxEntrantsInput);
        geolocationSwitch = findViewById(R.id.geolocationSwitch);
        createEventButton = findViewById(R.id.createEventButton);
    }

    /**
     * Initializes the data and sets up listeners.
     */
    private void initializeData() {
        dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        firebaseManager = FirebaseManager.getInstance();

        // Progress Dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating event...");
        progressDialog.setCancelable(false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Sets up the listeners for the UI components.
     */
    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        posterCard.setOnClickListener(v -> openImagePicker());
        startDateInput.setOnClickListener(v -> showDatePicker(true));
        endDateInput.setOnClickListener(v -> showDatePicker(false));
        createEventButton.setOnClickListener(v -> validateAndCreateEvent());
    }

    /**
     * Sets up the image picker for selecting an image.
     */
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;

                        // Load image using Glide
                        Glide.with(this)
                                .load(uri)
                                .centerCrop()
                                .into(posterImageView);

                        posterImageView.setVisibility(View.VISIBLE);
                        uploadPosterText.setVisibility(View.GONE);
                        uploadIcon.setVisibility(View.GONE);
                    }
                }
        );
    }

    /**
     * Opens the image picker to select an image.
     */
    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    /**
     * Shows a date picker dialog.
     * @param isStartDate Whether to show the start date picker.
     */
    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        if (isStartDate && startDate != null) {
            calendar.setTime(startDate);
        } else if (!isStartDate && endDate != null) {
            calendar.setTime(endDate);
        }

        // 1. Define the listener first
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth);
            Date selectedDate = selectedCalendar.getTime();

            String formattedDate = dateFormatter.format(selectedDate);

            if (isStartDate) {
                startDate = selectedDate;
                startDateInput.setText(formattedDate);
            } else {
                endDate = selectedDate;
                endDateInput.setText(formattedDate);
            }
        };

        // 2. Use the corrected constructor
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                dateSetListener, // Pass the listener
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    /**
     * Validates and creates an event based on user input.
     */
    private void validateAndCreateEvent() {
        // Get input values
        String name = Objects.requireNonNull(eventNameInput.getText()).toString().trim();
        String description = Objects.requireNonNull(descriptionInput.getText()).toString().trim();
        String location = Objects.requireNonNull(locationInput.getText()).toString().trim();
        String maxEntrantsStr = Objects.requireNonNull(maxEntrantsInput.getText()).toString().trim();

        // Validate required fields
        if (TextUtils.isEmpty(name)) {
            eventNameInput.setError("Event name is required");
            eventNameInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            descriptionInput.setError("Description is required");
            descriptionInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(location)) {
            locationInput.setError("Location is required");
            locationInput.requestFocus();
            return;
        }

        if (startDate == null) {
            Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDate == null) {
            Toast.makeText(this, "Please select an end date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate dates
        if (endDate.before(startDate)) {
            Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate max entrants (optional)
        Integer maxEntrants = null;
        if (!TextUtils.isEmpty(maxEntrantsStr)) {
            try {
                maxEntrants = Integer.parseInt(maxEntrantsStr);
                if (maxEntrants <= 0) {
                    maxEntrantsInput.setError("Must be greater than 0");
                    maxEntrantsInput.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                maxEntrantsInput.setError("Invalid number");
                maxEntrantsInput.requestFocus();
                return;
            }
        }

        // Create Event object
        Event event = new Event(
                name,
                description,
                location,
                new Timestamp(startDate),
                new Timestamp(endDate),
                maxEntrants,
                geolocationSwitch.isChecked(),
                null, // Will be set after image upload
                null, // Will be set by FirebaseManager
                null  // Will be set by FirebaseManager
        );

        // Upload and create event
        Log.d("CreateEventActivity", "Validation passed — creating event...");
        createEvent(event);
    }

    /**
     * Creates an event using FirebaseManager.
     * @param event The event to be created.
     */
    private void createEvent(Event event) {
        progressDialog.show();

        FirebaseManager.getInstance().uploadImageAndCreateEvent(
                selectedImageUri,
                event,
                new FirebaseManager.CreateEventCallback() {
                    @Override
                    public void onSuccess(Event createdEvent) {
                        progressDialog.dismiss();
                        Toast.makeText(CreateEventActivity.this,
                                "Event created successfully!",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(CreateEventActivity.this,
                                "Error creating event: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                },
                progress -> progressDialog.setMessage("Uploading image... " + progress + "%")
        );
    }

    private void uploadPosterAndSave(Event event) {
        if (selectedImageUri == null) {
            saveEventToFirestore(event, null);
            return;
        }

        StorageReference ref = storage.getReference()
                .child("event_posters/" + System.currentTimeMillis() + ".jpg");

        UploadTask uploadTask = ref.putFile(selectedImageUri);

        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
            progressDialog.setMessage("Uploading image... " + (int) progress + "%");
        }).addOnSuccessListener(taskSnapshot ->
                ref.getDownloadUrl().addOnSuccessListener(uri ->
                        saveEventToFirestore(event, uri.toString())
                )
        ).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void saveEventToFirestore(Event event, String imageUrl) {
        if (imageUrl != null) event.setPosterImageUrl(imageUrl);

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            event.setOrganizerId(user.getUid());
            event.setOrganizerName(user.getDisplayName() != null ?
                    user.getDisplayName() : "Anonymous");
        }

        // Use a LinkedHashMap to preserve insertion order.
        Map<String, Object> eventData = event.toMap();

        // After getting the map with good order, add the map to Firestore instead of the 'event' object
        db.collection("events")
                .add(eventData) // Use the map here
                .addOnSuccessListener(doc -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error creating event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
