package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;

import java.util.Optional;

import com.helospark.financialdata.domain.CompanyFinancials;

public class ProfitabilityCalculator {

    public static Optional<Integer> calculateNumberOfYearsProfitable(CompanyFinancials company, double offset) {
        for (int i = 30; i > 0; --i) {
            if (isProfitableEveryYearSince(company.financials, offset + i, offset)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public static boolean hasNegativeFreeCashFlow(CompanyFinancials company, int index, int years) {
        for (int i = index; i < company.financials.size() && i < years * 4; ++i) {
            if (company.financials.get(i).cashFlowTtm.freeCashFlow < 0) {
                return true;
            }
        }
        return false;
    }

}
