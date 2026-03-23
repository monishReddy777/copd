package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class StaffDetailResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("department")
    private String department;

    @SerializedName("status")
    private String status;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public String getStatus() { return status; }
}
