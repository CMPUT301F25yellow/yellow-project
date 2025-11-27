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

    private static final int TYPE_DEFAULT = 0;
    private static final int TYPE_WAITING_LIST = 1;

    public void setList(List<NotificationItem> newList) {
        list = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        NotificationItem item = list.get(position);
        boolean isWaitingList = false;

        if (item.getType() != null && item.getType().equalsIgnoreCase("waiting_list")) {
            isWaitingList = true;
        } else if (item.getMessage() != null && item.getMessage().toLowerCase().contains("waiting list")) {
            isWaitingList = true;
        }

        return isWaitingList ? TYPE_WAITING_LIST : TYPE_DEFAULT;
    }

    @NonNull
    @Override
    public NotifVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == TYPE_WAITING_LIST)
                ? R.layout.item_notification_waiting_list
                : R.layout.item_notification;

        View v = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new NotifVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifVH holder, int position) {
        NotificationItem item = list.get(position);
        int viewType = getItemViewType(position);

        // --- Safely normalize fields ---
        String rawType = item.getType();
        String type = rawType != null ? rawType : "";
        String msg = item.getMessage() != null ? item.getMessage().toLowerCase() : "";

        // --- Detect "non-selected" (lost lottery) notifications ---
        boolean isLotteryNotSelected =
                type.equalsIgnoreCase("lottery_not_selected")       // ideal
                        || type.equalsIgnoreCase("non_selected")    // legacy / mismatch
                        || msg.contains("you were not selected")    // fallback by message
                        || msg.contains("not selected in the lottery");

        // --- Detect "cancelled entrant" notifications ---
        boolean isCancelledEntrant =
                type.equalsIgnoreCase("entrant_cancelled")          // ideal
                        || msg.contains("your selection for") && msg.contains("was cancelled")
                        || msg.contains("selection was cancelled");

        // ----- Title -----
        if (viewType == TYPE_DEFAULT) {
            if (isLotteryNotSelected) {
                holder.tvTitle.setText("Lottery Result");
            } else if (isCancelledEntrant) {
                holder.tvTitle.setText("Selection Cancelled");
            } else {
                holder.tvTitle.setText("New Notification");
            }
        }
        // TYPE_WAITING_LIST uses its own XML title

        // ----- Message & time -----
        holder.tvMessage.setText(item.getMessage());
        holder.tvTime.setText(formatTime(item.getTimestamp()));

        // ----- Action buttons (Accept / Decline) -----
        if (holder.actionButtons != null) {

            boolean hasEventId = item.getEventId() != null && !item.getEventId().isEmpty();

            // Only actionable if:
            //  - it has an eventId
            //  - and is NOT a lottery_not_selected
            //  - and is NOT an entrant_cancelled
            boolean isActionable = hasEventId
                    && !isLotteryNotSelected
                    && !isCancelledEntrant;

            if (isActionable && listener != null) {
                holder.actionButtons.setVisibility(View.VISIBLE);

                holder.btnAccept.setOnClickListener(
                        v -> listener.onAccept(item.getEventId(), item.getNotificationId())
                );
                holder.btnDecline.setOnClickListener(
                        v -> listener.onDecline(item.getEventId(), item.getNotificationId())
                );
            } else {
                holder.actionButtons.setVisibility(View.GONE);
                // Optional: remove old listeners for safety
                if (holder.btnAccept != null) holder.btnAccept.setOnClickListener(null);
                if (holder.btnDecline != null) holder.btnDecline.setOnClickListener(null);
            }
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

            // These will be null for item_notification_waiting_list
            actionButtons = itemView.findViewById(R.id.actionButtons);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }

    private String formatTime(Timestamp ts) {
        if (ts == null)
            return "";

        long diff = System.currentTimeMillis() - ts.toDate().getTime();
        long min = diff / 60000;
        long hr = diff / (60000 * 60);
        long day = diff / (60000 * 60 * 24);

        if (min < 1)
            return "just now";
        if (min < 60)
            return min + "m ago";
        if (hr < 24)
            return hr + "h ago";
        if (day < 7)
            return day + "d ago";

        return new SimpleDateFormat("MMM d", Locale.getDefault())
                .format(ts.toDate());
    }
}
