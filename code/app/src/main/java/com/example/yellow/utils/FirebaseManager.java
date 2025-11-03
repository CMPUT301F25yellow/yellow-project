package com.example.yellow.utils;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.example.yellow.organizers.Event;

import java.util.UUID;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static final String EVENTS_COLLECTION = "events";
    private static final String STORAGE_EVENTS_PATH = "event_posters";

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;

    private static FirebaseManager instance;

    private FirebaseManager() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // Create Event
    public void createEvent(Event event, CreateEventCallback callback) {
        // Get current user
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        // Set organizer info
        event.setOrganizerId(currentUser.getUid());
        event.setOrganizerName(currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "Unknown");

        // Add to Firestore
        db.collection(EVENTS_COLLECTION)
                .add(event.toMap())
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Event created with ID: " + documentReference.getId());
                    event.setId(documentReference.getId());
                    callback.onSuccess(event);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating event", e);
                    callback.onFailure(e);
                });
    }

    // Upload Image and Create Event
    public void uploadImageAndCreateEvent(Uri imageUri, Event event,
                                          CreateEventCallback callback,
                                          UploadProgressCallback progressCallback) {
        if (imageUri == null) {
            // No image, just create event
            createEvent(event, callback);
            return;
        }

        // Create unique filename
        String filename = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storage.getReference()
                .child(STORAGE_EVENTS_PATH)
                .child(filename);

        // Upload image
        UploadTask uploadTask = imageRef.putFile(imageUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) /
                    taskSnapshot.getTotalByteCount();
            if (progressCallback != null) {
                progressCallback.onProgress((int) progress);
            }
        }).addOnSuccessListener(taskSnapshot -> {
            // Get download URL
            imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                event.setPosterImageUrl(downloadUri.toString());
                createEvent(event, callback);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting download URL", e);
                callback.onFailure(e);
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error uploading image", e);
            callback.onFailure(e);
        });
    }

    // Update Event
    public void updateEvent(String eventId, Event event, UpdateEventCallback callback) {
        db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .set(event.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event updated successfully");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating event", e);
                    callback.onFailure(e);
                });
    }

    // Delete Event
    public void deleteEvent(String eventId, DeleteEventCallback callback) {
        db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event deleted successfully");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting event", e);
                    callback.onFailure(e);
                });
    }

    // Callbacks
    public interface CreateEventCallback {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }

    public interface UpdateEventCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface DeleteEventCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface UploadProgressCallback {
        void onProgress(int progress);
    }
}
