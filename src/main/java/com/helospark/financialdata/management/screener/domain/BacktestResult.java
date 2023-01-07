package com.helospark.financialdata.management.screener.domain;

import java.util.Map;
import java.util.Set;

public class BacktestResult {
    public int beatCount;
    public int investedCount;
    public double beatPercent;

    public double screenerAvgPercent;
    public double sp500AvgPercent;

    public double screenerMedianPercent;
    public double sp500MedianPercent;

    public boolean investedInAllMatching;

    public double investedAmount;
    public double sp500Returned;
    public double screenerReturned;

    public Map<Integer, BacktestYearInformation> yearData;
    public Set<String> columns;
    public Set<String> yearDataColumns;

}
