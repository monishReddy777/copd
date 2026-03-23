package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class DashboardStatsResponse {

    @SerializedName("total_doctors")
    private int totalDoctors;

    @SerializedName("total_staff")
    private int totalStaff;

    @SerializedName("total_pending_requests")
    private int pendingRequests;

    public int getTotalDoctors() {
        return totalDoctors;
    }

    public int getTotalStaff() {
        return totalStaff;
    }

    public int getPendingRequests() {
        return pendingRequests;
    }
}