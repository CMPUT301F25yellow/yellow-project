package com.example.yellow.organizers.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment; // CHANGED FROM FRAGMENT
import androidx.lifecycle.ViewModelProvider;

import com.example.yellow.R;
import com.example.yellow.organizers.Event;
import com.example.yellow.organizers.EventViewModel;
import com.example.yellow.organizers.ViewEventActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class EventSettingsFragment extends DialogFragment {

    private ImageView posterImageView;
    private Button btnChangePoster;
    private String eventId;
    private EventViewModel eventViewModel;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public static EventSettingsFragment newInstance(String eventId) {
        EventSettingsFragment fragment = new EventSettingsFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Scoped to the Activity, so it's the SAME instance as the one in ViewEventActivity
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
        initializeImagePicker();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_settings, container, false);

        posterImageView = view.findViewById(R.id.posterImageView);
        btnChangePoster = view.findViewById(R.id.btnChangePoster);

        btnChangePoster.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // THIS IS THE KEY FIX FOR THE FRAGMENT:
        // Observe the ViewModel. This will set the initial poster AND update it if another change happens.
        eventViewModel.getEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                // When the dialog opens, this sets the current poster from the single source of truth.
                setImageFromDataUri(posterImageView, event.getPosterUrl());
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
            // Optional: set a transparent background if your layout has rounded corners
            // dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }


    private void initializeImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri newImageUri = result.getData().getData();
                        if (newImageUri != null) {
                            updatePoster(newImageUri);
                        }
                    }
                }
        );
    }

    private void updatePoster(Uri newImageUri) {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Event ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Processing image...", Toast.LENGTH_SHORT).show();
        btnChangePoster.setEnabled(false);

        try {
            // Use the new, safe method to encode the image
            String newDataUri = encodeImageUriToDataUri(newImageUri);

            Toast.makeText(getContext(), "Updating poster...", Toast.LENGTH_SHORT).show();
            FirebaseFirestore.getInstance().collection("events").document(eventId)
                    .update("posterImageUrl", newDataUri)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Poster updated successfully!", Toast.LENGTH_LONG).show();
                        dismiss(); // The ViewModel's listener will handle the UI update
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to update poster: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnChangePoster.setEnabled(true);
                    });

        } catch (Exception e) {
            // This will now catch OutOfMemoryError, FileNotFoundException, etc.
            Log.e("UpdatePoster", "Failed to encode or upload image", e);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnChangePoster.setEnabled(true);
        }
    }

    /**
     * Safely reads an image by Uri, downsamples it to prevent memory errors,
     * compresses it, and returns it as a Base64 data URI string.
     *
     * @param uri the content Uri of the image
     * @return a string like "data:image/jpeg;base64,..."
     * @throws Exception if the image cannot be opened, decoded, or processed
     */
    private String encodeImageUriToDataUri(Uri uri) throws Exception {
        final int MAX_DIMENSION = 1280; // Max width/height for the poster, e.g., 1280px
        final int MAX_DOC_BYTES = 900 * 1024; // Firestore document size limit is ~1MB, so we aim for under 900KB
        final int MIN_JPEG_QUALITY = 40;

        InputStream inputStream = null;
        try {
            // --- Step 1: Decode bounds to check image size without loading it into memory ---
            inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) throw new FileNotFoundException("Cannot open input stream from URI.");

            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, boundsOptions);
            inputStream.close(); // Close the stream

            int originalWidth = boundsOptions.outWidth;
            int originalHeight = boundsOptions.outHeight;
            if (originalWidth <= 0 || originalHeight <= 0) {
                throw new IllegalArgumentException("Invalid image dimensions.");
            }

            // --- Step 2: Calculate sampling size to load a scaled-down version ---
            int sampleSize = 1;
            while (originalWidth / sampleSize > MAX_DIMENSION || originalHeight / sampleSize > MAX_DIMENSION) {
                sampleSize *= 2;
            }

            // --- Step 3: Decode the image with the calculated sample size ---
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = sampleSize;
            // Use a memory-friendly config
            bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;

            inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) throw new FileNotFoundException("Cannot open input stream from URI a second time.");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, bitmapOptions);
            if (bitmap == null) throw new IllegalArgumentException("Failed to decode bitmap. Image may be corrupt.");

            // --- Step 4: Compress the downsampled bitmap until it's under our size limit ---
            int quality = 90; // Start with high quality
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] jpgBytes;

            do {
                baos.reset(); // Clear the stream for the next compression attempt
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                jpgBytes = baos.toByteArray();
                quality -= 5; // Reduce quality for the next loop if needed
            } while (jpgBytes.length > MAX_DOC_BYTES && quality >= MIN_JPEG_QUALITY);

            if (jpgBytes.length > MAX_DOC_BYTES) {
                throw new RuntimeException("Image is too large to save even after compression.");
            }

            // --- Step 5: Convert to Base64 and return the Data URI ---
            String base64 = Base64.encodeToString(jpgBytes, Base64.NO_WRAP);
            return "data:image/jpeg;base64," + base64;

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {}
            }
        }
    }


    private void setImageFromDataUri(@Nullable ImageView view, @Nullable String dataUri) {
        // This method is fine as is
        if (view == null || dataUri == null || dataUri.isEmpty()) return;
        try {
            String base64 = dataUri.startsWith("data:") ? dataUri.substring(dataUri.indexOf(',') + 1) : dataUri;
            byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp != null) view.setImageBitmap(bmp);
        } catch (Exception ignored) {}
    }
}
