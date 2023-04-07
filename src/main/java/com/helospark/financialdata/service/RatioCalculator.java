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
        return calculatePriceToBookRatio(data, data.price);
    }

    public static double calculatePriceToBookRatio(FinancialsTtm data, double price) {
        return price / calculateBookValuePerShare(data);
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
        double result = (double) data.incomeStatementTtm.grossProfit / data.incomeStatementTtm.revenue;
        if (!Double.isFinite(result)) {
            return Double.NaN;
        }
        return result;
    }

    public static double calculateNetMargin(FinancialsTtm data) {
        double result = (double) data.incomeStatementTtm.netIncome / data.incomeStatementTtm.revenue;
        if (!Double.isFinite(result)) {
            return Double.NaN;
        }
        return result;
    }

    public static double calculateFcfMargin(FinancialsTtm data) {
        double result = (double) data.cashFlowTtm.freeCashFlow / data.incomeStatementTtm.revenue;
        if (!Double.isFinite(result)) {
            return Double.NaN;
        }
        return result;
    }

    public static double calculateOperatingCashflowMargin(FinancialsTtm data) {
        double result = (double) data.cashFlowTtm.operatingCashFlow / data.incomeStatementTtm.revenue;
        if (!Double.isFinite(result)) {
            return Double.NaN;
        }
        return result;
    }

    public static double calculateOperatingMargin(FinancialsTtm data) {
        double result = (double) data.incomeStatementTtm.operatingIncome / data.incomeStatementTtm.revenue;
        if (!Double.isFinite(result)) {
            return Double.NaN;
        }
        return result;
    }

    public static double calculateAssetTurnover(FinancialsTtm data) {
        return (double) data.incomeStatementTtm.revenue / data.balanceSheet.totalAssets;
    }

    public static Double calculateFcfPayoutRatio(FinancialsTtm financialsTtm) {
        return (double) -financialsTtm.cashFlowTtm.dividendsPaid / financialsTtm.cashFlowTtm.freeCashFlow;
    }

    public static double calculateSloanPercent(FinancialsTtm financialsTtm) {
        return (((double) (financialsTtm.incomeStatementTtm.netIncome - financialsTtm.cashFlowTtm.operatingCashFlow - financialsTtm.cashFlowTtm.netCashUsedForInvestingActivites))
                / financialsTtm.balanceSheet.totalAssets) * 100.0;
    }

    public static double calculatePriceToSalesRatio(FinancialsTtm financial, double latestPrice) {
        double marketCap = financial.incomeStatementTtm.weightedAverageShsOut * latestPrice;
        double result = marketCap / financial.incomeStatementTtm.revenue;
        return result;
    }

    public static Double calculateInterestCoverageRatio(FinancialsTtm financialsTtm) {
        Double coverage = null;
        if (financialsTtm.incomeStatementTtm.interestExpense > 0) {
            double ebit = RoicCalculator.calculateEbit(financialsTtm);
            coverage = (ebit / financialsTtm.incomeStatementTtm.interestExpense);
        }
        return coverage;
    }

    public static double calculateDebtToEquityRatio(FinancialsTtm financial) {
        double debt = financial.balanceSheet.totalDebt;
        double equity = financial.balanceSheet.totalStockholdersEquity;
        return debt / equity;
    }

}
