package com.helospark.financialdata.management.watchlist.domain;

public class CalculatorParameters {
    public Double startMargin;
    public Double endMargin;

    public Double startGrowth;
    public Double endGrowth;

    public Double startShChange;
    public Double endShChange;

    public Double discount;
    public Double endMultiple;

    public Double startPayout;
    public Double endPayout;

    public String type;

    @Override
    public String toString() {
        return "CalculatorParameters [startMargin=" + startMargin + ", endMargin=" + endMargin + ", startGrowth=" + startGrowth + ", endGrowth=" + endGrowth + ", startShChange=" + startShChange
                + ", endShChange=" + endShChange + ", discount=" + discount + ", endMultiple=" + endMultiple + ", startPayout=" + startPayout + ", endPayout=" + endPayout + ", type=" + type + "]";
    }

    public static CalculatorParameters deepClone(CalculatorParameters old) {
        CalculatorParameters result = new CalculatorParameters();

        result.startMargin = old.startMargin;
        result.endMargin = old.endMargin;
        result.startGrowth = old.startGrowth;
        result.endGrowth = old.endGrowth;
        result.startShChange = old.startShChange;
        result.endShChange = old.endShChange;
        result.discount = old.discount;
        result.endMultiple = old.endMultiple;
        result.startPayout = old.startPayout;
        result.endPayout = old.endPayout;
        result.type = old.type;

        return result;
    }

}
