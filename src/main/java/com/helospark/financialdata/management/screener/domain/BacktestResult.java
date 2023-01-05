package com.helospark.financialdata.management.screener.domain;

import java.util.Map;

public class BacktestResult {
    public int beatCount;
    public int investedCount;
    public double beatPercent;

    public double screenerAvgPercent;
    public double sp500AvgPercent;

    public double screenerMedianPercent;
    public double sp500MedianPercent;

    public Map<Integer, BacktestYearInformation> yearData;
}
