package com.helospark.financialdata.service;

import java.time.LocalDate;
import java.util.List;

import com.helospark.financialdata.domain.FinancialsTtm;

public class CapeCalculator {

    public static Double calculateCapeRatioQ(List<FinancialsTtm> financials, int offset, int years) {
        if (financials.size() <= offset) {
            return null;
        }
        LocalDate startDate = financials.get(offset).getDate();
        FinancialsTtm currentYear = financials.get(offset);
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

        return currentYear.price / avgInflationAdjustedEps;
    }
}
