package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StaffAlertsResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("total_count")
    private int totalCount;

    @SerializedName("critical_count")
    private int criticalCount;

    @SerializedName("moderate_count")
    private int moderateCount;

    @SerializedName("critical_alerts")
    private List<ReassessmentAlert> criticalAlerts;

    @SerializedName("moderate_alerts")
    private List<ReassessmentAlert> moderateAlerts;

    public String getStatus() { return status; }
    public int getTotalCount() { return totalCount; }
    public int getCriticalCount() { return criticalCount; }
    public int getModerateCount() { return moderateCount; }
    public List<ReassessmentAlert> getCriticalAlerts() { return criticalAlerts; }
    public List<ReassessmentAlert> getModerateAlerts() { return moderateAlerts; }

    public static class ReassessmentAlert {
        @SerializedName("id")
        private int id;

        @SerializedName("patient_id")
        private int patientId;

        @SerializedName("patient_name")
        private String patientName;

        @SerializedName("bed_no")
        private String bedNo;

        @SerializedName("ward_no")
        private String wardNo;

        @SerializedName("reassessment_type")
        private String reassessmentType;

        @SerializedName("scheduled_time")
        private String scheduledTime;

        @SerializedName("due_in")
        private int dueIn;

        @SerializedName("status")
        private String status;

        @SerializedName("severity")
        private String severity;

        @SerializedName("scheduled_by")
        private String scheduledBy;

        public int getId() { return id; }
        public int getPatientId() { return patientId; }
        public String getPatientName() { return patientName; }
        public String getBedNo() { return bedNo; }
        public String getWardNo() { return wardNo; }
        public String getReassessmentType() { return reassessmentType; }
        public String getScheduledTime() { return scheduledTime; }
        public int getDueIn() { return dueIn; }
        public String getStatus() { return status; }
        public String getSeverity() { return severity; }
        public String getScheduledBy() { return scheduledBy; }
    }
}
