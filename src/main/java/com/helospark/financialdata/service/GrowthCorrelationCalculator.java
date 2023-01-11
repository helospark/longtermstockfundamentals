package com.helospark.financialdata.service;

import static com.helospark.financialdata.CommonConfig.NOW;
import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.util.List;
import java.util.Optional;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import com.helospark.financialdata.domain.FinancialsTtm;

public class GrowthCorrelationCalculator {

    public static Optional<Double> calculateEpsFcfCorrelation(List<FinancialsTtm> financials, double year, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, NOW.minusMonths((long) (year * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1 || newIndex == -1 || newIndex >= financials.size()) {
            return Optional.empty();
        }

        double[] epses = new double[oldIndex - newIndex + 1];
        double[] fcfs = new double[oldIndex - newIndex + 1];
        int j = 0;
        for (int i = newIndex; i < oldIndex; ++i) {
            epses[j] = financials.get(i).incomeStatementTtm.eps;
            fcfs[j] = (double) financials.get(i).cashFlowTtm.freeCashFlow / financials.get(i).incomeStatementTtm.weightedAverageShsOut;
            ++j;
        }

        if (epses.length < 5) {
            return Optional.empty();
        }

        double corr = new PearsonsCorrelation().correlation(epses, fcfs);

        return Optional.of(corr);
    }

}
