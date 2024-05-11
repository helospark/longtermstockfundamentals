package com.helospark.financialdata.util.spconstituents;

public class Sp500WeightedConstituent {
    public String symbol;
    public double weight;

    public Sp500WeightedConstituent(String symbol, double weight) {
        this.symbol = symbol;
        this.weight = weight;
    }
}
