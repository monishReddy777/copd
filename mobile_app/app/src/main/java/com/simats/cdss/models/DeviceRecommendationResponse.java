package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class DeviceRecommendationResponse {

    @SerializedName("patient_id")
    private int patientId;

    @SerializedName("recommended_device")
    private String recommendedDevice;

    @SerializedName("target_spo2")
    private String targetSpo2;

    @SerializedName("flow_range")
    private String flowRange;

    @SerializedName("confidence_score")
    private double confidenceScore;

    public int getPatientId() { return patientId; }
    public String getRecommendedDevice() { return recommendedDevice; }
    public String getTargetSpo2() { return targetSpo2; }
    public String getFlowRange() { return flowRange; }
    public double getConfidenceScore() { return confidenceScore; }
}
