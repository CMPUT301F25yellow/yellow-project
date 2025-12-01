package com.example.yellow.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.yellow.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying poster images in a grid layout in the
 * Manage Images screen.
 * 
 * This adapter displays event poster images with associated event information
 * and provides
 * a delete button for each image. Images are loaded using Glide and displayed
 * in a 2-column
 * grid managed by the parent fragment's GridLayoutManager.
 * 
 * @author Tabrez
 */
public class ManageImagesAdapter extends RecyclerView.Adapter<ManageImagesAdapter.ImageViewHolder> {

    private final Context context;
    private final List<ImageItem> items = new ArrayList<>();
    private final OnDeleteListener deleteListener;

    /**
     * Callback interface for handling image deletion events.
     */
    public interface OnDeleteListener {
        /**
         * Called when the user requests to delete an image.
         * 
         * @param eventId   The ID of the event whose image should be deleted
         * @param eventName The name of the event (for confirmation dialogs)
         */
        void onDelete(String eventId, String eventName);
    }

    /**
     * Data class representing a single image item to display.
     */
    public static class ImageItem {
        String eventId;
        String eventName;
        String organizerName;
        String posterUri;

        /**
         * Constructs a new ImageItem.
         * 
         * @param eventId       The Firestore document ID of the event
         * @param eventName     The name/title of the event
         * @param organizerName The name of the user who created the event
         * @param posterUri     The download URL or Base64 string of the poster image
         */
        public ImageItem(String eventId, String eventName, String organizerName, String posterUri) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.organizerName = organizerName;
            this.posterUri = posterUri;
        }
    }

    /**
     * Constructs a new ManageImagesAdapter.
     * 
     * @param context        Application context for inflating layouts and loading
     *                       images
     * @param deleteListener Callback to handle delete button clicks
     */
    public ManageImagesAdapter(Context context, OnDeleteListener deleteListener) {
        this.context = context;
        this.deleteListener = deleteListener;
    }

    /**
     * Replaces the current list of images with a new list.
     * 
     * @param newItems The new list of ImageItem objects to display
     */
    public void setItems(List<ImageItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /**
     * Inflates the layout for a new image item view.
     * 
     * @param parent   The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return A new ImageViewHolder that holds the View for each image item
     */
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.manage_images_card_admin, parent, false);
        return new ImageViewHolder(view);
    }

    /**
     * Binds data to the views in the ViewHolder.
     * 
     * @param holder   The ViewHolder which should be updated
     * @param position The position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageItem item = items.get(position);

        holder.tvTitle.setText("Event : " + (!item.eventName.isEmpty() ? item.eventName : "(untitled)"));
        holder.tvUploaderName.setText("by " + (!item.organizerName.isEmpty() ? item.organizerName : "Unknown"));

        try {
            Glide.with(context)
                    .load(item.posterUri)
                    .placeholder(R.drawable.ic_image_icon)
                    .error(R.drawable.ic_image_icon)
                    .centerCrop()
                    .into(holder.ivThumb);
        } catch (Throwable t) {
            holder.ivThumb.setImageResource(R.drawable.ic_image_icon);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(item.eventId, item.eventName);
            }
        });
    }

    /**
     * Returns the total number of image items in the adapter.
     * 
     * @return The size of the items list
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder class for image item views.
     * Holds references to the views within each grid item.
     */
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvTitle;
        TextView tvUploaderName;
        View btnDelete; // MaterialButton or View

        /**
         * Constructs a new ImageViewHolder and finds view references.
         * 
         * @param itemView The root view of the image item layout
         */
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.posterThumb);
            tvTitle = itemView.findViewById(R.id.title);
            tvUploaderName = itemView.findViewById(R.id.tvUploaderName);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}
