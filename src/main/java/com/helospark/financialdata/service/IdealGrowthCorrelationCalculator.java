package com.helospark.financialdata.service;

import static com.helospark.financialdata.CommonConfig.NOW;
import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import com.helospark.financialdata.domain.FinancialsTtm;

public class IdealGrowthCorrelationCalculator {

    public static Optional<Double> calculateRevenueCorrelation(List<FinancialsTtm> financials, double year, double offset) {
        return calculateCorrelationWithFunction(financials, year, offset, i -> (double) financials.get(i).incomeStatementTtm.revenue);
    }

    public static Optional<Double> calculateEpsCorrelation(List<FinancialsTtm> financials, double year, double offset) {
        return calculateCorrelationWithFunction(financials, year, offset, i -> (double) financials.get(i).incomeStatementTtm.eps);
    }

    public static Optional<Double> calculateFcfCorrelation(List<FinancialsTtm> financials, double year, double offset) {
        return calculateCorrelationWithFunction(financials, year, offset, i -> (double) financials.get(i).cashFlowTtm.freeCashFlow);
    }

    private static Optional<Double> calculateCorrelationWithFunction(List<FinancialsTtm> financials, double year, double offset, Function<Integer, Double> dataSource) {
        int oldIndex = findIndexWithOrBeforeDate(financials, NOW.minusMonths((long) (year * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1 || newIndex == -1 || newIndex >= financials.size()) {
            return Optional.empty();
        }

        double thenValue = dataSource.apply(oldIndex);
        double nowValue = dataSource.apply(newIndex);
        if (thenValue < 0 || nowValue < 0 || (oldIndex - newIndex) < 3) {
            return Optional.empty();
        }

        double growthPerYear = GrowthCalculator.calculateGrowth(nowValue, thenValue, (year - offset)) / 100.0 + 1.0;
        double growthPerQ = Math.pow(growthPerYear, 1 / 4.0);

        if (!Double.isFinite(growthPerYear)) {
            return Optional.empty();
        }

        double growthToUse = growthPerQ;
        if (Math.abs(ChronoUnit.MONTHS.between(financials.get(oldIndex).getDate(), financials.get(oldIndex - 1).getDate())) > 5) {
            growthToUse = growthPerYear;
        }

        double[] epses = new double[oldIndex - newIndex + 1];
        double[] idealGrowth = new double[oldIndex - newIndex + 1];
        double value = thenValue;
        int j = 0;
        for (int i = oldIndex; i >= newIndex; --i) {
            epses[j] = dataSource.apply(i);
            idealGrowth[j] = value;
            value *= growthToUse;
            ++j;
        }

        if (epses.length < 5) {
            return Optional.empty();
        }

        Arrays.toString(epses);
        Arrays.toString(idealGrowth);

        double corr = new PearsonsCorrelation().correlation(epses, idealGrowth);

        return Optional.of(corr);
    }

}
