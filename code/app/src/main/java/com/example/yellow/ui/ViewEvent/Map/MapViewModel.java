package com.example.yellow.ui.ViewEvent.Map;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.yellow.users.WaitingUser; // Correctly imports your WaitingUser class
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MapViewModel extends ViewModel {
    private static final String TAG = "MapViewModel";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // This LiveData will hold the list of entrants with their locations.
    private final MutableLiveData<List<WaitingUser>> entrantsWithLocation = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<List<WaitingUser>> getEntrantsWithLocation() {
        return entrantsWithLocation;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Starts the one-step process to load locations for all event entrants.
     * @param eventId The ID of the event to load entrants for.
     */
    public void loadEntrantLocations(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            errorMessage.setValue("Event ID is missing.");
            return;
        }

        Log.d(TAG, "Fetching entrants for event: " + eventId);

        // This is now a simple, one-step fetch.
        db.collection("events").document(eventId).collection("entrants").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No entrants found for this event.");
                        entrantsWithLocation.setValue(new ArrayList<>()); // Post empty list
                        return;
                    }

                    List<WaitingUser> usersWithLocation = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        WaitingUser entrant = document.toObject(WaitingUser.class);

                        // Important check: only add users who have location data
                        if (entrant != null && entrant.getLatitude() != null && entrant.getLongitude() != null &&
                                entrant.getLatitude() != 0 && entrant.getLongitude() != 0) {
                            usersWithLocation.add(entrant);
                        }
                    }

                    Log.d(TAG, "Found " + usersWithLocation.size() + " entrants with location data.");
                    // Post the final list to LiveData for the fragment to observe
                    entrantsWithLocation.setValue(usersWithLocation);

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching entrants", e);
                    errorMessage.setValue("Failed to load entrant locations.");
                });
    }
}