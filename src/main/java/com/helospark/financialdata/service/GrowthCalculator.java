package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.helospark.financialdata.domain.FinancialsTtm;

public class GrowthCalculator {

    public static Optional<Double> getFcfGrowthInInterval(List<FinancialsTtm> financials, int years, int offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(years));
        int newIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(offset));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double now = getFcfPerShare(financials.get(newIndex));
        double then = getFcfPerShare(financials.get(oldIndex));

        int distance = years - offset;
        double resultPercent = (Math.pow(now / then, 1.0 / distance) - 1.0) * 100.0;

        return Optional.of(resultPercent);
    }

    public static double getFcfPerShare(FinancialsTtm financials) {
        long fcf = financials.cashFlowTtm.freeCashFlow;
        double fcfPerShare = (double) fcf / financials.incomeStatementTtm.weightedAverageShsOut;

        return fcfPerShare;
    }

    public static Optional<Double> getGrowthInInterval(List<FinancialsTtm> financials, int years, int offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(years));
        int newIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(offset));

        if (oldIndex >= financials.size() || oldIndex < 0 ||
                newIndex > financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double now = minEpsOf(financials, newIndex, newIndex);
        double then = maxEpsOf(financials, oldIndex, oldIndex);

        int distance = years - offset;
        double resultPercent = (Math.pow(now / then, 1.0 / distance) - 1.0) * 100.0;

        return Optional.of(resultPercent);
    }

    private static double maxEpsOf(List<FinancialsTtm> financials, int start, int end) {
        double max = financials.get(start).incomeStatementTtm.eps;
        for (int i = start + 1; i < end; ++i) {
            double newEps = financials.get(i).incomeStatementTtm.eps;
            if (newEps > max) {
                max = newEps;
            }
        }
        return max;
    }

    private static double minEpsOf(List<FinancialsTtm> financials, int start, int end) {
        double min = financials.get(start).incomeStatementTtm.eps;
        for (int i = start + 1; i < end && i < financials.size(); ++i) {
            double newEps = financials.get(i).incomeStatementTtm.eps;
            if (newEps < min) {
                min = newEps;
            }
        }
        return min;
    }

    public static Optional<Double> getRevenueGrowthInInterval(List<FinancialsTtm> financials, int years, int offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(years));
        int newIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(offset));

        if (oldIndex >= financials.size() || oldIndex == -1 || financials.get(oldIndex).incomeStatementTtm.revenue <= 0 ||
                newIndex >= financials.size() || newIndex == -1) {
            return Optional.empty();
        }

        FinancialsTtm now = financials.get(newIndex);
        FinancialsTtm then = financials.get(oldIndex);

        double resultPercent = (Math.pow((double) now.incomeStatementTtm.revenue / then.incomeStatementTtm.revenue, 1.0 / (years - offset)) - 1.0) * 100.0;

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getPriceGrowthInInterval(List<FinancialsTtm> financials, int years, int offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(years));
        int newIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(offset));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double now = financials.get(newIndex).price;
        double then = financials.get(oldIndex).price;

        int distance = years - offset;
        double resultPercent = (Math.pow(now / then, 1.0 / distance) - 1.0) * 100.0;

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getShareCountGrowthInInterval(List<FinancialsTtm> financials, int years, int offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(years));
        int newIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(offset));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double now = financials.get(newIndex).incomeStatementTtm.weightedAverageShsOut;
        double then = financials.get(oldIndex).incomeStatementTtm.weightedAverageShsOut;

        int distance = years - offset;
        double resultPercent = (Math.pow(now / then, 1.0 / distance) - 1.0) * 100.0;

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getDividendGrowthInInterval(List<FinancialsTtm> financials, int years, int offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(years));
        int newIndex = findIndexWithOrBeforeDate(financials, LocalDate.now().minusYears(offset));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        FinancialsTtm nowFinancialTtm = financials.get(newIndex);
        FinancialsTtm thenFinancialTtm = financials.get(oldIndex);
        double now = (double) -nowFinancialTtm.cashFlowTtm.dividendsPaid / nowFinancialTtm.incomeStatementTtm.weightedAverageShsOut;
        double then = (double) -thenFinancialTtm.cashFlowTtm.dividendsPaid / thenFinancialTtm.incomeStatementTtm.weightedAverageShsOut;

        int distance = years - offset;
        double resultPercent = (Math.pow(now / then, 1.0 / distance) - 1.0) * 100.0;

        return Optional.of(resultPercent);
    }

}
