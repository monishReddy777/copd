package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    // Old fields (JWT tokens) - kept for backward compatibility
    private String access;
    private String refresh;

    // New unified auth fields
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("role")
    private String role;

    @SerializedName("email")
    private String email;

    @SerializedName("user_id")
    private int user_id;

    @SerializedName("name")
    private String name;

    @SerializedName("otp")
    private String otp;

    @SerializedName("verified")
    private boolean verified;

    @SerializedName("token")
    private String token;

    // Getters
    public String getAccess() { return access; }
    public String getRefresh() { return refresh; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getRole() { return role; }
    public String getEmail() { return email; }
    public int getUserId() { return user_id; }
    public String getName() { return name; }
    public String getOtp() { return otp; }
    public boolean isVerified() { return verified; }
    public String getToken() { return token; }
}
