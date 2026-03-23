package com.simats.cdss.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.simats.cdss.PatientDetailsActivity;
import com.simats.cdss.R;
import com.simats.cdss.models.DoctorDashboardResponse;

import java.util.List;

public class NeedsAttentionAdapter extends RecyclerView.Adapter<NeedsAttentionAdapter.ViewHolder> {

    private Context context;
    private List<DoctorDashboardResponse.PatientItem> patients;

    public NeedsAttentionAdapter(Context context, List<DoctorDashboardResponse.PatientItem> patients) {
        this.context = context;
        this.patients = patients;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_needs_attention, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DoctorDashboardResponse.PatientItem patient = patients.get(position);

        holder.tvName.setText(patient.getName());
        holder.tvInfo.setText("ID: #" + patient.getId() + " • Room " + patient.getRoom());

        int spo2 = patient.getSpo2();
        holder.tvSpo2Chip.setText("SpO2 " + spo2 + "%");

        // Color logic based on SpO2 value
        // < 85 = Critical (red), 85-87 = Warning (orange), >= 88 = Stable (green)
        if (spo2 < 85) {
            // CRITICAL → RED
            holder.ivStatusIcon.setImageResource(R.drawable.ic_notifications);
            holder.ivStatusIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
            holder.iconBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
            holder.tvSpo2Chip.setBackgroundResource(R.drawable.chip_red_rounded);
            holder.tvSpo2Chip.setTextColor(Color.parseColor("#EF4444"));
        } else if (spo2 < 88) {
            // WARNING → ORANGE
            holder.ivStatusIcon.setImageResource(R.drawable.ic_warning);
            holder.ivStatusIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
            holder.iconBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
            holder.tvSpo2Chip.setBackgroundResource(R.drawable.chip_orange_rounded);
            holder.tvSpo2Chip.setTextColor(Color.parseColor("#854D0E"));
        } else {
            // STABLE → GREEN
            holder.ivStatusIcon.setImageResource(R.drawable.ic_notifications);
            holder.ivStatusIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
            holder.iconBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
            holder.tvSpo2Chip.setBackgroundResource(R.drawable.chip_green_rounded);
            holder.tvSpo2Chip.setTextColor(Color.parseColor("#166534"));
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PatientDetailsActivity.class);
            intent.putExtra("patient_id", patient.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    public void updateList(List<DoctorDashboardResponse.PatientItem> newList) {
        this.patients = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvInfo, tvSpo2Chip;
        ImageView ivStatusIcon;
        View iconBg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvInfo = itemView.findViewById(R.id.tv_info);
            tvSpo2Chip = itemView.findViewById(R.id.tv_spo2_chip);
            ivStatusIcon = itemView.findViewById(R.id.iv_status_icon);
            iconBg = itemView.findViewById(R.id.icon_bg);
        }
    }
}
