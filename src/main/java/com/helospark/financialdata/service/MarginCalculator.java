package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.util.List;
import java.util.Optional;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.FinancialsTtm;

public class MarginCalculator {

    public static Optional<Double> getNetMarginGrowthRate(List<FinancialsTtm> financials, double years, double newYear) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (years * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (newYear * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double now = getAvgNetMargin(financials, newIndex);
        double then = getAvgNetMargin(financials, oldIndex);

        double distance = years - newYear;
        double resultPercent = GrowthCalculator.calculateGrowth(now, then, distance);

        return Optional.of(resultPercent);
    }

    public static Optional<Double> getGrossMargin(List<FinancialsTtm> financials, double years) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (years * 12.0)));

        if (oldIndex == -1) {
            return Optional.empty();
        }

        return Optional.of(financials.get(oldIndex).incomeStatementTtm.grossProfitRatio * 100.0);
    }

    private static double getAvgNetMargin(List<FinancialsTtm> financials, int oldIndex) {
        double sum = 0.0;
        int count = 0;
        for (int i = oldIndex; i < oldIndex + 4 && i < financials.size(); ++i) {
            double margin = (double) financials.get(i).incomeStatementTtm.netIncome / financials.get(i).incomeStatementTtm.revenue;
            sum += margin;
            ++count;
        }
        return sum / count;
    }

}
