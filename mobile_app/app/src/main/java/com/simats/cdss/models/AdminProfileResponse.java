package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class AdminProfileResponse {

    @SerializedName("admin_id")
    private int adminId;

    @SerializedName("username")
    private String username;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("permissions")
    private String permissions;

    public int getAdminId() { return adminId; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getPermissions() { return permissions; }
}
