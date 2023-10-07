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

    @Override
    public String toString() {
        return "CalculatorParameters [startMargin=" + startMargin + ", endMargin=" + endMargin + ", startGrowth=" + startGrowth + ", endGrowth=" + endGrowth + ", startShChange=" + startShChange
                + ", endShChange=" + endShChange + ", discount=" + discount + ", endMultiple=" + endMultiple + ", startPayout=" + startPayout + ", endPayout=" + endPayout + "]";
    }

}
