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

/**
 * ViewModel for the MapFragment.
 * @author Kien Tran - kht
 */
public class MapViewModel extends ViewModel {
    private static final String TAG = "MapViewModel";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // This LiveData will hold the list of entrants with their locations.
    private final MutableLiveData<List<WaitingUser>> entrantsWithLocation = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /**
     * Returns the LiveData for the list of entrants with their locations.
     * @return The LiveData for the list of entrants with their locations.
     */
    public LiveData<List<WaitingUser>> getEntrantsWithLocation() {
        return entrantsWithLocation;
    }

    /**
     * Returns the LiveData for error messages.
     * @return The LiveData for error messages.
     */
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

        Log.d(TAG, "Fetching waiting-list users for event: " + eventId);

        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No waiting users found for this event.");
                        entrantsWithLocation.setValue(new ArrayList<>());
                        return;
                    }

                    List<WaitingUser> usersWithLocation = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        WaitingUser user = document.toObject(WaitingUser.class);

                        // Only include users with valid location
                        if (user != null &&
                                user.getLatitude() != null &&
                                user.getLongitude() != null &&
                                user.getLatitude() != 0 &&
                                user.getLongitude() != 0) {
                            usersWithLocation.add(user);
                        }
                    }

                    Log.d(TAG, "Found " + usersWithLocation.size() + " waiting users with location data.");
                    entrantsWithLocation.setValue(usersWithLocation);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching waiting users", e);
                    errorMessage.setValue("Failed to load waiting user locations.");
                });
    }

}