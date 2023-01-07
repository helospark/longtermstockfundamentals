package com.helospark.financialdata.management.screener.domain;

import java.util.List;
import java.util.Map;

public class BacktestYearInformation {
    public double investedAmount;

    public double spAnnualReturnPercent;
    public double screenerAnnualReturnPercent;

    public double spTotalReturnPercent;
    public double screenerTotalReturnPercent;

    public double spReturnDollar;
    public double screenerReturnDollar;

    public boolean investedInAllMatching;

    public List<Map<String, String>> investedStocks;
}
