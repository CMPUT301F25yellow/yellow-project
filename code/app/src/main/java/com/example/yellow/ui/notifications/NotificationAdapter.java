package com.example.yellow.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yellow.R;
import com.example.yellow.models.NotificationItem;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifVH> {

    private List<NotificationItem> list = new ArrayList<>();

    public interface ActionListener {
        void onAccept(String eventId, String notificationId);
        void onDecline(String eventId, String notificationId);
    }

    private ActionListener listener;

    public void setActionListener(ActionListener listener) {
        this.listener = listener;
    }

    public void setList(List<NotificationItem> newList) {
        list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotifVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotifVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifVH holder, int position) {
        NotificationItem item = list.get(position);

        holder.tvTitle.setText("New Notification");
        holder.tvMessage.setText(item.getMessage());
        holder.tvTime.setText(formatTime(item.getTimestamp()));

        if (item.getEventId() != null && !item.getEventId().isEmpty()) {
            holder.actionButtons.setVisibility(View.VISIBLE);

            holder.btnAccept.setOnClickListener(v ->
                    listener.onAccept(item.getEventId(), item.getNotificationId())
            );


            holder.btnDecline.setOnClickListener(v ->
                    listener.onDecline(item.getEventId(), item.getNotificationId())
            );
        } else {
            holder.actionButtons.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class NotifVH extends RecyclerView.ViewHolder {

        ImageView imgType;
        TextView tvTitle, tvTime, tvMessage, tvAction;
        LinearLayout actionButtons;
        Button btnAccept, btnDecline;

        public NotifVH(@NonNull View itemView) {
            super(itemView);
            imgType = itemView.findViewById(R.id.imgType);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvAction = itemView.findViewById(R.id.tvAction);

            actionButtons = itemView.findViewById(R.id.actionButtons);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }

    private String formatTime(Timestamp ts) {
        if (ts == null) return "";

        long diff = System.currentTimeMillis() - ts.toDate().getTime();
        long min = diff / 60000;
        long hr = diff / (60000 * 60);
        long day = diff / (60000 * 60 * 24);

        if (min < 1) return "just now";
        if (min < 60) return min + "m ago";
        if (hr < 24) return hr + "h ago";
        if (day < 7) return day + "d ago";

        return new SimpleDateFormat("MMM d", Locale.getDefault())
                .format(ts.toDate());
    }
}


