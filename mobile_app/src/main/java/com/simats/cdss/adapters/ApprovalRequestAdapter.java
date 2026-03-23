package com.simats.cdss.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.simats.cdss.R;
import com.simats.cdss.models.ApprovalRequest;

import java.util.ArrayList;
import java.util.List;

public class ApprovalRequestAdapter extends RecyclerView.Adapter<ApprovalRequestAdapter.ViewHolder> {

    private List<ApprovalRequest> requestList;
    private List<ApprovalRequest> fullList;
    private final OnActionClickListener listener;

    public interface OnActionClickListener {
        void onApproveClick(ApprovalRequest request, int position);
        void onRejectClick(ApprovalRequest request, int position);
    }

    public ApprovalRequestAdapter(List<ApprovalRequest> requestList, OnActionClickListener listener) {
        this.requestList = new ArrayList<>(requestList);
        this.fullList = new ArrayList<>(requestList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_approval_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApprovalRequest request = requestList.get(position);

        holder.tvName.setText(request.getName());
        holder.tvEmail.setText(request.getEmail());
        holder.tvRole.setText("Role: " + request.getRole());
        holder.tvStatus.setText("PENDING");

        if (request.getLicense() != null && !request.getLicense().isEmpty()) {
            holder.tvLicense.setVisibility(View.VISIBLE);
            holder.tvLicense.setText("License: " + request.getLicense());
        } else {
            holder.tvLicense.setVisibility(View.GONE);
        }

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApproveClick(request, holder.getAdapterPosition());
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRejectClick(request, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < requestList.size()) {
            requestList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, requestList.size());
        }
    }

    public void updateList(List<ApprovalRequest> newList) {
        this.requestList = new ArrayList<>(newList);
        this.fullList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            requestList = new ArrayList<>(fullList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            List<ApprovalRequest> filtered = new ArrayList<>();
            for (ApprovalRequest r : fullList) {
                if ((r.getName() != null && r.getName().toLowerCase().contains(lowerQuery)) ||
                    (r.getEmail() != null && r.getEmail().toLowerCase().contains(lowerQuery)) ||
                    (r.getRole() != null && r.getRole().toLowerCase().contains(lowerQuery))) {
                    filtered.add(r);
                }
            }
            requestList = filtered;
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole, tvLicense, tvStatus;
        MaterialButton btnApprove, btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_request_name);
            tvEmail = itemView.findViewById(R.id.tv_request_email);
            tvRole = itemView.findViewById(R.id.tv_request_role);
            tvLicense = itemView.findViewById(R.id.tv_request_license);
            tvStatus = itemView.findViewById(R.id.tv_request_status);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}
