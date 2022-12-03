package com.helospark.financialdata.domain;

public class MeanAvg {
    public double mean;
    public double avg;

    public MeanAvg(Double meanDividend, double avgDividend) {
        this.mean = meanDividend;
        this.avg = avgDividend;
    }

    @Override
    public String toString() {
        return "MeanAvg [mean=" + mean + ", avg=" + avg + "]";
    }

}