package com.helospark.financialdata.management.screener.domain;

import java.util.Map;
import java.util.Set;

public class BacktestResult {
    public int beatCount;
    public int investedCount;
    public double beatPercent;

    public double screenerAvgPercent;
    public double sp500AvgPercent;

    public double screenerWithDividendsAvgPercent;
    public double sp500WithDividendsAvgPercent;

    public double screenerMedianPercent;
    public double sp500MedianPercent;

    public double screenerWithDividendsMedianPercent;
    public double sp500WithDividendsMedianPercent;

    public boolean investedInAllMatching;

    public double investedAmount;

    public double sp500Returned;
    public double sp500ReturnedWithDividends;

    public double screenerReturned;
    public double screenerReturnedWithDividends;

    public Map<String, BacktestYearInformation> yearData;
    public Set<String> columns;
    public Set<String> yearDataColumns;

}
