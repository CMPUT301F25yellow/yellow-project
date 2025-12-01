package com.example.yellow.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.yellow.R;
import com.example.yellow.organizers.Event;
import com.example.yellow.organizers.ViewEventActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying the user's event history.
 * 
 * This adapter displays a list of events where the user is a participant.
 * The cards are read-only and do not allow interaction or navigation.
 * 
 * @author Tabrez
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<Event> events = new ArrayList<>();
    private final Context context;

    /**
     * Constructs a new HistoryAdapter.
     *
     * @param context Application context for inflating layouts and loading images
     */
    public HistoryAdapter(Context context) {
        this.context = context;
    }

    /**
     * Updates the list of events displayed in the adapter.
     *
     * @param newEvents The new list of Event objects to display
     */
    public void setEvents(List<Event> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    /**
     * Inflates the layout for a new event item view.
     *
     * @param parent   The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return A new ViewHolder that holds the View for each event item
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds data to the views in the ViewHolder.
     * Configures the event card to be read-only (non-clickable).
     *
     * @param holder   The ViewHolder which should be updated
     * @param position The position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);

        holder.title.setText(event.getName());
        holder.details.setText(event.getFormattedDateAndLocation());

        if (event.getPosterImageUrl() != null && !event.getPosterImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(event.getPosterImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_icon)
                    .into(holder.image);
        } else {
            holder.image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.image.setImageResource(R.drawable.ic_image_icon);
        }

        // In History, we don't want the action button.
        holder.actionButton.setVisibility(View.GONE);

        // Also make the whole card non-clickable as per user request
        holder.itemView.setOnClickListener(null);
        holder.itemView.setClickable(false);
    }

    /**
     * Returns the total number of events in the adapter.
     *
     * @return The size of the events list
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder class for event item views.
     * Holds references to the views within each list item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView details;
        Button actionButton;

        /**
         * Constructs a new ViewHolder and finds view references.
         *
         * @param itemView The root view of the event item layout
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.eventImage);
            title = itemView.findViewById(R.id.eventTitle);
            details = itemView.findViewById(R.id.eventDetails);
            actionButton = itemView.findViewById(R.id.eventButton);
        }
    }
}
