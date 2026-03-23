package com.simats.cdss.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.simats.cdss.R;
import com.simats.cdss.models.Doctor;
import java.util.ArrayList;
import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.ViewHolder> {

    private List<Doctor> doctorList;
    private List<Doctor> filteredList;
    private final OnDoctorActionListener listener;

    public interface OnDoctorActionListener {
        void onToggleStatus(Doctor doctor, int position, boolean isActive);
        void onRemoveDoctor(Doctor doctor, int position);
        void onViewDetails(Doctor doctor);
    }

    public DoctorAdapter(List<Doctor> doctorList, OnDoctorActionListener listener) {
        this.doctorList = doctorList;
        this.filteredList = new ArrayList<>(doctorList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Doctor doctor = filteredList.get(position);
        holder.tvName.setText(doctor.getName());
        holder.tvSpecialization.setText("Doctor"); 
        holder.tvLicense.setText("License: " + (doctor.getLicenseNumber() != null ? doctor.getLicenseNumber() : "null"));

        boolean isActive = "active".equalsIgnoreCase(doctor.getStatus());
        
        // Setup initial UI state without triggering listeners
        holder.switchStatus.setOnCheckedChangeListener(null);
        holder.switchStatus.setChecked(isActive);
        updateUIForStatus(holder, isActive);

        holder.switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                updateUIForStatus(holder, isChecked);
                listener.onToggleStatus(doctor, position, isChecked);
            }
        });

        holder.btnRemove.setOnClickListener(v -> listener.onRemoveDoctor(doctor, position));
        holder.btnDetails.setOnClickListener(v -> listener.onViewDetails(doctor));
    }

    private void updateUIForStatus(ViewHolder holder, boolean isActive) {
        int colorRes = isActive ? R.color.admin_success : R.color.admin_danger;
        int color = ContextCompat.getColor(holder.itemView.getContext(), colorRes);
        
        // Update Status Badge text and color
        holder.tvStatusBadge.setText(isActive ? "Active" : "Disabled");
        holder.tvStatusBadge.setTextColor(color);

        // Update Switch colors (Thumb and Track)
        ColorStateList colorStateList = ColorStateList.valueOf(color);
        holder.switchStatus.setThumbTintList(colorStateList);
        holder.switchStatus.setTrackTintList(colorStateList.withAlpha(64)); // ~25% opacity for track
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void updateList(List<Doctor> newList) {
        doctorList = newList;
        filteredList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(doctorList);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (Doctor doc : doctorList) {
                if (doc.getName().toLowerCase().contains(filterPattern)) {
                    filteredList.add(doc);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < filteredList.size()) {
            Doctor doctor = filteredList.get(position);
            doctorList.remove(doctor);
            filteredList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, filteredList.size());
        }
    }

    public void updateItemStatus(int position, String newStatus) {
        if (position >= 0 && position < filteredList.size()) {
            filteredList.get(position).setStatus(newStatus);
            notifyItemChanged(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialization, tvLicense, tvStatusBadge;
        SwitchMaterial switchStatus;
        View btnDetails, btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_doctor_name);
            tvSpecialization = itemView.findViewById(R.id.tv_specialization);
            tvLicense = itemView.findViewById(R.id.tv_license);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            switchStatus = itemView.findViewById(R.id.switch_doctor_status);
            btnDetails = itemView.findViewById(R.id.btn_view_details);
            btnRemove = itemView.findViewById(R.id.btn_remove_doctor);
        }
    }
}