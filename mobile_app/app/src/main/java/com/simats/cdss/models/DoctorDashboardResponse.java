package com.simats.cdss.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DoctorDashboardResponse {

    @SerializedName("doctor")
    private DoctorInfo doctor;

    @SerializedName("summary")
    private Summary summary;

    @SerializedName("patients")
    private List<PatientItem> patients;

    public DoctorInfo getDoctor() { return doctor; }
    public Summary getSummary() { return summary; }
    public List<PatientItem> getPatients() { return patients; }

    public static class DoctorInfo {
        @SerializedName("name")
        private String name;

        public String getName() { return name; }
    }

    public static class Summary {
        @SerializedName("total_patients")
        private int totalPatients;

        @SerializedName("critical")
        private int critical;

        @SerializedName("warning")
        private int warning;

        public int getTotalPatients() { return totalPatients; }
        public int getCritical() { return critical; }
        public int getWarning() { return warning; }
    }

    public static class PatientItem {
        @SerializedName("id")
        private int id;

        @SerializedName("name")
        private String name;

        @SerializedName("room")
        private String room;

        @SerializedName("spo2")
        private int spo2;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getRoom() { return room; }
        public int getSpo2() { return spo2; }
    }
}
