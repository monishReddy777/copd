package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class Staff {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("staff_role")
    private String staffRole;

    @SerializedName("staff_id")
    private String staffId;

    @SerializedName("phone")
    private String phone;

    @SerializedName("status")
    private String status;

    @SerializedName("is_active")
    private boolean isActive;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getStaffRole() { return staffRole; }
    public String getStaffId() { return staffId; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
