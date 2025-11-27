package com.example.yellow.ui.ViewEvent.Map;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.yellow.R;
import com.example.yellow.organizers.EventViewModel;
import com.example.yellow.ui.ViewEvent.Map.MapViewModel;
import com.example.yellow.users.WaitingUser; // Import the correct class
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";
    private GoogleMap googleMap;
    private MapViewModel mapViewModel;
    private EventViewModel eventViewModel; // To get the event ID from the activity

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModels
        mapViewModel = new ViewModelProvider(this).get(com.example.yellow.ui.ViewEvent.Map.MapViewModel.class);
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);

        // Setup the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        Log.d(TAG, "Map is ready.");
        setupObservers();
    }

    /**
     * Sets up observers for the ViewModel.
     * This method is called after the map is ready.
     */
    private void setupObservers() {
        // First, get the event ID from the shared EventViewModel
        eventViewModel.getEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null && event.getId() != null) {
                // Tell our MapViewModel to load the data
                mapViewModel.loadEntrantLocations(event.getId());
            }
        });

        // Now, observe our MapViewModel for the list of entrants
        mapViewModel.getEntrantsWithLocation().observe(getViewLifecycleOwner(), this::updateMapMarkers);

        // Observe for any error messages
        mapViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Updates the map markers with the list of entrants.
     * @param entrants
     */
    private void updateMapMarkers(List<WaitingUser> entrants) {
        if (googleMap == null) return; // Don't do anything if the map isn't ready
        googleMap.clear(); // Clear any old markers

        if (entrants == null || entrants.isEmpty()) {
            Toast.makeText(getContext(), "No entrants have shared their location.", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        int markersAdded = 0;

        // Loop through the WaitingUser objects
        for (WaitingUser entrant : entrants) {
            LatLng location = new LatLng(entrant.getLatitude(), entrant.getLongitude());
            googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(entrant.getName())); // Use the name from the WaitingUser object
            boundsBuilder.include(location);
            markersAdded++;
        }

        Log.d(TAG, "Added " + markersAdded + " markers to the map.");

        // Animate the camera to fit all the markers
        if (markersAdded > 0) {
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 150; // Padding in pixels
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }
    }
}
