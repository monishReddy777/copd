package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class PatientRequest {

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("date_of_birth")
    private String dateOfBirth;

    @SerializedName("sex")
    private String sex;

    @SerializedName("ward")
    private String ward;

    @SerializedName("bed_number")
    private String bedNumber;

    public PatientRequest(String fullName, String dateOfBirth, String sex, String ward, String bedNumber) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.sex = sex;
        this.ward = ward;
        this.bedNumber = bedNumber;
    }

    // Getters
    public String getFullName() { return fullName; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getSex() { return sex; }
    public String getWard() { return ward; }
    public String getBedNumber() { return bedNumber; }
}
