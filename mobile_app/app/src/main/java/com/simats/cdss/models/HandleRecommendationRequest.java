package com.simats.cdss.models;

public class HandleRecommendationRequest {
    private String action;
    private String reason;

    public HandleRecommendationRequest(String action, String reason) {
        this.action = action;
        this.reason = reason;
    }
}
