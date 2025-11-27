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

public class ManageImagesAdapter extends RecyclerView.Adapter<ManageImagesAdapter.ImageViewHolder> {

    private final Context context;
    private final List<ImageItem> items = new ArrayList<>();
    private final OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(String eventId, String eventName);
    }

    public static class ImageItem {
        String eventId;
        String eventName;
        String organizerName;
        String posterUri;

        public ImageItem(String eventId, String eventName, String organizerName, String posterUri) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.organizerName = organizerName;
            this.posterUri = posterUri;
        }
    }

    public ManageImagesAdapter(Context context, OnDeleteListener deleteListener) {
        this.context = context;
        this.deleteListener = deleteListener;
    }

    public void setItems(List<ImageItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.manage_images_card_admin, parent, false);
        return new ImageViewHolder(view);
    }

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

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvTitle;
        TextView tvUploaderName;
        View btnDelete; // MaterialButton or View

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.posterThumb);
            tvTitle = itemView.findViewById(R.id.title);
            tvUploaderName = itemView.findViewById(R.id.tvUploaderName);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}
