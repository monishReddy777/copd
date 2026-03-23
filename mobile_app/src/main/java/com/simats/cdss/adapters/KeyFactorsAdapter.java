package com.simats.cdss.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.simats.cdss.R;
import com.simats.cdss.models.AIRiskResponse;

import java.util.List;

public class KeyFactorsAdapter extends RecyclerView.Adapter<KeyFactorsAdapter.ViewHolder> {

    private List<AIRiskResponse.KeyFactor> factorsList;

    public KeyFactorsAdapter(List<AIRiskResponse.KeyFactor> factorsList) {
        this.factorsList = factorsList;
    }

    public void setFactors(List<AIRiskResponse.KeyFactor> list) {
        this.factorsList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_key_factor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AIRiskResponse.KeyFactor factor = factorsList.get(position);
        holder.tvFactorName.setText(factor.getFactor());
        holder.tvFactorLevel.setText(factor.getLevel());

        String level = factor.getLevel();
        if (level != null) {
            if (level.equalsIgnoreCase("Critical")) {
                holder.tvFactorLevel.setTextColor(Color.parseColor("#EF4444"));
                holder.tvFactorLevel.setBackgroundResource(R.drawable.chip_red_rounded);
                holder.tvFactorLevel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF2F2")));
            } else if (level.equalsIgnoreCase("Warning")) {
                holder.tvFactorLevel.setTextColor(Color.parseColor("#F59E0B"));
                holder.tvFactorLevel.setBackgroundResource(R.drawable.chip_orange_rounded);
                holder.tvFactorLevel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFBEB")));
            } else {
                holder.tvFactorLevel.setTextColor(Color.parseColor("#10B981"));
                holder.tvFactorLevel.setBackgroundResource(R.drawable.chip_green_rounded);
                holder.tvFactorLevel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
            }
        }
    }

    @Override
    public int getItemCount() {
        return factorsList == null ? 0 : factorsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFactorName, tvFactorLevel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFactorName = itemView.findViewById(R.id.tv_factor_name);
            tvFactorLevel = itemView.findViewById(R.id.tv_factor_level);
        }
    }
}
