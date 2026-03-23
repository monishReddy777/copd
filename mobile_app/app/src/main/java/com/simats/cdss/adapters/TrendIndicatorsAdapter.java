package com.simats.cdss.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.simats.cdss.R;
import com.simats.cdss.models.TrendAnalysisResponse;

import java.util.List;

public class TrendIndicatorsAdapter extends RecyclerView.Adapter<TrendIndicatorsAdapter.ViewHolder> {

    private List<TrendAnalysisResponse.TrendIndicator> indicatorsList;

    public TrendIndicatorsAdapter(List<TrendAnalysisResponse.TrendIndicator> indicatorsList) {
        this.indicatorsList = indicatorsList;
    }

    public void setIndicators(List<TrendAnalysisResponse.TrendIndicator> list) {
        this.indicatorsList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trend_indicator, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrendAnalysisResponse.TrendIndicator indicator = indicatorsList.get(position);
        
        holder.tvFactor.setText(indicator.getFactor());
        holder.tvDesc.setText(indicator.getDescription());
        holder.tvStatus.setText(indicator.getStatus());

        String status = indicator.getStatus();
        if (status != null) {
            if (status.equalsIgnoreCase("Rising") || status.equalsIgnoreCase("Dropping")) {
                holder.tvStatus.setTextColor(Color.parseColor("#EF4444"));
                holder.ivIcon.setColorFilter(Color.parseColor("#EF4444"));
                holder.ivIcon.setImageResource(status.equalsIgnoreCase("Rising") ? R.drawable.ic_trending_up : R.drawable.ic_trending_down);
            } else if (status.equalsIgnoreCase("Unstable")) {
                holder.tvStatus.setTextColor(Color.parseColor("#F59E0B"));
                holder.ivIcon.setColorFilter(Color.parseColor("#F59E0B"));
                holder.ivIcon.setImageResource(R.drawable.ic_trending_unstable); // assume you have this or re-use another one
            } else { // Stable
                holder.tvStatus.setTextColor(Color.parseColor("#10B981"));
                holder.ivIcon.setColorFilter(Color.parseColor("#10B981"));
                holder.ivIcon.setImageResource(R.drawable.ic_trending_up); // adjust as needed
            }
        }
    }

    @Override
    public int getItemCount() {
        return indicatorsList == null ? 0 : indicatorsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFactor, tvDesc, tvStatus;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFactor = itemView.findViewById(R.id.tv_indicator_factor);
            tvDesc = itemView.findViewById(R.id.tv_indicator_desc);
            tvStatus = itemView.findViewById(R.id.tv_indicator_status);
            ivIcon = itemView.findViewById(R.id.iv_indicator_icon);
        }
    }
}
