package com.helospark.financialdata.service;

import java.util.Optional;

import com.helospark.financialdata.domain.FinancialsTtm;

public class GrahamNumberCalculator {

    public static Optional<Double> calculateGrahamNumber(FinancialsTtm financialsTtm) {
        double valueBeforeSqrt = 22.5 * financialsTtm.incomeStatementTtm.eps * RatioCalculator.calculateBookValuePerShare(financialsTtm);
        if (valueBeforeSqrt < 0.0) {
            return Optional.empty();
        }
        return Optional.of(Math.sqrt(valueBeforeSqrt));
    }

}
