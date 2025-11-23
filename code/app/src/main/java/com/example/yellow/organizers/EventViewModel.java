package com.example.yellow.organizers;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;

public class EventViewModel extends ViewModel {

    private final MutableLiveData<Event> _event = new MutableLiveData<>();
    public LiveData<Event> getEvent() {
        return _event;
    }

    public void loadEvent(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _event.postValue(null);
            return;
        }

        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        // Handle error
                        _event.postValue(null);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if(event != null) {
                            event.setId(documentSnapshot.getId()); // IMPORTANT: Set the document ID
                        }
                        _event.postValue(event);
                    } else {
                        _event.postValue(null);
                    }
                });
    }
}
