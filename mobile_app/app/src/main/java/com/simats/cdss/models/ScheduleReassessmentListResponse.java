package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ScheduleReassessmentListResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("count")
    private int count;

    @SerializedName("data")
    private List<ScheduleItem> data;

    public String getStatus() { return status; }
    public int getCount() { return count; }
    public List<ScheduleItem> getData() { return data; }

    public static class ScheduleItem {
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
        public String getScheduledBy() { return scheduledBy; }
    }
}
