package com.simats.cdss.models;

public class RecommendationResponse {
    private int id;
    private String rec_type;
    private String content;
    private String status;

    public int getId() { return id; }
    public String getRec_type() { return rec_type; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
}
