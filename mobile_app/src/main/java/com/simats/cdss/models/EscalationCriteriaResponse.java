package com.simats.cdss.models;

import java.util.List;

public class EscalationCriteriaResponse {
    private int patient_id;
    private boolean criteria_met;
    private List<String> escalation_triggers;
    private String recommendation;

    public int getPatientId() { return patient_id; }
    public boolean isCriteriaMet() { return criteria_met; }
    public List<String> getEscalationTriggers() { return escalation_triggers; }
    public String getRecommendation() { return recommendation; }
}
