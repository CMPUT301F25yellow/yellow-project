package com.example.yellow.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.yellow.R;
import com.example.yellow.organizers.EventViewModel;

public class QrFragmentAfterCreateEvent extends Fragment {

    private ImageView qrImageView;
    private TextView tvDeepLink;
    private EventViewModel eventViewModel;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null.
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the SAME ViewModel instance as the parent activity
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_qr_creation, container, false);

        qrImageView = view.findViewById(R.id.qrImageView);
        tvDeepLink = view.findViewById(R.id.tvDeepLink);

        // Observe the ViewModel for the event data
        eventViewModel.getEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                // Once we have the event, display the QR code and deep link
                displayQrCode(event.getQrImagePng());
                if (event.getQrDeepLink() != null) {
                    tvDeepLink.setText(event.getQrDeepLink());
                } else {
                    tvDeepLink.setText("No deep link found.");
                }
            } else {
                // Handle case where event data is not available
                qrImageView.setImageBitmap(null); // Clear previous image
                tvDeepLink.setText("");
                Toast.makeText(getContext(), "QR Code not available.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    /**
     * Display the QR code in the ImageView.
     * @param qrDataUri
     */
    private void displayQrCode(@Nullable String qrDataUri) {
        if (qrDataUri == null || qrDataUri.trim().isEmpty()) {
            // Handle the case where the QR code data is missing
            Toast.makeText(getContext(), "This event does not have a QR code.", Toast.LENGTH_SHORT).show();

            // Explicitly hide the ImageView so the white square doesn't appear
            qrImageView.setVisibility(View.GONE);

            // If you have a placeholder in your layout, you could show it here instead.
            // e.g., noQrCodeTextView.setVisibility(View.VISIBLE);
            return;
        }

        // If we have data, make sure the ImageView is visible
        qrImageView.setVisibility(View.VISIBLE);

        try {
            // Strip the "data:image/png;base64," header
            String base64String = qrDataUri.substring(qrDataUri.indexOf(',') + 1);

            // Decode the Base64 string into a byte array
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

            // Convert the byte array into a Bitmap
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            if (decodedBitmap != null) {
                // Set the Bitmap to the ImageView
                qrImageView.setImageBitmap(decodedBitmap);
            } else {
                // The data was present but could not be decoded into a valid image
                throw new IllegalArgumentException("Decoded bitmap is null.");
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to decode QR code.", Toast.LENGTH_LONG).show();
            // Set an error icon and make sure the view is still visible to show the error
            qrImageView.setVisibility(View.VISIBLE);
            qrImageView.setImageResource(R.drawable.my_image); // 'my_image' is from your file
            // Correct
            Log.e("QrFragment", "Error decoding Base64 QR code", e);}
    }
}
