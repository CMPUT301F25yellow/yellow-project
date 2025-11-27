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

    /**
     * Sets the list of notifications.
     * @param newList
     */
    public void setList(List<NotificationItem> newList) {
        list = newList;
        notifyDataSetChanged();
    }

    /**
     * Returns the view type of the item at position for the purposes of view recycling.
     * @param position position to query
     * @return
     */
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

    /**
     * Called when RecyclerView needs a new {@link NotifVH} of the given type to represent
     * an item.
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return
     */
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

    /**
     * Called by RecyclerView to display the data at the specified position.
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull NotifVH holder, int position) {
        NotificationItem item = list.get(position);
        int viewType = getItemViewType(position);

        // Normalize type string
        String type = item.getType() != null ? item.getType() : "";

        boolean isLotteryNotSelected = type.equalsIgnoreCase("lottery_not_selected");
        boolean isCancelledEntrant  = type.equalsIgnoreCase("entrant_cancelled");

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
        // For TYPE_WAITING_LIST, title is set in XML ("Waiting List")

        // ----- Message & time -----
        holder.tvMessage.setText(item.getMessage());
        holder.tvTime.setText(formatTime(item.getTimestamp()));

        // ----- Action buttons (Accept / Decline) -----
        if (holder.actionButtons != null) {
            boolean hasEventId = item.getEventId() != null && !item.getEventId().isEmpty();

            // Only appears if:
            //  - eventId is there
            //  - is NOT a lottery_not_selected
            //  - is NOT an entrant_cancelled
            boolean isActionable = hasEventId
                    && !isLotteryNotSelected
                    && !isCancelledEntrant;

            if (isActionable) {
                holder.actionButtons.setVisibility(View.VISIBLE);

                holder.btnAccept.setOnClickListener(
                        v -> listener.onAccept(item.getEventId(), item.getNotificationId())
                );
                holder.btnDecline.setOnClickListener(
                        v -> listener.onDecline(item.getEventId(), item.getNotificationId())
                );
            } else {
                holder.actionButtons.setVisibility(View.GONE);
            }
        }
    }


    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * Clears the list.
     */
    public void clear() {
        list.clear();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for a notification item.
     */
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

            // These will be null for item_notification_waiting_list
            actionButtons = itemView.findViewById(R.id.actionButtons);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }

    /**
     * Formats a timestamp into a human-readable string.
     * @return formatted string
     */
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
