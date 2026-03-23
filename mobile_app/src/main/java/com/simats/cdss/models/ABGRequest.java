package com.simats.cdss.models;

public class ABGRequest {
    private double ph;
    private double pao2;
    private double paco2;
    private double hco3;
    private double fio2;

    public ABGRequest(double ph, double pao2, double paco2, double hco3, double fio2) {
        this.ph = ph;
        this.pao2 = pao2;
        this.paco2 = paco2;
        this.hco3 = hco3;
        this.fio2 = fio2;
    }
}
