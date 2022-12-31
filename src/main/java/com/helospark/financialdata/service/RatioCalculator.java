package com.helospark.financialdata.service;

import com.helospark.financialdata.domain.FinancialsTtm;

public class RatioCalculator {

    public static Double calculateCurrentRatio(FinancialsTtm data) {
        if (data.balanceSheet.totalCurrentLiabilities == 0) {
            return null;
        }
        return (double) data.balanceSheet.totalCurrentAssets / data.balanceSheet.totalCurrentLiabilities;
    }

    public static Double calculateQuickRatio(FinancialsTtm data) {
        if (data.balanceSheet.totalCurrentLiabilities == 0) {
            return null;
        }
        return (double) (data.balanceSheet.cashAndCashEquivalents + data.balanceSheet.shortTermInvestments + data.balanceSheet.netReceivables) / data.balanceSheet.totalCurrentLiabilities;
    }

    public static double calculatePriceToBookRatio(FinancialsTtm data) {
        return data.price / ((double) data.balanceSheet.totalStockholdersEquity / data.incomeStatement.weightedAverageShsOut);
    }

    public static double calculatePayoutRatio(FinancialsTtm data) {
        return (double) -data.cashFlowTtm.dividendsPaid / data.incomeStatementTtm.netIncome;
    }

    public static Double calculatePriceToEarningsRatio(FinancialsTtm financialsTtm) {
        return financialsTtm.price / financialsTtm.incomeStatementTtm.eps;
    }

    public static double calculateGrossProfitMargin(FinancialsTtm data) {
        return (double) data.incomeStatementTtm.grossProfit / data.incomeStatementTtm.revenue;
    }

    public static double calculateAssetTurnover(FinancialsTtm data) {
        return (double) data.incomeStatementTtm.revenue / data.balanceSheet.totalAssets;
    }

}