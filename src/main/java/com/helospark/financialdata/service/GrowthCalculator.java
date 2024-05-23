package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.DateAware;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.SimpleDateDataElement;

public class GrowthCalculator {

    public static Optional<Double> getFcfGrowthInInterval(List<FinancialsTtm> financials, double years, double offset) {
        return getFcfGrowthInInterval(financials, years, offset, true);
    }

    public static Optional<Double> getFcfGrowthInInterval(List<FinancialsTtm> financials, double years, double offset, boolean ignoreNegativeTransition) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (years * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double now = getFcfPerShare(financials.get(newIndex));
        double then = getFcfPerShare(financials.get(oldIndex));

        if (ignoreNegativeTransition && isNegativeTransition(now, then)) {
            return Optional.empty();
        }

        FinancialsTtm financialsNow = financials.get(newIndex);
        FinancialsTtm financialThen = financials.get(oldIndex);
        double distance = calculateYearsDifference(financialsNow, financialThen);

        double resultPercent = calculatePercentChange(now, then, distance);

        return Optional.of(resultPercent);
    }

    public static double getFcfPerShare(FinancialsTtm financials) {
        long fcf = financials.cashFlowTtm.freeCashFlow;
        double fcfPerShare = (double) fcf / financials.incomeStatementTtm.weightedAverageShsOut;

        return fcfPerShare;
    }

    public static Optional<Double> getEpsGrowthInInterval(List<FinancialsTtm> financials, double year, double offsetYear) {
        return getEpsGrowthInInterval(financials, year, offsetYear, true);
    }

    public static Optional<Double> getEpsGrowthInInterval(List<FinancialsTtm> financials, double year, double offsetYear, boolean ignoreNegativeTransition) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (year * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (offsetYear * 12.0)));

        if (oldIndex >= financials.size() || oldIndex < 0 ||
                newIndex > financials.size() || newIndex == -1) {
            return Optional.empty();
        }

        double now = minEpsOf(financials, newIndex, newIndex);
        double then = maxEpsOf(financials, oldIndex, oldIndex);

        if (ignoreNegativeTransition && isNegativeTransition(now, then)) {
            return Optional.empty();
        }

        double distance = year - offsetYear;
        double resultPercent = calculatePercentChange(now, then, distance);

        if (!Double.isFinite(resultPercent)) {
            return Optional.empty();
        }

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getEpsGrowthInIntervalExRnd(List<FinancialsTtm> financials, double year, double offsetYear) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (year * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (offsetYear * 12.0)));

        if (oldIndex >= financials.size() || oldIndex < 0 ||
                newIndex > financials.size() || newIndex == -1) {
            return Optional.empty();
        }

        Double now = RatioCalculator.calculateEpsExRnd(financials.get(newIndex));
        Double then = RatioCalculator.calculateEpsExRnd(financials.get(oldIndex));

        if (now == null || then == null || isNegativeTransition(now, then)) {
            return Optional.empty();
        }

        double distance = year - offsetYear;
        double resultPercent = calculatePercentChange(now, then, distance);

        if (!Double.isFinite(resultPercent)) {
            return Optional.empty();
        }

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getEpsGrowthInIntervalExMns(List<FinancialsTtm> financials, double year, double offsetYear) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (year * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (offsetYear * 12.0)));

        if (oldIndex >= financials.size() || oldIndex < 0 ||
                newIndex > financials.size() || newIndex == -1) {
            return Optional.empty();
        }

        Double now = RatioCalculator.calculateEpsExMns(financials.get(newIndex));
        Double then = RatioCalculator.calculateEpsExMns(financials.get(oldIndex));

        if (now == null || then == null || isNegativeTransition(now, then)) {
            return Optional.empty();
        }

        double distance = year - offsetYear;
        double resultPercent = calculatePercentChange(now, then, distance);

        if (!Double.isFinite(resultPercent)) {
            return Optional.empty();
        }

        return Optional.of(resultPercent);
    }

    private static double maxEpsOf(List<FinancialsTtm> financials, int start, int end) {
        double max = (double) financials.get(start).incomeStatementTtm.netIncome / financials.get(start).incomeStatementTtm.weightedAverageShsOut;
        for (int i = start + 1; i < end; ++i) {
            double newEps = (double) financials.get(i).incomeStatementTtm.netIncome / financials.get(i).incomeStatementTtm.weightedAverageShsOut;
            if (newEps > max) {
                max = newEps;
            }
        }
        return max;
    }

    private static double minEpsOf(List<FinancialsTtm> financials, int start, int end) {
        double min = (double) financials.get(start).incomeStatementTtm.netIncome / financials.get(start).incomeStatementTtm.weightedAverageShsOut;
        for (int i = start + 1; i < end && i < financials.size(); ++i) {
            double newEps = (double) financials.get(i).incomeStatementTtm.netIncome / financials.get(i).incomeStatementTtm.weightedAverageShsOut;
            if (newEps < min) {
                min = newEps;
            }
        }
        return min;
    }

    public static Optional<Double> getMedianRevenueGrowth(List<FinancialsTtm> financials, int maxYears, double offset) {
        List<Double> values = new ArrayList<>();
        for (int i = maxYears; i >= 3; --i) {
            Optional<Double> growthInInterval = getRevenueGrowthInInterval(financials, i + offset, offset);
            if (growthInInterval.isPresent()) {
                values.add(growthInInterval.get());
            }
        }
        Collections.sort(values);
        return values.isEmpty() ? getShortTermGrowth(financials, offset) : Optional.of(values.get(values.size() / 2));
    }

    public static Optional<Double> getMedianEpsGrowth(List<FinancialsTtm> financials, int maxYears, double offset) {
        List<Double> values = new ArrayList<>();
        for (int i = maxYears; i >= 3; --i) {
            Optional<Double> growthInInterval = getEpsGrowthInInterval(financials, i + offset, offset);
            if (growthInInterval.isPresent()) {
                values.add(growthInInterval.get());
            }
        }
        Collections.sort(values);
        return values.isEmpty() ? getShortTermGrowth(financials, offset) : Optional.of(values.get(values.size() / 2));
    }

    public static Optional<Double> getMedianFcfGrowth(List<FinancialsTtm> financials, int maxYears, double offset) {
        List<Double> values = new ArrayList<>();
        for (int i = maxYears; i >= 3; --i) {
            Optional<Double> growthInInterval = getFcfGrowthInInterval(financials, i + offset, offset);
            if (growthInInterval.isPresent()) {
                values.add(growthInInterval.get());
            }
        }
        Collections.sort(values);
        return values.isEmpty() ? getShortTermGrowth(financials, offset) : Optional.of(values.get(values.size() / 2));
    }

    private static Optional<Double> getShortTermGrowth(List<FinancialsTtm> financials, double offset) {
        for (int i = 2; i >= 1; --i) {
            Optional<Double> growthInInterval = getRevenueGrowthInInterval(financials, i + offset, offset);
            if (growthInInterval.isPresent()) {
                return growthInInterval.map(a -> a * 0.6);
            }
        }
        return Optional.empty();
    }

    public static Optional<Double> getRevenueGrowthInInterval(List<FinancialsTtm> financials, double years, double offset) {
        return getRevenueGrowthInInterval(financials, years, offset, true);
    }

    public static Optional<Double> getRevenueGrowthInInterval(List<FinancialsTtm> financials, double years, double offset, boolean ignoreNegativeTransition) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (years * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1 || financials.get(oldIndex).incomeStatementTtm.revenue <= 0 ||
                newIndex >= financials.size() || newIndex == -1) {
            return Optional.empty();
        }

        long now = financials.get(newIndex).incomeStatementTtm.revenue;
        long then = financials.get(oldIndex).incomeStatementTtm.revenue;

        if (ignoreNegativeTransition && isNegativeTransition(now, then)) {
            return Optional.empty();
        }

        FinancialsTtm financialsNow = financials.get(newIndex);
        FinancialsTtm financialThen = financials.get(oldIndex);
        double distance = calculateYearsDifference(financialsNow, financialThen);

        double resultPercent = calculatePercentChange(now, then, distance);

        if (!Double.isFinite(resultPercent)) {
            return Optional.empty();
        }

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getPriceGrowthInInterval(List<FinancialsTtm> financials, double years, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (years * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        FinancialsTtm financialsNow = financials.get(newIndex);
        FinancialsTtm financialThen = financials.get(oldIndex);
        double now = financialsNow.price;
        double then = financialThen.price;

        double distance = calculateYearsDifference(financialsNow, financialThen);
        double resultPercent = calculatePercentChange(now, then, distance);

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getPriceGrowthWithReinvestedDividendsGrowth(CompanyFinancials company, double years, double offset) {
        List<SimpleDateDataElement> result = ReturnWithDividendCalculator.getPriceWithDividendsReinvested(company);

        int oldIndex = findIndexWithOrBeforeDate(result, CommonConfig.NOW.minusMonths((long) (years * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(result, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= result.size() || oldIndex == -1) {
            return Optional.empty();
        }

        var oldValue = result.get(oldIndex);
        var newValue = result.get(newIndex);

        double distance = calculateYearsDifference(newValue, oldValue);
        double resultPercent = calculatePercentChange(newValue.value, oldValue.value, distance);

        if (!Double.isFinite(resultPercent)) {
            return Optional.empty();
        }

        return Optional.of(resultPercent);
    }

    private static double calculateYearsDifference(DateAware financialsNow, DateAware financialThen) {
        return Math.abs(ChronoUnit.DAYS.between(financialsNow.getDate(), financialThen.getDate()) / 365.0);
    }

    public static Optional<Double> getShareCountGrowthInInterval(List<FinancialsTtm> financials, double years, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (years * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double now = financials.get(newIndex).incomeStatementTtm.weightedAverageShsOut;
        double then = financials.get(oldIndex).incomeStatementTtm.weightedAverageShsOut;

        FinancialsTtm financialsNow = financials.get(newIndex);
        FinancialsTtm financialThen = financials.get(oldIndex);
        double distance = calculateYearsDifference(financialsNow, financialThen);
        double resultPercent = calculatePercentChange(now, then, distance);

        if (!Double.isFinite(resultPercent)) {
            return Optional.of(0.0);
        }

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

    public static Optional<Double> getNetIncomeGrowthInInterval(List<FinancialsTtm> financials, double year, double offsetYear) {
        return getNetIncomeGrowthInInterval(financials, year, offsetYear, true);
    }

    public static Optional<Double> getNetIncomeGrowthInInterval(List<FinancialsTtm> financials, double year, double offsetYear, boolean ignoreNegativeTransition) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (year * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (offsetYear * 12.0)));

        if (oldIndex >= financials.size() || oldIndex < 0 ||
                newIndex > financials.size() || newIndex == -1) {
            return Optional.empty();
        }

        double now = financials.get(newIndex).incomeStatementTtm.netIncome;
        double then = financials.get(oldIndex).incomeStatementTtm.netIncome;

        if (ignoreNegativeTransition && isNegativeTransition(now, then)) {
            return Optional.empty();
        }

        double distance = year - offsetYear;
        double resultPercent = calculatePercentChange(now, then, distance);

        if (!Double.isFinite(resultPercent)) {
            return Optional.empty();
        }

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getEquityPerShareGrowthInInterval(List<FinancialsTtm> financials, double year, double offsetYear) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (year * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((int) (offsetYear * 12.0)));

        if (oldIndex >= financials.size() || oldIndex < 0 ||
                newIndex > financials.size() || newIndex == -1) {
            return Optional.empty();
        }

        var newFinancials = financials.get(newIndex);
        var oldFinancials = financials.get(oldIndex);

        double equityPerShareNow = (double) newFinancials.balanceSheet.totalStockholdersEquity / newFinancials.incomeStatementTtm.weightedAverageShsOut;
        double equityPerShareThen = (double) oldFinancials.balanceSheet.totalStockholdersEquity / oldFinancials.incomeStatementTtm.weightedAverageShsOut;

        double distance = year - offsetYear;
        double resultPercent = calculatePercentChange(equityPerShareNow, equityPerShareThen, distance);

        if (!Double.isFinite(resultPercent)) {
            return Optional.empty();
        }

        return Optional.of(resultPercent);
    }

    public static Optional<Double> calculateAnnualGrowth(double oldValue, LocalDate oldDate, double newValue, LocalDate newDate) {
        double daysDiff = Math.abs(ChronoUnit.DAYS.between(newDate, oldDate) / 365.0);
        double resultPercent = calculatePercentChange(newValue, oldValue, daysDiff);

        if (!Double.isFinite(resultPercent)) {
            return Optional.empty();
        }

        return Optional.of(resultPercent);
    }

    private static double calculatePercentChange(double now, double then, double distance) {
        if (now < 0.0 && then < 0.0) {
            return -(Math.pow(now / then, 1.0 / distance) - 1.0) * 100.0;
        } else if (now > 0.0 && then > 0.0) {
            return (Math.pow(now / then, 1.0 / distance) - 1.0) * 100.0;
        } else if (now >= 0.0 && then < 0.0) {
            return (Math.pow(-then / now, 1.0 / distance) + 1.0) * 100.0;
        } else {
            return -(Math.pow(-now / then, 1.0 / distance) + 1.0) * 100.0;
        }
    }

    public static boolean isNegativeTransition(double now, double then) {
        return now < 0 && then > 0 || now > 0 && then < 0;
    }

}
