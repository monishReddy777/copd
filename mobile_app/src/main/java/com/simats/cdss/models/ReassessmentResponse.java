package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class ReassessmentResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private ReassessmentData data;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public ReassessmentData getData() { return data; }

    public static class ReassessmentData {
        @SerializedName("id")
        private int id;

        @SerializedName("patient_id")
        private int patientId;

        @SerializedName("spo2")
        private double spo2;

        @SerializedName("respiratory_rate")
        private double respiratoryRate;

        @SerializedName("heart_rate")
        private double heartRate;

        @SerializedName("notes")
        private String notes;

        @SerializedName("reassessment_time")
        private String reassessmentTime;

        public int getId() { return id; }
        public int getPatientId() { return patientId; }
        public double getSpo2() { return spo2; }
        public double getRespiratoryRate() { return respiratoryRate; }
        public double getHeartRate() { return heartRate; }
        public String getNotes() { return notes; }
        public String getReassessmentTime() { return reassessmentTime; }
    }
}
