package com.simats.cdss.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.simats.cdss.R;
import com.simats.cdss.VitalsActivity;
import com.simats.cdss.CurrentSymptomsActivity;
import com.simats.cdss.ABGEntryActivity;
import com.simats.cdss.models.PatientResponse;

import java.util.ArrayList;
import java.util.List;

public class StaffPatientAdapter extends RecyclerView.Adapter<StaffPatientAdapter.PatientViewHolder> {

    private final Context context;
    private List<PatientResponse> patientList;
    private String fromAction;

    public StaffPatientAdapter(Context context, List<PatientResponse> patientList) {
        this(context, patientList, "vitals");
    }

    public StaffPatientAdapter(Context context, List<PatientResponse> patientList, String fromAction) {
        this.context = context;
        this.patientList = patientList;
        this.fromAction = fromAction;
    }

    public void updateList(List<PatientResponse> newList) {
        patientList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_staff_patient, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        PatientResponse patient = patientList.get(position);

        holder.tvName.setText(patient.getName() != null ? patient.getName() : "Unknown");

        // Fixed: Synced method names with PatientResponse model
        String ward = patient.getWardNo() != null ? patient.getWardNo() : "--";
        String room = patient.getRoomNo() != null ? patient.getRoomNo() : "--";
        holder.tvDetails.setText("Ward " + ward + " • Room " + room);
        
        // Always show the vitals section even if NULL, using "--" as placeholder
        holder.layoutSpo2.setVisibility(View.VISIBLE);
        String spo2Str = patient.getSpo2() != null ? patient.getSpo2() : "--";
        if (!spo2Str.equals("--")) spo2Str += "%";
        holder.tvSpo2.setText(spo2Str);

        holder.layoutResp.setVisibility(View.VISIBLE);
        String respVal = patient.getRespiratoryRate() != null ? patient.getRespiratoryRate() : "--";
        holder.tvResp.setText(respVal);

        String status = patient.getStatus();
        if (status != null) {
            holder.tvStatus.setText(status.toUpperCase());
            if (status.equalsIgnoreCase("critical")) {
                holder.tvStatus.setTextColor(Color.parseColor("#EF4444"));
                holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
                holder.tvStatus.setBackgroundResource(R.drawable.chip_red_rounded);
                holder.tvStatus.setAlpha(1.0f);
                holder.tvSpo2.setTextColor(Color.parseColor("#EF4444"));
            } else if (status.equalsIgnoreCase("warning")) {
                holder.tvStatus.setTextColor(Color.parseColor("#854D0E"));
                holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
                holder.tvStatus.setBackgroundResource(R.drawable.chip_orange_rounded);
                holder.tvStatus.setAlpha(1.0f);
                holder.tvSpo2.setTextColor(Color.parseColor("#F59E0B"));
            } else {
                holder.tvStatus.setTextColor(Color.parseColor("#475569"));
                holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
                holder.tvStatus.setBackgroundResource(R.drawable.chip_green_rounded);
                holder.tvStatus.setAlpha(1.0f);
                holder.tvSpo2.setTextColor(Color.parseColor("#139487"));
            }
        } else {
            holder.tvStatus.setText("UNKNOWN");
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent;
            if ("symptoms".equals(fromAction)) {
                intent = new Intent(context, CurrentSymptomsActivity.class);
            } else if ("abg_entry".equals(fromAction)) {
                intent = new Intent(context, ABGEntryActivity.class);
            } else {
                intent = new Intent(context, VitalsActivity.class);
            }
            intent.putExtra("patient_id", patient.getPatientId());
            intent.putExtra("patient_name", patient.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return patientList != null ? patientList.size() : 0;
    }

    public static class PatientViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails, tvStatus, tvSpo2, tvResp;
        View layoutSpo2, layoutResp;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_patient_name);
            tvDetails = itemView.findViewById(R.id.tv_patient_details);
            tvStatus = itemView.findViewById(R.id.tv_patient_status);
            tvSpo2 = itemView.findViewById(R.id.tv_spo2);
            tvResp = itemView.findViewById(R.id.tv_resp);
            layoutSpo2 = itemView.findViewById(R.id.layout_spo2);
            layoutResp = itemView.findViewById(R.id.layout_resp);
        }
    }
}