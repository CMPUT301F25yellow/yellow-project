
package com.example.yellow.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.yellow.R;
import com.example.yellow.utils.QrUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment for scanning QR codes to view event details.
 * @author Kien Tran - kht
 */
public class QrScanFragment extends Fragment {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private BarcodeScanner scanner;
    private Camera camera;
    volatile boolean isScanning = true;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required to scan QR codes.", Toast.LENGTH_SHORT).show();
                }
            });

    /**
     * Inflates the QR scan layout.
     *
     * @param inflater LayoutInflater used to inflate the view.
     * @param container Optional parent container.
     * @param savedInstanceState Previously saved state, if any.
     * @return The root view for this fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qrscan, container, false);
    }

    /**
     * Sets up UI adjustments such as the status bar color and insets.
     *
     * @param v The root view of the fragment.
     * @param savedInstanceState Previously saved state, if any.
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        isScanning = true; // Reset scanning state

        // Match the status bar color with the header
        requireActivity().getWindow().setStatusBarColor(
                ContextCompat.getColor(requireContext(), R.color.surface_dark)
        );

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

        previewView = v.findViewById(R.id.viewFinder);
        cameraExecutor = Executors.newSingleThreadExecutor();
        scanner = BarcodeScanning.getClient();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Starts the camera preview and starts the QR code scanner.
     * This method is called when the camera permission is granted.
     *
     */
    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    if (!isScanning) {
                        image.close();
                        return;
                    }

                    if (image.getImage() != null) {
                        InputImage inputImage = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());
                        scanner.process(inputImage)
                                .addOnSuccessListener(barcodes -> {

                                    if (!barcodes.isEmpty()) {
                                        isScanning = false; // Pause scanning while we process the result
                                        String rawValue = barcodes.get(0).getRawValue();
                                        Log.i("QRScanFragment", "QR Code detected: " + rawValue);

                                        // Handle the navigation
                                        handleScannedQrCode(rawValue);

                                    }
                                })
                                .addOnFailureListener(e -> Log.e("QRScanFragment", "Error scanning QR code", e))
                                .addOnCompleteListener(task -> image.close());
                    } else {
                        image.close();
                    }
                });

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("QRScanFragment", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * Handles the scanned QR code and navigates to the event details screen (fragment).
     *
     * @param rawValue The raw value of the scanned QR code.
     */
    void handleScannedQrCode(String rawValue) {
        String eventId = QrUtils.getEventIdFromUri(rawValue);

        if (eventId != null) {
            // On a valid scan, we navigate away, so no need to unpause
            Log.d("QRScanFragment", "Navigating to event: " + eventId);
            Bundle bundle = new Bundle();
            bundle.putString("qr_code_data", eventId);

            EventDetailsFragment eventDetailsFragment = new EventDetailsFragment();
            eventDetailsFragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, eventDetailsFragment)
                    .addToBackStack(null)
                    .commit();

        } else {
            // On failure, show a toast and then unpause after a delay
            Log.w("QRScanFragment", "Scanned QR code is not a valid event link: " + rawValue);
            Toast.makeText(getContext(), "Not a valid event QR code.", Toast.LENGTH_SHORT).show();

            // Resume scanning after 2 second delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> isScanning = true, 2000);
        }
    }

    /**
     * Cleans up resources when the fragment is destroyed.
     * This includes shutting down the camera executor and closing the scanner.
     * @see Fragment#onDestroyView()
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (scanner != null) {
            scanner.close();
        }
    }
}
