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
import com.simats.cdss.models.Staff;
import java.util.ArrayList;
import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {

    private List<Staff> staffList;
    private List<Staff> filteredList;
    private final OnStaffActionListener listener;

    public interface OnStaffActionListener {
        void onToggleStatus(Staff staff, int position, boolean isActive);
        void onRemoveStaff(Staff staff, int position);
        void onViewDetails(Staff staff);
    }

    public StaffAdapter(List<Staff> staffList, OnStaffActionListener listener) {
        this.staffList = staffList;
        this.filteredList = new ArrayList<>(staffList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Staff staff = filteredList.get(position);
        holder.tvName.setText(staff.getName());
        holder.tvRole.setText("Clinical Staff"); 
        holder.tvStaffId.setText("Staff ID: " + (staff.getStaffId() != null ? staff.getStaffId() : "null"));

        boolean isActive = "active".equalsIgnoreCase(staff.getStatus());
        
        // Setup initial UI state without triggering listeners
        holder.switchStatus.setOnCheckedChangeListener(null);
        holder.switchStatus.setChecked(isActive);
        updateUIForStatus(holder, isActive);

        holder.switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                updateUIForStatus(holder, isChecked);
                listener.onToggleStatus(staff, position, isChecked);
            }
        });

        holder.btnRemove.setOnClickListener(v -> listener.onRemoveStaff(staff, position));
        holder.btnDetails.setOnClickListener(v -> listener.onViewDetails(staff));
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

    public void updateList(List<Staff> newList) {
        staffList = newList;
        filteredList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(staffList);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (Staff s : staffList) {
                if (s.getName().toLowerCase().contains(filterPattern)) {
                    filteredList.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < filteredList.size()) {
            Staff staff = filteredList.get(position);
            staffList.remove(staff);
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
        TextView tvName, tvRole, tvStaffId, tvStatusBadge;
        SwitchMaterial switchStatus;
        View btnDetails, btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_staff_name);
            tvRole = itemView.findViewById(R.id.tv_staff_role);
            tvStaffId = itemView.findViewById(R.id.tv_staff_id);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            switchStatus = itemView.findViewById(R.id.switch_staff_status);
            btnDetails = itemView.findViewById(R.id.btn_view_details);
            btnRemove = itemView.findViewById(R.id.btn_remove_staff);
        }
    }
}