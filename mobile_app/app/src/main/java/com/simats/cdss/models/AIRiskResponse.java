package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AIRiskResponse {

    @SerializedName("risk_level")
    private String riskLevel;

    @SerializedName("confidence_score")
    private int confidenceScore;

    @SerializedName("acidosis")
    private int acidosis;

    @SerializedName("hypercapnia")
    private int hypercapnia;

    @SerializedName("key_factors")
    private List<KeyFactor> keyFactors;

    @SerializedName("message")
    private String message;

    public String getRiskLevel() { return riskLevel; }
    public int getConfidenceScore() { return confidenceScore; }
    public int getAcidosis() { return acidosis; }
    public int getHypercapnia() { return hypercapnia; }
    public List<KeyFactor> getKeyFactors() { return keyFactors; }
    public String getMessage() { return message; }

    public static class KeyFactor {
        @SerializedName("factor")
        private String factor;

        @SerializedName("level")
        private String level;

        public String getFactor() { return factor; }
        public String getLevel() { return level; }
    }
}
