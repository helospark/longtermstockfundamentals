package com.helospark.financialdata.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;

public class TrailingPegCalculator {

    public static Optional<Double> calculateTrailingPeg(CompanyFinancials company, int i) {
        if (i >= company.financials.size()) {
            return Optional.empty();
        }
        FinancialsTtm financialsTtm = company.financials.get(i);
        double growthRate = getPastGrowthRate(company, i);
        double eps = financialsTtm.incomeStatementTtm.eps;
        if (eps <= 0.0) {
            return Optional.empty();
        }
        double value = ((financialsTtm.price / eps) / growthRate);

        if (!Double.isFinite(value)) {
            return Optional.empty();
        }
        if (value < 0.0) {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    public static double getPastGrowthRate(CompanyFinancials company, int offset) {
        double offsetYear = offset / 4.0;
        double growthRate = 0.0;
        List<Double> growthRates = new ArrayList<>();
        for (int i = 7; i > 0; --i) {
            Optional<Double> growthInYear = GrowthCalculator.getGrowthInInterval(company.financials, i + offsetYear, offsetYear);
            if (growthInYear.isPresent() && !growthInYear.get().isNaN() && !growthInYear.get().isInfinite()) {
                growthRates.add(growthInYear.get());
            }
        }
        Collections.sort(growthRates);
        if (!growthRates.isEmpty()) {
            growthRate = growthRates.get(growthRates.size() / 2);
            //            growthRate = growthRates.stream().mapToDouble(a -> a).average().getAsDouble();
        }
        if (growthRate == 0.0) {
            growthRate = 0.001;
        }

        return growthRate;
    }
}
