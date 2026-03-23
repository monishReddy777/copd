package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class ClinicalReviewResponse {

    @SerializedName("has_data")
    private boolean hasData;

    @SerializedName("message")
    private String message;

    @SerializedName("recommended_device")
    private String recommendedDevice;

    @SerializedName("fio2")
    private String fio2;

    @SerializedName("flow_rate")
    private String flowRate;

    @SerializedName("spo2")
    private double spo2;

    public boolean isHasData() { return hasData; }
    public String getMessage() { return message; }
    public String getRecommendedDevice() { return recommendedDevice; }
    public String getFio2() { return fio2; }
    public String getFlowRate() { return flowRate; }
    public double getSpo2() { return spo2; }
}
