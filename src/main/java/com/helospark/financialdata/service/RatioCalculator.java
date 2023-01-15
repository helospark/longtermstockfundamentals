package com.helospark.financialdata.service;

import java.util.Optional;

import com.helospark.financialdata.domain.FinancialsTtm;

public class RatioCalculator {

    public static Optional<Double> calculateCurrentRatio(FinancialsTtm data) {
        if (data.balanceSheet.totalCurrentLiabilities == 0) {
            return Optional.empty();
        }
        return Optional.of((double) data.balanceSheet.totalCurrentAssets / data.balanceSheet.totalCurrentLiabilities);
    }

    public static Optional<Double> calculateQuickRatio(FinancialsTtm data) {
        if (data.balanceSheet.totalCurrentLiabilities == 0) {
            return Optional.empty();
        }
        return Optional.of(((double) data.balanceSheet.cashAndCashEquivalents + data.balanceSheet.shortTermInvestments + data.balanceSheet.netReceivables) / data.balanceSheet.totalCurrentLiabilities);
    }

    public static double calculatePriceToBookRatio(FinancialsTtm data) {
        return data.price / calculateBookValuePerShare(data);
    }

    public static double calculatePriceToTangibleBookRatio(FinancialsTtm data) {
        double tangibleBookValue = ((double) data.balanceSheet.totalStockholdersEquity - data.balanceSheet.goodwillAndIntangibleAssets) / data.incomeStatement.weightedAverageShsOut;
        return data.price / tangibleBookValue;
    }

    public static double calculateBookValuePerShare(FinancialsTtm data) {
        return (double) data.balanceSheet.totalStockholdersEquity / data.incomeStatement.weightedAverageShsOut;
    }

    public static double calculatePayoutRatio(FinancialsTtm data) {
        return (double) -data.cashFlowTtm.dividendsPaid / data.incomeStatementTtm.netIncome;
    }

    public static Double calculatePriceToEarningsRatio(FinancialsTtm financialsTtm) {
        double result = financialsTtm.price / financialsTtm.incomeStatementTtm.eps;
        if (!Double.isFinite(result)) {
            return null;
        }
        return result;
    }

    public static double calculateGrossProfitMargin(FinancialsTtm data) {
        return (double) data.incomeStatementTtm.grossProfit / data.incomeStatementTtm.revenue;
    }

    public static double calculateAssetTurnover(FinancialsTtm data) {
        return (double) data.incomeStatementTtm.revenue / data.balanceSheet.totalAssets;
    }

    public static Double calculateFcfPayoutRatio(FinancialsTtm financialsTtm) {
        return (double) -financialsTtm.cashFlowTtm.dividendsPaid / financialsTtm.cashFlowTtm.freeCashFlow;
    }

}
