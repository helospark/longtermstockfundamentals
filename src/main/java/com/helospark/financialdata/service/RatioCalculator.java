package com.helospark.financialdata.service;

import java.time.LocalDate;
import java.util.List;
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

    public static Double calculateEpsExRnd(FinancialsTtm financialsTtm) {
        return ((double) financialsTtm.incomeStatementTtm.netIncome + financialsTtm.incomeStatementTtm.researchAndDevelopmentExpenses)
                / financialsTtm.incomeStatementTtm.weightedAverageShsOut;
    }

    public static Double calculatePriceToEarningsRatioExRnd(FinancialsTtm financialsTtm, Double price) {
        double eps = calculateEpsExRnd(financialsTtm);
        double result = price / eps;

        if (!Double.isFinite(result)) {
            return null;
        }

        return result;
    }

    public static double calculateEpsExMns(FinancialsTtm financialsTtm) {
        return ((double) financialsTtm.incomeStatementTtm.netIncome + financialsTtm.incomeStatementTtm.sellingAndMarketingExpenses) / financialsTtm.incomeStatementTtm.weightedAverageShsOut;
    }

    public static Double calculatePriceToEarningsRatioExMns(FinancialsTtm financialsTtm, Double price) {
        double eps = calculateEpsExMns(financialsTtm);
        double result = price / eps;

        if (!Double.isFinite(result)) {
            return null;
        }

        return result;
    }

    public static Double calculatePriceToEarningsRatio(double price, FinancialsTtm financialsTtm) {
        double result = price / (financialsTtm.incomeStatementTtm.eps);
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

    public static double calculateTotalPayoutRatio(FinancialsTtm data) {
        double netStockRepurchased = ((double) -data.cashFlowTtm.commonStockRepurchased - data.cashFlowTtm.commonStockIssued);
        double dividendPayed = -data.cashFlowTtm.dividendsPaid;

        double totalPayed = dividendPayed + netStockRepurchased;
        double result = totalPayed / data.incomeStatementTtm.netIncome;

        if (!Double.isFinite(result)) {
            return 0.0;
        }
        return result;
    }

    public static Double calculateTotalPayoutRatioAvg(List<FinancialsTtm> financials, int years) {
        double sum = 0.0;

        int startIndex = 0;
        int endIndex = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(years));
        if (endIndex < 0) {
            endIndex = financials.size();
        }

        int count = 0;
        for (int i = startIndex; i <= endIndex && i < financials.size(); ++i) {
            sum += calculateTotalPayoutRatio(financials.get(i));
            ++count;
        }

        return sum / count;
    }

    public static Double calculateTotalPayoutRatioFcf(FinancialsTtm data) {
        double netStockRepurchased = ((double) -data.cashFlowTtm.commonStockRepurchased - data.cashFlowTtm.commonStockIssued);
        double dividendPayed = -data.cashFlowTtm.dividendsPaid;

        double totalPayed = dividendPayed + netStockRepurchased;
        double result = totalPayed / data.cashFlowTtm.freeCashFlow;

        if (!Double.isFinite(result)) {
            return 0.0;
        }
        return result;
    }

    public static double calculateTotalPayoutRatioAvgFcf(List<FinancialsTtm> financials, int years) {
        double sum = 0.0;

        int startIndex = 0;
        int endIndex = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(years));
        if (endIndex < 0) {
            endIndex = financials.size();
        }

        int count = 0;
        for (int i = startIndex; i <= endIndex && i < financials.size(); ++i) {
            sum += calculateTotalPayoutRatioFcf(financials.get(i));
            ++count;
        }

        return sum / count;
    }

    public static double calculateAccrualRatio(FinancialsTtm financialsTtm) {
        return ((double) financialsTtm.incomeStatementTtm.netIncome - financialsTtm.cashFlowTtm.freeCashFlow) / (financialsTtm.balanceSheet.totalAssets);
    }

}
