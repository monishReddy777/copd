package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class ApprovalRequest {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("license")
    private String license;

    @SerializedName("status")
    private String status;

    @SerializedName("user_type")
    private String userType;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getLicense() { return license; }
    public String getStatus() { return status; }
    public String getUserType() { return userType; }
}
