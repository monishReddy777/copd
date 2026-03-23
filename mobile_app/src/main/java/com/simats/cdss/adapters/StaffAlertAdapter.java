package com.simats.cdss.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.simats.cdss.R;
import com.simats.cdss.models.StaffAlertsResponse;

import java.util.List;

public class StaffAlertAdapter extends RecyclerView.Adapter<StaffAlertAdapter.ViewHolder> {

    private final List<StaffAlertsResponse.ReassessmentAlert> items;
    private final Context context;
    private OnAlertActionListener listener;

    public interface OnAlertActionListener {
        void onPerformReassessment(StaffAlertsResponse.ReassessmentAlert alert);
        void onViewPatient(StaffAlertsResponse.ReassessmentAlert alert);
    }

    public StaffAlertAdapter(Context context, List<StaffAlertsResponse.ReassessmentAlert> items) {
        this.context = context;
        this.items = items;
    }

    public void setOnAlertActionListener(OnAlertActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_staff_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StaffAlertsResponse.ReassessmentAlert alert = items.get(position);

        // Set title — generic "Check Due" for all reassessment types
        holder.tvAlertTitle.setText("Check Due");
        holder.ivAlertIcon.setImageResource(R.drawable.ic_clock);

        // Set severity colors
        String severity = alert.getSeverity();
        if ("critical".equalsIgnoreCase(severity)) {
            holder.tvAlertTitle.setTextColor(context.getColor(R.color.red_500));
            holder.ivAlertIcon.setColorFilter(context.getColor(R.color.red_500));
            holder.tvSeverityBadge.setText("Overdue");
            holder.tvSeverityBadge.setTextColor(context.getColor(R.color.red_500));
            holder.tvSeverityBadge.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFEF2F2));
        } else {
            holder.tvAlertTitle.setTextColor(0xFFF59E0B);
            holder.ivAlertIcon.setColorFilter(0xFFF59E0B);
            holder.tvSeverityBadge.setText("Due Soon");
            holder.tvSeverityBadge.setTextColor(0xFFB45309);
            holder.tvSeverityBadge.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFFFBEB));
        }

        // Set patient info with bed and ward
        String bedNo = alert.getBedNo();
        String wardNo = alert.getWardNo();
        StringBuilder patientInfo = new StringBuilder(alert.getPatientName());
        if (bedNo != null && !bedNo.isEmpty()) {
            patientInfo.append(" (Bed ").append(bedNo);
            if (wardNo != null && !wardNo.isEmpty()) {
                patientInfo.append(" • Ward ").append(wardNo);
            }
            patientInfo.append(")");
        }
        holder.tvPatientInfo.setText(patientInfo.toString());

        // Set detail text (type + due info)
        int dueIn = alert.getDueIn();
        String timeText;
        if (dueIn <= 0) {
            timeText = "Overdue by " + Math.abs(dueIn) + " mins";
        } else {
            timeText = "Due in " + dueIn + " mins";
        }

        holder.tvAlertDetail.setText("Reassessment scheduled • " + timeText);

        // Set overdue text color
        if (dueIn <= 0) {
            holder.tvAlertDetail.setTextColor(context.getColor(R.color.red_500));
        } else {
            holder.tvAlertDetail.setTextColor(0xFF64748B);
        }

        // Scheduled by
        holder.tvScheduledBy.setText("Scheduled by: Doctor");

        // Action buttons
        holder.btnPerformReassessment.setOnClickListener(v -> {
            if (listener != null) listener.onPerformReassessment(alert);
        });

        holder.btnViewPatient.setOnClickListener(v -> {
            if (listener != null) listener.onViewPatient(alert);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlertIcon;
        TextView tvAlertTitle, tvSeverityBadge, tvPatientInfo, tvAlertDetail, tvScheduledBy;
        MaterialButton btnPerformReassessment, btnViewPatient;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlertIcon = itemView.findViewById(R.id.iv_alert_icon);
            tvAlertTitle = itemView.findViewById(R.id.tv_alert_title);
            tvSeverityBadge = itemView.findViewById(R.id.tv_severity_badge);
            tvPatientInfo = itemView.findViewById(R.id.tv_patient_info);
            tvAlertDetail = itemView.findViewById(R.id.tv_alert_detail);
            tvScheduledBy = itemView.findViewById(R.id.tv_scheduled_by);
            btnPerformReassessment = itemView.findViewById(R.id.btn_perform_reassessment);
            btnViewPatient = itemView.findViewById(R.id.btn_view_patient);
        }
    }
}
