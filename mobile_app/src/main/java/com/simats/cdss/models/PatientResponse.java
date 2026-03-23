package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class PatientResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("patient_id")
    private int patientId;

    @SerializedName("name")
    private String name;

    @SerializedName("ward_no")
    private String wardNo;

    @SerializedName("room_no")
    private String roomNo;

    @SerializedName("status")
    private String status;

    @SerializedName("spo2")
    private String spo2;

    @SerializedName("respiratory_rate")
    private String respiratoryRate;

    // Getters
    public String getMessage() { return message; }
    public int getPatientId() { return patientId; }
    public String getName() { return name; }
    public String getWardNo() { return wardNo; }
    public String getRoomNo() { return roomNo; }
    public String getSpo2() { return spo2; }
    public String getRespiratoryRate() { return respiratoryRate; }
    public String getStatus() { return status; }
}
