package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class TherapyPlanResponse {

    @SerializedName("has_data")
    private boolean hasData;

    @SerializedName("message")
    private String message;

    @SerializedName("device")
    private String device;

    @SerializedName("fio2")
    private String fio2;

    @SerializedName("flow_rate")
    private String flowRate;

    @SerializedName("target_spo2")
    private String targetSpo2;

    @SerializedName("next_abg_time")
    private String nextAbgTime;

    @SerializedName("rationale")
    private String rationale;

    @SerializedName("risk_level")
    private String riskLevel;

    public boolean isHasData() { return hasData; }
    public String getMessage() { return message; }
    public String getDevice() { return device; }
    public String getFio2() { return fio2; }
    public String getFlowRate() { return flowRate; }
    public String getTargetSpo2() { return targetSpo2; }
    public String getNextAbgTime() { return nextAbgTime; }
    public String getRationale() { return rationale; }
    public String getRiskLevel() { return riskLevel; }
}
