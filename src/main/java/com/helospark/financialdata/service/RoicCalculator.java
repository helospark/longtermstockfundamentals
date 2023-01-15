package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.FinancialsTtm;

public class RoicCalculator {

    public static Optional<Double> getAverageRoic(List<FinancialsTtm> financials, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex == -1) {
            return Optional.empty();
        }

        List<Double> roics = new ArrayList<>();
        for (int i = oldIndex; i < oldIndex + (5 * 4) && i < financials.size(); ++i) {
            var financialsTtm = financials.get(i);
            double roic = calculateRoic(financialsTtm);
            roics.add(roic);
        }
        Collections.sort(roics);
        return Optional.of(roics.get(roics.size() / 2));
    }

    public static double calculateRoic(FinancialsTtm financialsTtm) {
        double ebit = calculateEbit(financialsTtm);
        return ebit / (financialsTtm.balanceSheet.totalAssets - financialsTtm.balanceSheet.totalCurrentLiabilities);
    }

    public static long calculateEbit(FinancialsTtm financialsTtm) {
        return financialsTtm.incomeStatementTtm.ebitda + financialsTtm.incomeStatementTtm.depreciationAndAmortization;
    }

    public static double calculateROA(FinancialsTtm financialsTtm) {
        return ((double) financialsTtm.incomeStatementTtm.netIncome / financialsTtm.balanceSheet.totalAssets);
    }

    public static Double calculateROTA(FinancialsTtm financialsTtm) {
        if (financialsTtm.balanceSheet.goodwillAndIntangibleAssets == 0) {
            return null;
        }
        return (financialsTtm.incomeStatementTtm.netIncome / calculateTangibleAssets(financialsTtm));
    }

    public static double calculateTangibleAssets(FinancialsTtm financialsTtm) {
        return financialsTtm.balanceSheet.totalAssets - financialsTtm.balanceSheet.goodwillAndIntangibleAssets - financialsTtm.balanceSheet.totalLiabilities;
    }

}
