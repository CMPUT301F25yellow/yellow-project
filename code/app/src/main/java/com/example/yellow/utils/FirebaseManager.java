package com.example.yellow.utils;

import android.net.Uri;
import android.util.Log;

import com.example.yellow.organizers.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.List;
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

    // ---------------- CREATE EVENT ----------------
    public void createEvent(Event event, CreateEventCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();

        // Ensure user is authenticated (anonymous if needed)
        if (currentUser == null) {
            auth.signInAnonymously()
                    .addOnSuccessListener(result -> createEvent(event, callback))
                    .addOnFailureListener(callback::onFailure);
            return;
        }

        // Add organizer info
        event.setOrganizerId(currentUser.getUid());
        event.setOrganizerName(currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "Unknown");

        event.setCreatedAt(Timestamp.now());

        db.collection(EVENTS_COLLECTION)
                .add(event)
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

    // ---------------- UPLOAD IMAGE & CREATE EVENT ----------------
    public void uploadImageAndCreateEvent(Uri imageUri, Event event,
                                          CreateEventCallback callback,
                                          UploadProgressCallback progressCallback) {
        if (imageUri == null) {
            createEvent(event, callback);
            return;
        }

        String filename = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storage.getReference()
                .child(STORAGE_EVENTS_PATH)
                .child(filename);

        UploadTask uploadTask = imageRef.putFile(imageUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) /
                    taskSnapshot.getTotalByteCount();
            if (progressCallback != null) {
                progressCallback.onProgress((int) progress);
            }
        }).addOnSuccessListener(taskSnapshot ->
                imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    event.setPosterImageUrl(downloadUri.toString());
                    createEvent(event, callback);
                }).addOnFailureListener(callback::onFailure)
        ).addOnFailureListener(callback::onFailure);
    }

    // ---------------- FETCH EVENTS ----------------
    public void getAllEvents(GetEventsCallback callback) {
        db.collection(EVENTS_COLLECTION)
                .orderBy("startDate", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    callback.onSuccess(querySnapshot.getDocuments());
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ---------------- INTERFACES ----------------
    public interface CreateEventCallback {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }

    public interface UploadProgressCallback {
        void onProgress(int progress);
    }

    public void getEventsByOrganizer(String organizerId, GetEventsCallback callback) {
        db.collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(querySnapshot -> callback.onSuccess(querySnapshot.getDocuments()))
                .addOnFailureListener(callback::onFailure);
    }

    public interface GetEventsCallback {
        void onSuccess(List<DocumentSnapshot> documents);
        void onFailure(Exception e);
    }
}
