package com.helospark.financialdata.management.screener.domain;

import java.util.List;
import java.util.Map;

public class BacktestYearInformation {
    public double investedAmount;

    public double spAnnualReturnPercent;
    public double spAnnualReturnPercentWithDividends;
    public double screenerAnnualReturnPercent;
    public double screenerAnnualReturnPercentWithDividends;

    public double spTotalReturnPercent;
    public double spTotalReturnPercentWithDividends;
    public double screenerTotalReturnPercent;
    public double screenerTotalReturnPercentWithDividends;

    public double spReturnDollar;
    public double spReturnDollarWithDividends;
    public double screenerReturnDollar;
    public double screenerReturnDollarWithDividends;

    public boolean investedInAllMatching;

    public List<Map<String, String>> investedStocks;
}
