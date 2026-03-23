package com.simats.cdss.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.simats.cdss.R;
import com.simats.cdss.models.StaffReassessmentValuesResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StaffReassessmentAdapter extends RecyclerView.Adapter<StaffReassessmentAdapter.ViewHolder> {

    private final Context context;
    private final List<StaffReassessmentValuesResponse.StaffEntry> items;

    public StaffReassessmentAdapter(Context context, List<StaffReassessmentValuesResponse.StaffEntry> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_staff_reassessment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StaffReassessmentValuesResponse.StaffEntry entry = items.get(position);

        // Reassessment type
        String type = entry.getReassessmentType();
        holder.tvReassessmentType.setText(type != null ? type + " Reassessment" : "Reassessment");

        // Timestamp
        String ts = entry.getCreatedAt();
        if (ts != null && !ts.isEmpty()) {
            try {
                SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outFormat = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
                Date date = inFormat.parse(ts);
                holder.tvTimestamp.setText(outFormat.format(date));
            } catch (ParseException e) {
                holder.tvTimestamp.setText(ts);
            }
        } else {
            holder.tvTimestamp.setText("");
        }

        // SpO2
        holder.tvSpo2.setText(String.format(Locale.getDefault(), "%.0f%%", entry.getSpo2()));

        // Respiratory Rate
        holder.tvRespiratoryRate.setText(String.format(Locale.getDefault(), "%.0f /min", entry.getRespiratoryRate()));

        // Heart Rate
        Double hr = entry.getHeartRate();
        if (hr != null && hr > 0) {
            holder.tvHeartRate.setText(String.format(Locale.getDefault(), "%.0f bpm", hr));
        } else {
            holder.tvHeartRate.setText("--");
        }

        // ABG Values
        String abg = entry.getAbgValues();
        if (abg != null && !abg.trim().isEmpty()) {
            holder.tvAbgValues.setText("ABG: " + abg);
            holder.tvAbgValues.setVisibility(View.VISIBLE);
        } else {
            holder.tvAbgValues.setVisibility(View.GONE);
        }

        // Remarks
        String remarks = entry.getRemarks();
        if (remarks != null && !remarks.trim().isEmpty()) {
            holder.tvRemarks.setText("Remarks: " + remarks);
            holder.tvRemarks.setVisibility(View.VISIBLE);
        } else {
            holder.tvRemarks.setVisibility(View.GONE);
        }

        // Entered by
        String enteredBy = entry.getEnteredBy();
        holder.tvEnteredBy.setText("Entered by: " + (enteredBy != null ? enteredBy : "Staff"));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReassessmentType, tvTimestamp;
        TextView tvSpo2, tvRespiratoryRate, tvHeartRate;
        TextView tvAbgValues, tvRemarks, tvEnteredBy;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReassessmentType = itemView.findViewById(R.id.tv_reassessment_type);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvSpo2 = itemView.findViewById(R.id.tv_spo2);
            tvRespiratoryRate = itemView.findViewById(R.id.tv_respiratory_rate);
            tvHeartRate = itemView.findViewById(R.id.tv_heart_rate);
            tvAbgValues = itemView.findViewById(R.id.tv_abg_values);
            tvRemarks = itemView.findViewById(R.id.tv_remarks);
            tvEnteredBy = itemView.findViewById(R.id.tv_entered_by);
        }
    }
}
