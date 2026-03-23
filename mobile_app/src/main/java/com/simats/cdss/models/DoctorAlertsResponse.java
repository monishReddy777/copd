package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DoctorAlertsResponse {

    @SerializedName("unread_count")
    private int unreadCount;

    @SerializedName("critical_alerts")
    private List<AlertItem> criticalAlerts;

    @SerializedName("warning_alerts")
    private List<AlertItem> warningAlerts;

    @SerializedName("info_alerts")
    private List<AlertItem> infoAlerts;

    public int getUnreadCount() { return unreadCount; }
    public List<AlertItem> getCriticalAlerts() { return criticalAlerts; }
    public List<AlertItem> getWarningAlerts() { return warningAlerts; }
    public List<AlertItem> getInfoAlerts() { return infoAlerts; }

    public static class AlertItem {
        @SerializedName("id")
        private int id;

        @SerializedName("patient_id")
        private int patientId;

        @SerializedName("patient_name")
        private String patientName;

        @SerializedName("bed_number")
        private String bedNumber;

        @SerializedName("ward_no")
        private String wardNo;

        @SerializedName("alert_type")
        private String alertType;

        @SerializedName("severity")
        private String severity;

        @SerializedName("message")
        private String message;

        @SerializedName("status")
        private String status;

        @SerializedName("time_ago")
        private String timeAgo;

        @SerializedName("created_at")
        private String createdAt;

        public int getId() { return id; }
        public int getPatientId() { return patientId; }
        public String getPatientName() { return patientName; }
        public String getBedNumber() { return bedNumber; }
        public String getWardNo() { return wardNo; }
        public String getAlertType() { return alertType; }
        public String getSeverity() { return severity; }
        public String getMessage() { return message; }
        public String getStatus() { return status; }
        public String getTimeAgo() { return timeAgo; }
        public String getCreatedAt() { return createdAt; }
    }
}
