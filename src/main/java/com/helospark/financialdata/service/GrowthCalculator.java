package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.util.List;
import java.util.Optional;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.FinancialsTtm;

public class GrowthCalculator {

    public static Optional<Double> getFcfGrowthInInterval(List<FinancialsTtm> financials, double years, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (years * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double now = getFcfPerShare(financials.get(newIndex));
        double then = getFcfPerShare(financials.get(oldIndex));

        double distance = years - offset;
        double resultPercent = calculatePercentChange(now, then, distance);

        return Optional.of(resultPercent);
    }

    public static double getFcfPerShare(FinancialsTtm financials) {
        long fcf = financials.cashFlowTtm.freeCashFlow;
        double fcfPerShare = (double) fcf / financials.incomeStatementTtm.weightedAverageShsOut;

        return fcfPerShare;
    }

    public static Optional<Double> getGrowthInInterval(List<FinancialsTtm> financials, double year, double offsetYear) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (year * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (offsetYear * 12.0)));

        if (oldIndex >= financials.size() || oldIndex < 0 ||
                newIndex > financials.size() || newIndex == -1) {
            return Optional.empty();
        }

        double now = minEpsOf(financials, newIndex, newIndex);
        double then = maxEpsOf(financials, oldIndex, oldIndex);

        double distance = year - offsetYear;
        double resultPercent = calculatePercentChange(now, then, distance);

        if (!Double.isFinite(resultPercent)) {
            return Optional.empty();
        }

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

    public static Optional<Double> getRevenueGrowthInInterval(List<FinancialsTtm> financials, double years, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (years * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1 || financials.get(oldIndex).incomeStatementTtm.revenue <= 0 ||
                newIndex >= financials.size() || newIndex == -1) {
            return Optional.empty();
        }

        FinancialsTtm now = financials.get(newIndex);
        FinancialsTtm then = financials.get(oldIndex);

        double resultPercent = (Math.pow((double) now.incomeStatementTtm.revenue / then.incomeStatementTtm.revenue, 1.0 / (years - offset)) - 1.0) * 100.0;

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getPriceGrowthInInterval(List<FinancialsTtm> financials, double years, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (years * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double now = financials.get(newIndex).price;
        double then = financials.get(oldIndex).price;

        double distance = years - offset;
        double resultPercent = calculatePercentChange(now, then, distance);

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getShareCountGrowthInInterval(List<FinancialsTtm> financials, double years, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (years * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double now = financials.get(newIndex).incomeStatementTtm.weightedAverageShsOut;
        double then = financials.get(oldIndex).incomeStatementTtm.weightedAverageShsOut;

        double distance = years - offset;
        double resultPercent = calculatePercentChange(now, then, distance);

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getDividendGrowthInInterval(List<FinancialsTtm> financials, double years, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (years * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        FinancialsTtm nowFinancialTtm = financials.get(newIndex);
        FinancialsTtm thenFinancialTtm = financials.get(oldIndex);
        double now = (double) -nowFinancialTtm.cashFlowTtm.dividendsPaid / nowFinancialTtm.incomeStatementTtm.weightedAverageShsOut;
        double then = (double) -thenFinancialTtm.cashFlowTtm.dividendsPaid / thenFinancialTtm.incomeStatementTtm.weightedAverageShsOut;

        double distance = years - offset;
        double resultPercent = calculatePercentChange(now, then, distance);

        if (Double.isFinite(resultPercent)) {
            return Optional.of(resultPercent);
        } else {
            return Optional.empty();
        }
    }

    public static double calculateGrowth(double now, double then, double yearsAgo) {
        return calculatePercentChange(now, then, yearsAgo);
    }

    private static double calculatePercentChange(double now, double then, double distance) {
        if (now < 0.0 && then < 0.0) {
            return (Math.pow(then / now, 1.0 / distance) - 1.0) * 100.0;
        } else if (now > 0.0 && then > 0.0) {
            return (Math.pow(now / then, 1.0 / distance) - 1.0) * 100.0;
        } else if (now > 0.0 && then < 0.0) {
            return (Math.pow(-then / now, 1.0 / distance) - 2.0) * 100.0;
        } else {
            return (Math.pow(-now / then, 1.0 / distance)) * 100.0;
        }
    }

}
