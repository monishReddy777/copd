package com.simats.cdss.models;

public class VitalsRequest {
    private double spo2;
    private int resp_rate;
    private int heart_rate;
    private double temperature;
    private String bp;

    public VitalsRequest(double spo2, int resp_rate, int heart_rate, double temperature, String bp) {
        this.spo2 = spo2;
        this.resp_rate = resp_rate;
        this.heart_rate = heart_rate;
        this.temperature = temperature;
        this.bp = bp;
    }

    public double getSpo2() { return spo2; }
    public int getResp_rate() { return resp_rate; }
    public int getHeart_rate() { return heart_rate; }
    public double getTemperature() { return temperature; }
    public String getBp() { return bp; }
}
