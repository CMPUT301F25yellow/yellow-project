package com.example.yellow.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.function.Consumer;

public class LocationHelper {

    private final FragmentActivity activity;
    private final FusedLocationProviderClient fusedLocationClient;
    private final ActivityResultLauncher<String> requestPermissionLauncher;
    private Consumer<Location> onLocationResult;

    public LocationHelper(FragmentActivity activity, Consumer<Location> onLocationResult) {
        this.activity = activity;
        this.onLocationResult = onLocationResult;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

        // This launcher handles the result of the permission request dialog
        this.requestPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission was granted, now get the location
                        requestLocationUpdates();
                    } else {
                        // Permission was denied, report failure by calling the callback with null
                        Toast.makeText(activity, "Location permission is required for this feature.", Toast.LENGTH_LONG).show();
                        this.onLocationResult.accept(null);
                    }
                }
        );
    }

    public void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // We already have permission, proceed to get location
            requestLocationUpdates();
        } else {
            // We don't have permission, launch the request dialog
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void requestLocationUpdates() {
        // Suppress the warning because we checked for permission in getCurrentLocation()
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(2000)
                    .setMaxUpdateDelayMillis(10000)
                    .build();

            // We only need one location update, so we create a callback that removes itself.
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

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            // This should not happen if permission check is correct
            onLocationResult.accept(null);
        }
    }
}
