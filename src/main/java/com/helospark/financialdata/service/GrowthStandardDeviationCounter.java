package com.helospark.financialdata.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.helospark.financialdata.domain.FinancialsTtm;

public class GrowthStandardDeviationCounter {

    public static Optional<Double> calculateEpsGrowthDeviation(List<FinancialsTtm> financials, double offset) {
        return calculateGrowthDeviationInternal(financials, offset, stepYear -> GrowthCalculator.getGrowthInInterval(financials, stepYear + offset + 4, stepYear + offset));
    }

    public static Optional<Double> calculateRevenueGrowthDeviation(List<FinancialsTtm> financials, double offset) {
        return calculateGrowthDeviationInternal(financials, offset, stepYear -> GrowthCalculator.getRevenueGrowthInInterval(financials, stepYear + offset + 4, stepYear + offset));
    }

    public static Optional<Double> calculateFcfGrowthDeviation(List<FinancialsTtm> financials, double offset) {
        return calculateGrowthDeviationInternal(financials, offset, stepYear -> GrowthCalculator.getFcfGrowthInInterval(financials, stepYear + offset + 4, stepYear + offset));
    }

    private static Optional<Double> calculateGrowthDeviationInternal(List<FinancialsTtm> financials, double offset, Function<Double, Optional<Double>> growthFunc) {
        List<Double> result = new ArrayList<>();
        for (int i = 0; i < 23 * 4; ++i) {
            double stepYear = (i / 4.0);
            Optional<Double> growth = growthFunc.apply(stepYear);
            if (growth.isPresent() && Double.isFinite(growth.get())) {
                result.add(growth.get());
            }
        }
        if (result.size() > 3) {
            return Optional.of(getArraySD(result));
        } else {
            return Optional.empty();
        }
    }

    private static double getArraySD(List<Double> numArray) {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.size();

        for (double num : numArray) {
            sum += num;
        }

        double mean = sum / length;

        for (double num : numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation / length);
    }
}
