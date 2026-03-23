package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StaffReassessmentValuesResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("patient_id")
    private int patientId;

    @SerializedName("patient_name")
    private String patientName;

    @SerializedName("count")
    private int count;

    @SerializedName("data")
    private List<StaffEntry> data;

    public String getStatus() { return status; }
    public int getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public int getCount() { return count; }
    public List<StaffEntry> getData() { return data; }

    public static class StaffEntry {
        @SerializedName("id")
        private int id;

        @SerializedName("patient_id")
        private int patientId;

        @SerializedName("patient_name")
        private String patientName;

        @SerializedName("reassessment_id")
        private int reassessmentId;

        @SerializedName("reassessment_type")
        private String reassessmentType;

        @SerializedName("spo2")
        private double spo2;

        @SerializedName("respiratory_rate")
        private double respiratoryRate;

        @SerializedName("heart_rate")
        private Double heartRate;

        @SerializedName("abg_values")
        private String abgValues;

        @SerializedName("remarks")
        private String remarks;

        @SerializedName("entered_by")
        private String enteredBy;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("schedule_info")
        private ScheduleInfo scheduleInfo;

        public int getId() { return id; }
        public int getPatientId() { return patientId; }
        public String getPatientName() { return patientName; }
        public int getReassessmentId() { return reassessmentId; }
        public String getReassessmentType() { return reassessmentType; }
        public double getSpo2() { return spo2; }
        public double getRespiratoryRate() { return respiratoryRate; }
        public Double getHeartRate() { return heartRate; }
        public String getAbgValues() { return abgValues; }
        public String getRemarks() { return remarks; }
        public String getEnteredBy() { return enteredBy; }
        public String getCreatedAt() { return createdAt; }
        public ScheduleInfo getScheduleInfo() { return scheduleInfo; }
    }

    public static class ScheduleInfo {
        @SerializedName("reassessment_type")
        private String reassessmentType;

        @SerializedName("scheduled_time")
        private String scheduledTime;

        @SerializedName("scheduled_by")
        private String scheduledBy;

        public String getReassessmentType() { return reassessmentType; }
        public String getScheduledTime() { return scheduledTime; }
        public String getScheduledBy() { return scheduledBy; }
    }
}
