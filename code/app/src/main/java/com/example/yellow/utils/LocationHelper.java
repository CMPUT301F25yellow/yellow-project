package com.example.yellow.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.function.Consumer;

/**
 * Helper class to get the user's current location, handling runtime permission.
 * IMPORTANT: The ActivityResult registration is done against an ActivityResultCaller,
 * which should be the Fragment (not the Activity). That avoids the "register while
 * RESUMED" crash.
 */
public class LocationHelper {

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final ActivityResultLauncher<String> requestPermissionLauncher;
    private final Consumer<Location> onLocationResult;

    /**
     * Constructor.
     * @param caller: ActivityResultCaller (Fragment)
     * @param context: Context
     * @param onLocationResult: Callback for the location
     */
    public LocationHelper(@NonNull ActivityResultCaller caller,
                          @NonNull Context context,
                          @NonNull Consumer<Location> onLocationResult) {

        this.context = context;
        this.onLocationResult = onLocationResult;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        // Register for the permission result with the Fragment (caller), not the Activity.
        this.requestPermissionLauncher =
                caller.registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            if (isGranted) {
                                requestLocationUpdates();
                            } else {
                                Toast.makeText(
                                        context,
                                        "Location permission is required for this feature.",
                                        Toast.LENGTH_LONG
                                ).show();
                                this.onLocationResult.accept(null);
                            }
                        }
                );
    }

    /**
     * Public entry point – call this to start the "get location" flow.
     */
    public void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, go straight to location request
            requestLocationUpdates();
        } else {
            // Ask for permission
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Internal helper that actually requests a single high-accuracy location update.
     */
    private void requestLocationUpdates() {
        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setInterval(5000)
                    .setFastestInterval(2000)
                    .setNumUpdates(1); // One-shot location

            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null || locationResult.getLastLocation() == null) {
                        onLocationResult.accept(null);
                        return;
                    }
                    // Report success by passing the location to the callback
                    onLocationResult.accept(locationResult.getLastLocation());
                    // Stop listening for updates to save battery
                    fusedLocationClient.removeLocationUpdates(this);
                }
            };

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        } catch (SecurityException e) {
            // This should not happen if permission check is correct
            onLocationResult.accept(null);
        }
    }
}
