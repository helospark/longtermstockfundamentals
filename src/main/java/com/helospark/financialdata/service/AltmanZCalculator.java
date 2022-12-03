package com.helospark.financialdata.service;

import com.helospark.financialdata.domain.BalanceSheet;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.IncomeStatement;

public class AltmanZCalculator {

    public static double calculateAltmanZScore(FinancialsTtm financialsTtm, double latestPrice) {
        BalanceSheet balanceSheet = financialsTtm.balanceSheet;
        IncomeStatement incomeStatement = financialsTtm.incomeStatementTtm;
        double workingCapital = balanceSheet.totalCurrentAssets - balanceSheet.totalCurrentLiabilities;
        double totalAssets = balanceSheet.totalAssets;
        double a = workingCapital / totalAssets;

        double retainedEarnings = balanceSheet.retainedEarnings;
        double b = retainedEarnings / totalAssets;

        double ebit = incomeStatement.incomeBeforeTax + incomeStatement.interestExpense;
        double c = ebit / totalAssets;

        double marketValueOfEquity = incomeStatement.weightedAverageShsOut * latestPrice;
        double totalLiabilities = balanceSheet.totalLiabilities;
        double d = marketValueOfEquity / totalLiabilities;

        double sales = incomeStatement.revenue;
        double e = sales / totalAssets;
        return 1.2 * a + 1.4 * b + 3.3 * c + 0.6 * d + 1.0 * e;
    }

}
