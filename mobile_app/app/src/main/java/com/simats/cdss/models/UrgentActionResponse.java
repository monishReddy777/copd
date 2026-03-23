package com.simats.cdss.models;

import java.util.List;

public class UrgentActionResponse {
    private int patient_id;
    private boolean icu_required;
    private List<String> triggers;

    public int getPatientId() { return patient_id; }
    public boolean isIcuRequired() { return icu_required; }
    public List<String> getTriggers() { return triggers; }
}
