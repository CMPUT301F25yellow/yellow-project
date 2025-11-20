package com.example.yellow.utils;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

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
import java.util.Map;
import java.util.UUID;

/**
 * Simple helper for Firebase tasks used by the app:
 * <ul>
 *     <li>Create events in Firestore</li>
 *     <li>Upload poster images to Firebase Storage</li>
 *     <li>Query events</li>
 *     <li>Patch/update event fields</li>
 * </ul>
 *
 * <p>Use {@link #getInstance()} to access the singleton.</p>
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static final String EVENTS_COLLECTION = "events";
    private static final String STORAGE_EVENTS_PATH = "event_posters";

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseAuth auth;

    private static FirebaseManager instance;

    private FirebaseManager() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Returns the single shared instance of this manager.
     *
     * @return the {@link FirebaseManager} instance
     */
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    /**
     * Creates a new event document in Firestore. If the user is not signed in,
     * signs in anonymously first.
     *
     * @param event    the event to create
     * @param callback callback for success or failure
     */
    public void createEvent(Event event, CreateEventCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            auth.signInAnonymously()
                    .addOnSuccessListener(result -> createEvent(event, callback))
                    .addOnFailureListener(callback::onFailure);
            return;
        }

        event.setOrganizerId(currentUser.getUid());
        event.setOrganizerName(currentUser.getDisplayName() != null
                ? currentUser.getDisplayName() : "Unknown");
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

    /**
     * Uploads an image to Firebase Storage and then creates the event in Firestore.
     * If {@code imageUri} is null, skips upload and just creates the event.
     *
     * @param imageUri          the local image to upload (may be {@code null})
     * @param event             the event to create
     * @param callback          callback for create success or failure
     * @param progressCallback  upload progress callback (may be {@code null})
     */
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
            double progress = (100.0 * taskSnapshot.getBytesTransferred())
                    / taskSnapshot.getTotalByteCount();
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

    /**
     * Loads all events ordered by start date.
     *
     * @param callback callback receiving the list of documents or an error
     */
    public void getAllEvents(GetEventsCallback callback) {
        db.collection(EVENTS_COLLECTION)
                .orderBy("startDate", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        callback.onSuccess(querySnapshot.getDocuments()))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Loads events created by a specific organizer.
     *
     * @param organizerId the organizer's user ID
     * @param callback    callback receiving the results or an error
     */
    public void getEventsByOrganizer(String organizerId, GetEventsCallback callback) {
        db.collection(EVENTS_COLLECTION)
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        callback.onSuccess(querySnapshot.getDocuments()))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Partially updates fields on an existing event document.
     *
     * @param docId   the document ID to update
     * @param patch   map of fields to update
     * @param cb      callback for success or failure
     */
    public void updateEvent(@NonNull String docId,
                            @NonNull Map<String, Object> patch,
                            @NonNull SimpleCallback cb) {
        Log.d("FirebaseUpdate", "Updating /events/" + docId + " with: " + patch);

        FirebaseFirestore.getInstance()
                .collection(EVENTS_COLLECTION)
                .document(docId)
                .update(patch)
                .addOnSuccessListener(unused -> {
                    Log.d("FirebaseUpdate", "Update success for /events/" + docId);
                    cb.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseUpdate", "Update FAILED for /events/" + docId, e);
                    cb.onFailure(e);
                });
    }

    /**
     * Updates the poster for a specific event. It uploads the new image and then
     * updates the 'posterImageUrl' field in the corresponding Firestore document.
     *
     * @param eventId           The ID of the event document to update.
     * @param newImageUri       The local URI of the new poster image.
     * @param callback          A simple callback for success or failure.
     * @param progressCallback  Optional callback for upload progress.
     */
    public void updateEventPoster(@NonNull String eventId, @NonNull Uri newImageUri,
                                  @NonNull SimpleCallback callback,
                                  @NonNull UploadProgressCallback progressCallback) {

        String filename = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storage.getReference()
                .child(STORAGE_EVENTS_PATH)
                .child(filename);

        UploadTask uploadTask = imageRef.putFile(newImageUri);

        // 1. Handle Progress (Optional but good practice)
        uploadTask.addOnProgressListener(taskSnapshot -> {
            if (progressCallback != null) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                progressCallback.onProgress((int) progress);
            }
        });

        // 2. Chain the upload to get the URL
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return imageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 3. Once we have the new URL, update the Firestore document
                Uri downloadUri = task.getResult();
                db.collection(EVENTS_COLLECTION).document(eventId)
                        .update("posterImageUrl", downloadUri.toString())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Event poster updated successfully for event: " + eventId);
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update poster URL in Firestore.", e);
                            callback.onFailure(e);
                        });
            } else {
                // This handles failures in the upload or getDownloadUrl steps
                Log.e(TAG, "Image upload or URL retrieval failed.", task.getException());
                callback.onFailure(task.getException());
            }
        });
    }

    /**
     * Callback for event creation.
     */
    public interface CreateEventCallback {
        /**
         * Called with the created document ID (if provided by the caller).
         *
         * @param docId the new document ID
         */
        void onSuccess(String docId);

        /**
         * Called with the created {@link Event}.
         *
         * @param event the created event
         */
        void onSuccess(Event event);

        /**
         * Called when creation fails.
         *
         * @param e the error
         */
        void onFailure(Exception e);
    }

    /**
     * Progress updates while uploading a file.
     */
    public interface UploadProgressCallback {
        /**
         * Called with upload progress [0..100].
         *
         * @param progress percentage complete
         */
        void onProgress(int progress);
    }

    /**
     * Callback for event query results.
     */
    public interface GetEventsCallback {
        /**
         * Called with the fetched documents.
         *
         * @param documents list of event document snapshots
         */
        void onSuccess(List<DocumentSnapshot> documents);

        /**
         * Called when the query fails.
         *
         * @param e the error
         */
        void onFailure(Exception e);
    }

    /**
     * Simple callback with success/failure only.
     */
    public interface SimpleCallback {
        /** Called when the operation succeeds. */
        void onSuccess();

        /**
         * Called when the operation fails.
         *
         * @param e the error
         */
        void onFailure(Exception e);
    }
}
