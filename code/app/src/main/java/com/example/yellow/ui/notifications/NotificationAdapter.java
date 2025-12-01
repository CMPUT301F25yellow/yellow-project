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

/**
 * Adapter for displaying notifications.
 * @author Waylon Wang - waylon1
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifVH> {

    private List<NotificationItem> list = new ArrayList<>();

    /**
     * Listener for accept/decline buttons and click events.
     */
    public interface ActionListener {
        void onAccept(String eventId, String notificationId);

        void onDecline(String eventId, String notificationId);
    }

    private ActionListener listener;

    /**
     * Sets the listener for accept/decline buttons and click events.
     * @param listener: the listener to set
     */
    public void setActionListener(ActionListener listener) {
        this.listener = listener;
    }

    private static final int TYPE_DEFAULT = 0;
    private static final int TYPE_WAITING_LIST = 1;
    private static final int TYPE_LOTTERY_LOSER = 2;
    private static final int TYPE_CANCELLED = 3;

    /**
     * Sets the list of notifications to display.
     * @param newList: the new list to set
     */
    public void setList(List<NotificationItem> newList) {
        list = newList;
        notifyDataSetChanged();
    }

    /**
     * Gets the view type for the given position.
     * @param position position to query
     * @return view type
     */
    @Override
    public int getItemViewType(int position) {
        NotificationItem item = list.get(position);

        String type = item.getType() != null ? item.getType() : "";
        String msg = item.getMessage() != null ? item.getMessage().toLowerCase() : "";

        // Waiting-list
        if (type.equalsIgnoreCase("waiting_list") ||
                msg.contains("waiting list")) {
            return TYPE_WAITING_LIST;
        }

        // Lottery not selected
        if (type.equalsIgnoreCase("lottery_not_selected") ||
                type.equalsIgnoreCase("non_selected") ||
                msg.contains("you were not selected") ||
                msg.contains("not selected in the lottery")) {
            return TYPE_LOTTERY_LOSER;
        }

        // Selection cancelled
        if (type.equalsIgnoreCase("entrant_cancelled") ||
                (msg.contains("your selection for") && msg.contains("was cancelled")) ||
                msg.contains("selection was cancelled")) {
            return TYPE_CANCELLED;
        }

        return TYPE_DEFAULT;
    }

    /**
     * Called when RecyclerView needs a new {@link NotifVH} of the given type to represent
     * an item.
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public NotifVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        switch (viewType) {
            case TYPE_WAITING_LIST:
                layoutId = R.layout.item_notification_waiting_list;
                break;
            case TYPE_LOTTERY_LOSER:
                layoutId = R.layout.item_notification_lottery_loser;
                break;
            case TYPE_CANCELLED:
                layoutId = R.layout.item_notification_cancelled_entrants;
                break;
            case TYPE_DEFAULT:
            default:
                layoutId = R.layout.item_notification;
                break;
        }

        View v = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new NotifVH(v);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link NotifVH#itemView}
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull NotifVH holder, int position) {
        NotificationItem item = list.get(position);
        int viewType = getItemViewType(position);

        // --- Safely normalize fields ---
        String rawType = item.getType();
        String type = rawType != null ? rawType : "";
        String msg = item.getMessage() != null ? item.getMessage().toLowerCase() : "";

        // --- Detect "non-selected" (lost lottery) notifications ---
        boolean isLotteryNotSelected = type.equalsIgnoreCase("lottery_not_selected") // ideal
                || type.equalsIgnoreCase("non_selected") // legacy / mismatch
                || msg.contains("you were not selected") // fallback by message
                || msg.contains("not selected in the lottery");

        // --- Detect "cancelled entrant" notifications ---
        boolean isCancelledEntrant = type.equalsIgnoreCase("entrant_cancelled") // ideal
                || msg.contains("your selection for") && msg.contains("was cancelled")
                || msg.contains("selection was cancelled");

        // --- Detect "enrolled" notifications ---
        boolean isEnrolled = msg.contains("you are enrolled") || msg.contains("enrolled in");

        // ----- Title -----
        if (viewType == TYPE_DEFAULT) {
            if (isLotteryNotSelected) {
                holder.tvTitle.setText("Lottery Result");
            } else if (isCancelledEntrant) {
                holder.tvTitle.setText("Selection Cancelled");
            } else if (isEnrolled) {
                holder.tvTitle.setText("Enrolled");
                // Set green tick icon for enrolled notifications
                if (holder.imgType != null) {
                    holder.imgType.setImageResource(R.drawable.ic_green_tick);
                    holder.imgType.setVisibility(View.VISIBLE);
                }
            } else {
                holder.tvTitle.setText("New Notification");
            }
        }

        // ----- Message & time -----
        holder.tvMessage.setText(item.getMessage());
        holder.tvTime.setText(formatTime(item.getTimestamp()));

        // ----- Action buttons (Accept / Decline) -----
        if (holder.actionButtons != null) {

            boolean hasEventId = item.getEventId() != null && !item.getEventId().isEmpty();

            // Only actionable if:
            // - it has an eventId
            // - and is NOT a lottery_not_selected
            // - and is NOT an entrant_cancelled
            // - and is NOT an enrolled notification
            boolean isActionable = hasEventId
                    && !isLotteryNotSelected
                    && !isCancelledEntrant
                    && !isEnrolled; // Hide buttons for enrolled notifications

            if (isActionable && listener != null) {
                holder.actionButtons.setVisibility(View.VISIBLE);

                holder.btnAccept.setOnClickListener(
                        v -> listener.onAccept(item.getEventId(), item.getNotificationId()));
                holder.btnDecline.setOnClickListener(
                        v -> listener.onDecline(item.getEventId(), item.getNotificationId()));
            } else {
                holder.actionButtons.setVisibility(View.GONE);
                // Optional: remove old listeners for safety
                if (holder.btnAccept != null)
                    holder.btnAccept.setOnClickListener(null);
                if (holder.btnDecline != null)
                    holder.btnDecline.setOnClickListener(null);
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
     * ViewHolder for notifications.
     * @author Waylon Wang - waylon1
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

            // These will be null for item_notification_waiting_list
            actionButtons = itemView.findViewById(R.id.actionButtons);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }

    /**
     * Formats the time.
     * @param ts: the timestamp to format
     * @return the formatted time
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
