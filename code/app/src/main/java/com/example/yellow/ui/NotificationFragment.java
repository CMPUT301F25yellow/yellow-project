package com.example.yellow.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.yellow.R;

public class NotificationFragment extends Fragment {

    enum NotifType { INFO, EVENT, REMINDER, ALERT, ACTIONABLE }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Make status bar the same color as header
        requireActivity().getWindow().setStatusBarColor(
                ContextCompat.getColor(requireContext(), R.color.surface_dark)
        );

        // Size the spacer to the real status bar height
        View spacer = v.findViewById(R.id.statusBarSpacer);
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.LayoutParams lp = spacer.getLayoutParams();
            if (lp.height != bars.top) {
                lp.height = bars.top;
                spacer.setLayoutParams(lp);
            }
            return insets;
        });

        // Back -> pop to Home (MainActivity shows Home when stack empties)
        View back = v.findViewById(R.id.btnBack);
        if (back != null) {
            back.setOnClickListener(x ->
                    requireActivity().getSupportFragmentManager().popBackStack()
            );
        }

        // (Optional) set adapter later
        RecyclerView rv = v.findViewById(R.id.rvNotifications);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        // 1) Model for this step
        // Types your UI will support for now


        class Notif {
            final String title, message, time, action;
            final NotifType type;
            final boolean unread;

            Notif(String title, String message, String time, String action,
                  NotifType type, boolean unread) {
                this.title = title;
                this.message = message;
                this.time = time;
                this.action = action;
                this.type = type;
                this.unread = unread;
            }
        }


        // 2) Adapter that inflates your card layout
        class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.VH> {
            private final java.util.List<Notif> items;

            NotificationsAdapter(java.util.List<Notif> items) { this.items = items; }

            private static final int VT_NORMAL = 0;
            private static final int VT_ACTIONABLE = 1;

            @Override
            public int getItemViewType(int position) {
                return items.get(position).type == NotifType.ACTIONABLE ? VT_ACTIONABLE : VT_NORMAL;
            }


            class VH extends RecyclerView.ViewHolder {
                com.google.android.material.card.MaterialCardView card;
                android.widget.ImageView imgType;
                android.widget.TextView tvTitle, tvMessage, tvTime, tvAction;
                android.view.View dot;

                VH(@NonNull View itemView) {
                    super(itemView);
                    card     = itemView.findViewById(R.id.card);
                    imgType  = itemView.findViewById(R.id.imgType);
                    tvTitle  = itemView.findViewById(R.id.tvTitle);
                    tvMessage= itemView.findViewById(R.id.tvMessage);
                    tvTime   = itemView.findViewById(R.id.tvTime);
                    tvAction = itemView.findViewById(R.id.tvAction);
                    dot      = itemView.findViewById(R.id.dot); // if not in XML, this can be null
                }
            }

            @NonNull @Override
            public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                int layout = (viewType == VT_ACTIONABLE)
                        ? R.layout.item_notification_actionable
                        : R.layout.item_notification;

                View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
                return new VH(v);
            }


            @Override
            public void onBindViewHolder(@NonNull VH h, int pos) {
                Notif n = items.get(pos);

                // common fields
                h.tvTitle.setText(n.title);
                h.tvMessage.setText(n.message);
                h.tvTime.setText(n.time);
                if (h.tvAction != null) { // actionable row doesn't have tvAction
                    h.tvAction.setText(n.action);
                    h.tvAction.setOnClickListener(v ->
                            android.widget.Toast.makeText(v.getContext(),
                                    "Action: " + n.action, android.widget.Toast.LENGTH_SHORT).show());
                }

                // icon + tint only if the layout has an ImageView
                if (h.imgType != null) {
                    int iconRes;
                    int tintColor;
                    switch (n.type) {
                        case EVENT:
                            iconRes = R.drawable.ic_eventscal;
                            tintColor = androidx.core.content.ContextCompat.getColor(h.itemView.getContext(), R.color.brand_primary);
                            break;
                        case REMINDER:
                            iconRes = R.drawable.ic_clock;
                            tintColor = androidx.core.content.ContextCompat.getColor(h.itemView.getContext(), R.color.yellow_400);
                            break;
                        case ALERT:
                            iconRes = R.drawable.ic_notif;
                            tintColor = androidx.core.content.ContextCompat.getColor(h.itemView.getContext(), R.color.red_400);
                            break;
                        case INFO:
                        case ACTIONABLE: // fallback icon/tint for actionable
                        default:
                            iconRes = R.drawable.ic_notif;
                            tintColor = androidx.core.content.ContextCompat.getColor(h.itemView.getContext(), R.color.green_400);
                            break;
                    }
                    h.imgType.setImageResource(iconRes);
                    androidx.core.widget.ImageViewCompat.setImageTintList(
                            h.imgType,
                            android.content.res.ColorStateList.valueOf(tintColor)
                    );
                }

                // unread dot (if present in this layout)
                if (h.dot != null) h.dot.setVisibility(n.unread ? View.VISIBLE : View.GONE);

                // actionable buttons exist only in item_notification_actionable
                View btnAccept = h.itemView.findViewById(R.id.btnAccept);
                View btnReject = h.itemView.findViewById(R.id.btnReject);
                View btnView   = h.itemView.findViewById(R.id.btnViewDetails);

                if (btnAccept != null) {
                    btnAccept.setOnClickListener(v ->
                            android.widget.Toast.makeText(v.getContext(), "Accepted ✅", android.widget.Toast.LENGTH_SHORT).show());
                }
                if (btnReject != null) {
                    btnReject.setOnClickListener(v ->
                            android.widget.Toast.makeText(v.getContext(), "Rejected ❌", android.widget.Toast.LENGTH_SHORT).show());
                }
                if (btnView != null) {
                    btnView.setOnClickListener(v ->
                            android.widget.Toast.makeText(v.getContext(), "View Details →", android.widget.Toast.LENGTH_SHORT).show());
                }
            }
            @Override public int getItemCount() { return items.size(); }
        }

        // 3) Attach adapter immediately
        java.util.List<Notif> demo = new java.util.ArrayList<>();
        NotificationsAdapter adapter = new NotificationsAdapter(demo);
        rv.setAdapter(adapter);

        // 4) Add a few demo cards to SEE it styled
        demo.add(new Notif(
                "Selected for Summer Music Festival",
                "Congratulations! You’ve been selected to attend on Oct 20, 2025.",
                "1h ago",
                "View Details",
                NotifType.ACTIONABLE,
                true   // unread
        ));

        demo.add(new Notif(
                "Selected for Summer Music Festival",
                "Congratulations! You’ve been selected for the festival on Oct 20, 2025.",
                "1h ago",
                "View Details",
                NotifType.EVENT,
                true   // unread
        ));
        demo.add(new Notif(
                "New Event Available",
                "“Jazz Under the Stars” is open for lottery registration. Join now!",
                "3h ago",
                "View Details",
                NotifType.INFO,
                false
        ));
        demo.add(new Notif(
                "Not Selected for Championship Finals",
                "Unfortunately, you were not selected. Better luck next time!",
                "5h ago",
                "Learn More",
                NotifType.ALERT,
                false
        ));
        demo.add(new Notif(
                "System Maintenance Notice",
                "The app will undergo maintenance on Oct 25 from 2:00–4:00 AM.",
                "1d ago",
                "OK",
                NotifType.REMINDER,
                false
        ));
        adapter.notifyItemRangeInserted(0, demo.size());

        // rv.setAdapter(...);
    }
}