package com.helospark.financialdata.service;

import com.helospark.financialdata.domain.FinancialsTtm;

public class RevenueProjector {

    public static double projectRevenue(FinancialsTtm financials, double startGrowth, double endGrowth) {
        long lastRevenue = financials.incomeStatementTtm.revenue;
        long shareCount = financials.incomeStatementTtm.weightedAverageShsOut;
        double revenuePerShare = lastRevenue / shareCount;

        double currentRevenue = revenuePerShare;
        for (int i = 0; i < 10; ++i) {
            double currentGrowth = startGrowth - ((startGrowth - endGrowth) * i) / 9.0;
            currentRevenue *= (1.0 + currentGrowth / 100.0);
        }
        double marginToUse = 0.02;
        double operativeMargin = ((double) financials.incomeStatementTtm.operatingIncome) / financials.incomeStatementTtm.revenue;
        double grossMargin = (financials.incomeStatementTtm.grossProfitRatio);

        if (operativeMargin > 0.0) {
            marginToUse = operativeMargin * 0.8;
        } else if (grossMargin > 0.0) {
            marginToUse = grossMargin * 0.35;
        }
        if (marginToUse < 0.05) {
            marginToUse = 0.05;
        }

        double endSharePrice = currentRevenue * marginToUse * endGrowth;
        double currentSharePrice = endSharePrice / Math.pow(1.1, 10) * 0.8;
        return currentSharePrice;
    }

}
