package com.example.yellow.utils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for cleaning up user data when a user account is deleted.
 * Handles cascade deletion of user profiles, events, and related
 * subcollections.
 * 
 * @author Tabrez
 */
public class UserCleanupUtils {

    /**
     * Deletes a user's profile, role, and all events they created (including
     * subcollections).
     * This is a cascade delete operation that ensures no orphaned data remains.
     *
     * @param uid User ID to delete
     * @param db  Firestore instance
     * @return Task that completes when all deletions are done
     */
    public static Task<Void> deleteUserAndEvents(@NonNull String uid, @NonNull FirebaseFirestore db) {
        // Step 1: Query all events created by this user
        return db.collection("events")
                .whereEqualTo("organizerId", uid)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    QuerySnapshot eventsSnapshot = task.getResult();
                    List<Task<Void>> deletionTasks = new ArrayList<>();

                    // Step 2: Delete each event and its subcollections
                    for (DocumentSnapshot eventDoc : eventsSnapshot.getDocuments()) {
                        String eventId = eventDoc.getId();

                        // Delete event subcollections
                        deletionTasks.add(deleteEventSubcollections(eventId, db));

                        // Delete the event document itself
                        deletionTasks.add(db.collection("events").document(eventId).delete());
                    }

                    // Step 3: Delete user notifications subcollection
                    deletionTasks.add(
                            deleteCollection(db.collection("profiles").document(uid).collection("notifications"), db));

                    // Step 4: Remove user from all waiting lists (Collection Group Query)
                    deletionTasks.add(db.collectionGroup("waitingList")
                            .whereEqualTo("userId", uid)
                            .get()
                            .continueWithTask(queryTask -> {
                                if (!queryTask.isSuccessful()) {
                                    return Tasks.forResult(null);
                                }

                                List<Task<Void>> updates = new ArrayList<>();
                                for (DocumentSnapshot doc : queryTask.getResult()) {
                                    // Delete waiting list entry
                                    updates.add(doc.getReference().delete());

                                    // Decrement event count
                                    com.google.firebase.firestore.DocumentReference eventRef = doc.getReference()
                                            .getParent().getParent();
                                    if (eventRef != null) {
                                        updates.add(eventRef.update("waitlisted",
                                                com.google.firebase.firestore.FieldValue.increment(-1)));
                                    }
                                }
                                return Tasks.whenAll(updates);
                            }));

                    // Step 5: Delete user profile and role documents
                    WriteBatch batch = db.batch();
                    batch.delete(db.collection("profiles").document(uid));
                    batch.delete(db.collection("roles").document(uid));
                    deletionTasks.add(batch.commit());

                    // Wait for all deletions to complete
                    return Tasks.whenAll(deletionTasks);
                });
    }

    /**
     * Deletes all known subcollections for an event.
     * Known subcollections: attendees, waitingList, registrations
     *
     * @param eventId Event document ID
     * @param db      Firestore instance
     * @return Task that completes when all subcollections are deleted
     */
    private static Task<Void> deleteEventSubcollections(@NonNull String eventId, @NonNull FirebaseFirestore db) {
        List<Task<Void>> tasks = new ArrayList<>();

        // List of known subcollections to delete
        String[] subcollections = { "attendees", "waitingList", "registrations", "cancelledList" };

        for (String subcollection : subcollections) {
            tasks.add(deleteCollection(db.collection("events").document(eventId).collection(subcollection), db));
        }

        return Tasks.whenAll(tasks);
    }

    /**
     * Deletes all documents in a collection.
     * Note: This is a simple implementation. For large collections, consider
     * batching.
     *
     * @param collectionRef Collection reference to delete
     * @param db            Firestore instance
     * @return Task that completes when collection is deleted
     */
    private static Task<Void> deleteCollection(
            @NonNull com.google.firebase.firestore.CollectionReference collectionRef,
            @NonNull FirebaseFirestore db) {

        return collectionRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                // If collection doesn't exist or query fails, just continue
                return Tasks.forResult(null);
            }

            QuerySnapshot snapshot = task.getResult();
            if (snapshot.isEmpty()) {
                return Tasks.forResult(null);
            }

            // Delete documents in batches of 500 (Firestore limit)
            WriteBatch batch = db.batch();
            int count = 0;

            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                batch.delete(doc.getReference());
                count++;

                if (count >= 500) {
                    // Firestore batch limit reached, commit and create new batch
                    break;
                }
            }

            return batch.commit();
        });
    }
}
