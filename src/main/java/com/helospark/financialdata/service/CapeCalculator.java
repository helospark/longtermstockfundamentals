package com.helospark.financialdata.service;

import java.time.LocalDate;
import java.util.List;

import com.helospark.financialdata.domain.FinancialsTtm;

public class CapeCalculator {

    public static Double calculateCapeRatioQ(List<FinancialsTtm> financials, int years, int offsetIndex) {
        if (financials.size() <= offsetIndex) {
            return null;
        }
        LocalDate startDate = financials.get(offsetIndex).getDate();
        FinancialsTtm currentYear = financials.get(offsetIndex);
        LocalDate toDate = currentYear.getDate().minusYears(years);
        double sumEps = 0.0;
        int epsCount = 0;
        LocalDate currentDate = currentYear.getDate();
        while (currentDate.compareTo(toDate) >= 0) {
            int previousYearIndex = Helpers.findIndexWithOrBeforeDate(financials, currentDate);
            if (previousYearIndex == -1) {
                break;
            }
            var previousYear = financials.get(previousYearIndex);

            double adjustedEps = CpiAdjustor.adjustForInflationToOldDate(previousYear.incomeStatementTtm.eps, previousYear.getDate(), startDate);

            sumEps += adjustedEps;
            ++epsCount;
            currentDate = currentDate.minusYears(1);
        }

        double avgInflationAdjustedEps = sumEps / epsCount;

        double result = currentYear.price / avgInflationAdjustedEps;

        if (!Double.isFinite(result)) {
            return null;
        }

        return result;
    }
}
