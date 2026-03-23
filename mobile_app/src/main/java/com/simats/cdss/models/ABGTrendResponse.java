package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ABGTrendResponse {

    @SerializedName("patient_id")
    private int patientId;

    @SerializedName("patient_name")
    private String patientName;

    @SerializedName("diagnosis")
    private String diagnosis;

    @SerializedName("status")
    private String status;

    @SerializedName("abg_trend_data")
    private List<ABGEntry> abgTrendData;

    @SerializedName("total_entries")
    private int totalEntries;

    @SerializedName("trend_analysis")
    private String trendAnalysis;

    public int getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getDiagnosis() { return diagnosis; }
    public String getStatus() { return status; }
    public List<ABGEntry> getAbgTrendData() { return abgTrendData; }
    public int getTotalEntries() { return totalEntries; }
    public String getTrendAnalysis() { return trendAnalysis; }

    public static class ABGEntry {
        @SerializedName("id")
        private int id;

        @SerializedName("ph")
        private double ph;

        @SerializedName("pao2")
        private double pao2;

        @SerializedName("paco2")
        private double paco2;

        @SerializedName("hco3")
        private double hco3;

        @SerializedName("fio2")
        private double fio2;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("time_label")
        private String timeLabel;

        @SerializedName("date_label")
        private String dateLabel;

        public int getId() { return id; }
        public double getPh() { return ph; }
        public double getPao2() { return pao2; }
        public double getPaco2() { return paco2; }
        public double getHco3() { return hco3; }
        public double getFio2() { return fio2; }
        public String getCreatedAt() { return createdAt; }
        public String getTimeLabel() { return timeLabel; }
        public String getDateLabel() { return dateLabel; }
    }
}
