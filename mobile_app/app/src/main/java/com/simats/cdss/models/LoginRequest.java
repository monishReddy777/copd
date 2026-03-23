package com.simats.cdss.models;

public class LoginRequest {
    private String username;
    private String password;
    private String role;

    public LoginRequest(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
