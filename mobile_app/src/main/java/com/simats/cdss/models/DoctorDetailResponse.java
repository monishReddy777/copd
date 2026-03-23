package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class DoctorDetailResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("specialization")
    private String specialization;

    @SerializedName("license_number")
    private String licenseNumber;

    @SerializedName("phone")
    private String phone;

    @SerializedName("status")
    private String status;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getSpecialization() { return specialization; }
    public String getLicenseNumber() { return licenseNumber; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }
}
