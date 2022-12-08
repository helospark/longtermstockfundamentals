package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.util.List;
import java.util.Optional;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.FinancialsTtm;

public class MarginCalculator {

    public static Optional<Double> getNetMarginGrowthRate(List<FinancialsTtm> financials, int years, int offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusYears(years));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusYears(offset));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double now = getNetMargin(financials.get(newIndex)) + 1.0;
        double then = getNetMargin(financials.get(oldIndex)) + 1.0;
        double change = ((now) / (then));

        int distance = years - offset;
        double resultPercent = (Math.pow(change, 1.0 / distance) - 1.0) * 100.0;

        return Optional.of(resultPercent * 100.0);
    }

    private static double getNetMargin(FinancialsTtm financials) {
        return (double) financials.incomeStatementTtm.netIncome / financials.incomeStatementTtm.revenue;
    }

}
