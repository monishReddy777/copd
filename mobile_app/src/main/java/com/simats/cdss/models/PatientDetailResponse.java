package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class PatientDetailResponse {

    @SerializedName("patient_id")
    private int patientId;

    @SerializedName("name")
    private String name;

    @SerializedName("ward_no")
    private String wardNo;

    @SerializedName("age")
    private Integer age;

    @SerializedName("gender")
    private String gender;

    @SerializedName("diagnosis")
    private String diagnosis;

    @SerializedName("target_spo2")
    private String targetSpo2;

    @SerializedName("room_no")
    private String roomNo;

    @SerializedName("spo2")
    private String spo2;

    @SerializedName("respiratory_rate")
    private String respiratoryRate;

    @SerializedName("heart_rate")
    private String heartRate;

    @SerializedName("abg_values")
    private Map<String, Object> abgValues;

    @SerializedName("device")
    private String device;

    @SerializedName("flow")
    private String flow;

    @SerializedName("status")
    private String status;

    // Getters
    public int getPatientId() { return patientId; }
    public String getName() { return name; }
    public String getWardNo() { return wardNo; }
    public Integer getAge() { return age; }
    public String getGender() { return gender; }
    public String getDiagnosis() { return diagnosis; }
    public String getTargetSpo2() { return targetSpo2; }
    public String getRoomNo() { return roomNo; }
    public String getSpo2() { return spo2; }
    public String getRespiratoryRate() { return respiratoryRate; }
    public String getHeartRate() { return heartRate; }
    public Map<String, Object> getAbgValues() { return abgValues; }
    public String getDevice() { return device; }
    public String getFlow() { return flow; }
    public String getStatus() { return status; }
}
