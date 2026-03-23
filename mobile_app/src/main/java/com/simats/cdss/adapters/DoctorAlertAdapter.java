package com.simats.cdss.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.simats.cdss.R;
import com.simats.cdss.models.DoctorAlertsResponse;

import java.util.List;

public class DoctorAlertAdapter extends RecyclerView.Adapter<DoctorAlertAdapter.ViewHolder> {

    public interface OnAlertActionListener {
        void onViewPatient(DoctorAlertsResponse.AlertItem item);
        void onAcknowledge(DoctorAlertsResponse.AlertItem item);
        void onMarkRead(DoctorAlertsResponse.AlertItem item);
    }

    private final Context context;
    private final List<DoctorAlertsResponse.AlertItem> items;
    private final String sectionType; // "critical", "warning", "info"
    private OnAlertActionListener listener;

    public DoctorAlertAdapter(Context context, List<DoctorAlertsResponse.AlertItem> items, String sectionType) {
        this.context = context;
        this.items = items;
        this.sectionType = sectionType;
    }

    public void setOnAlertActionListener(OnAlertActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_doctor_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DoctorAlertsResponse.AlertItem item = items.get(position);

        // Set alert type title
        holder.tvAlertType.setText(item.getAlertType());

        // Set time ago
        String timeAgo = item.getTimeAgo();
        if (timeAgo != null && !timeAgo.isEmpty()) {
            holder.tvTimeAgo.setText(timeAgo);
            holder.tvTimeAgo.setVisibility(View.VISIBLE);
        } else {
            holder.tvTimeAgo.setVisibility(View.GONE);
        }

        // Set message
        holder.tvMessage.setText(item.getMessage());

        // Style based on severity
        String severity = item.getSeverity();
        if ("critical".equalsIgnoreCase(severity)) {
            holder.ivIcon.setImageResource(R.drawable.ic_warning);
            holder.ivIcon.setColorFilter(context.getColor(R.color.red_500));
            holder.tvAlertType.setTextColor(context.getColor(R.color.red_500));

            // Show action buttons for critical
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnViewPatient.setVisibility(View.VISIBLE);
            holder.btnAcknowledge.setVisibility(View.VISIBLE);
            holder.btnMarkRead.setVisibility(View.GONE);

        } else if ("warning".equalsIgnoreCase(severity)) {
            holder.ivIcon.setImageResource(R.drawable.ic_description);
            holder.ivIcon.setColorFilter(context.getColor(R.color.amber_700));
            holder.tvAlertType.setTextColor(context.getColor(R.color.amber_700));

            // Show view patient for warning
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnViewPatient.setVisibility(View.VISIBLE);
            holder.btnAcknowledge.setVisibility(View.VISIBLE);
            holder.btnMarkRead.setVisibility(View.GONE);

        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_check_circle);
            holder.ivIcon.setColorFilter(context.getColor(R.color.green_500));
            holder.tvAlertType.setTextColor(context.getColor(R.color.text_secondary));

            // Show mark read for info
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnViewPatient.setVisibility(View.GONE);
            holder.btnAcknowledge.setVisibility(View.GONE);
            holder.btnMarkRead.setVisibility(View.VISIBLE);
        }

        // Grey out acknowledged alerts
        if ("acknowledged".equalsIgnoreCase(item.getStatus()) || "read".equalsIgnoreCase(item.getStatus())) {
            holder.itemView.setAlpha(0.5f);
            holder.layoutActions.setVisibility(View.GONE);
        } else {
            holder.itemView.setAlpha(1.0f);
        }

        // Click listeners
        holder.btnViewPatient.setOnClickListener(v -> {
            if (listener != null) listener.onViewPatient(item);
        });
        holder.btnAcknowledge.setOnClickListener(v -> {
            if (listener != null) listener.onAcknowledge(item);
        });
        holder.btnMarkRead.setOnClickListener(v -> {
            if (listener != null) listener.onMarkRead(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvAlertType, tvTimeAgo, tvMessage;
        LinearLayout layoutActions;
        MaterialButton btnViewPatient, btnAcknowledge, btnMarkRead;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_alert_icon);
            tvAlertType = itemView.findViewById(R.id.tv_alert_type);
            tvTimeAgo = itemView.findViewById(R.id.tv_time_ago);
            tvMessage = itemView.findViewById(R.id.tv_alert_message);
            layoutActions = itemView.findViewById(R.id.layout_actions);
            btnViewPatient = itemView.findViewById(R.id.btn_view_patient);
            btnAcknowledge = itemView.findViewById(R.id.btn_acknowledge);
            btnMarkRead = itemView.findViewById(R.id.btn_mark_read);
        }
    }
}
