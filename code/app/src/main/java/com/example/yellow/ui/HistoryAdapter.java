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

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<Event> events = new ArrayList<>();
    private final Context context;

    public HistoryAdapter(Context context) {
        this.context = context;
    }

    public void setEvents(List<Event> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new ViewHolder(view);
    }

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

        // In History, "Join Waiting List" doesn't make much sense if they are already
        // joined.
        // We can change the text to "View Details" or hide it.
        // For now, let's make it "View Details"
        holder.actionButton.setText("View Details");
        holder.actionButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, ViewEventActivity.class);
            intent.putExtra("eventId", event.getId());
            context.startActivity(intent);
        });

        // Also make the whole card clickable
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ViewEventActivity.class);
            intent.putExtra("eventId", event.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView details;
        Button actionButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.eventImage);
            title = itemView.findViewById(R.id.eventTitle);
            details = itemView.findViewById(R.id.eventDetails);
            actionButton = itemView.findViewById(R.id.eventButton);
        }
    }
}
