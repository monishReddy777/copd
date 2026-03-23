package com.simats.cdss.models;

public class UserDetailsResponse {
    private int id;
    private String username;
    private String email;
    private String first_name;
    private String last_name;
    private String role;
    private String phone_number;
    private String department;
    private boolean is_approved;
    private boolean is_active;

    public int getId() { return id; }
    public String getUsername() { return username != null ? username : ""; }
    public String getEmail() { return email != null ? email : "N/A"; }
    public String getFirstName() { return first_name != null ? first_name : ""; }
    public String getLastName() { return last_name != null ? last_name : ""; }
    public String getRole() { return role; }
    public String getPhoneNumber() { return phone_number != null ? phone_number : "N/A"; }
    public String getDepartment() { return department != null ? department : "N/A"; }
    public boolean isApproved() { return is_approved; }
    public boolean isActive() { return is_active; }

    public String getFullName() {
        if (first_name != null && !first_name.isEmpty()) {
            return first_name + (last_name != null ? " " + last_name : "");
        }
        return getUsername();
    }
}
