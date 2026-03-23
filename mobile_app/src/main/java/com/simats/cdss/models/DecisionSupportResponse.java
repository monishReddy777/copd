package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class DecisionSupportResponse {

    @SerializedName("has_data")
    private boolean hasData;

    @SerializedName("message")
    private String message;

    @SerializedName("risk_level")
    private String riskLevel;

    @SerializedName("confidence_score")
    private int confidenceScore;

    @SerializedName("acidosis")
    private int acidosis;

    @SerializedName("hypercapnia")
    private int hypercapnia;

    @SerializedName("overall_status")
    private String overallStatus;

    @SerializedName("paco2_status")
    private String paco2Status;

    @SerializedName("ph_status")
    private String phStatus;

    @SerializedName("spo2_status")
    private String spo2Status;

    @SerializedName("recommendation")
    private String recommendation;

    @SerializedName("action_level")
    private String actionLevel;

    public boolean isHasData() { return hasData; }
    public String getMessage() { return message; }
    public String getRiskLevel() { return riskLevel; }
    public int getConfidenceScore() { return confidenceScore; }
    public int getAcidosis() { return acidosis; }
    public int getHypercapnia() { return hypercapnia; }
    public String getOverallStatus() { return overallStatus; }
    public String getPaco2Status() { return paco2Status; }
    public String getPhStatus() { return phStatus; }
    public String getSpo2Status() { return spo2Status; }
    public String getRecommendation() { return recommendation; }
    public String getActionLevel() { return actionLevel; }
}
