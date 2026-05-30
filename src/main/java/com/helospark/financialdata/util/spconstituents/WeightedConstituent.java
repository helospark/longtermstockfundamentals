package com.helospark.financialdata.util.spconstituents;

public class WeightedConstituent {
    public String symbol;
    public double weight;

    public WeightedConstituent(String symbol, double weight) {
        this.symbol = symbol;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "WeightedConstituent [symbol=" + symbol + ", weight=" + weight + "]";
    }

}
