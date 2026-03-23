package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class TrendAnalysisResponse {

    @SerializedName("overall_status")
    private String overallStatus;

    @SerializedName("paco2_status")
    private String paco2Status;

    @SerializedName("ph_status")
    private String phStatus;

    @SerializedName("spo2_status")
    private String spo2Status;

    public String getOverallStatus() {
        return overallStatus;
    }

    public String getPaco2Status() {
        return paco2Status;
    }

    public String getPhStatus() {
        return phStatus;
    }

    public String getSpo2Status() {
        return spo2Status;
    }

    public static class TrendIndicator {
        private String factor;
        private String description;
        private String status;

        public TrendIndicator(String factor, String description, String status) {
            this.factor = factor;
            this.description = description;
            this.status = status;
        }

        public String getFactor() {
            return factor;
        }

        public String getDescription() {
            return description;
        }

        public String getStatus() {
            return status;
        }
    }
}
