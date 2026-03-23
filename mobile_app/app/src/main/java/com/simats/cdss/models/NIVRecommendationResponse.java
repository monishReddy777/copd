package com.simats.cdss.models;

public class NIVRecommendationResponse {
    private int patient_id;
    private String mode;
    private double ipap;
    private double epap;
    private String indication;
    private boolean bipap_indicated;

    public int getPatientId() { return patient_id; }
    public String getMode() { return mode; }
    public double getIpap() { return ipap; }
    public double getEpap() { return epap; }
    public String getIndication() { return indication; }
    public boolean isBipapIndicated() { return bipap_indicated; }
}
