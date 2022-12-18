package com.helospark.financialdata.service;

import static com.helospark.financialdata.CommonConfig.NOW;
import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.util.List;

import com.helospark.financialdata.domain.FinancialsTtm;

public class GrowthAnalyzer {

    public static boolean isLargeGrowthEveryYear(List<FinancialsTtm> financials, int year, double growth) {
        int oldIndex = findIndexWithOrBeforeDate(financials, NOW.minusYears(year));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return false;
        }

        for (int i = oldIndex - 4; i >= 0; i -= 4) {
            double epsOld = financials.get(i + 4).incomeStatementTtm.eps;
            double epsNew = financials.get(i).incomeStatementTtm.eps;

            if (epsNew < epsOld * (1.0 + growth / 100.0)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isStableGrowth(List<FinancialsTtm> financials, double year, double offset) {
        int querterSmooth = 16;
        int oldIndex = findIndexWithOrBeforeDate(financials, NOW.minusMonths((long) (year * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1 || newIndex == -1 || newIndex >= financials.size() || newIndex > oldIndex) {
            return false;
        }

        for (int i = oldIndex - querterSmooth; i >= newIndex; --i) {
            double epsOld = financials.get(i + querterSmooth).incomeStatementTtm.eps;
            double epsNew = financials.get(i).incomeStatementTtm.eps;

            if (epsNew < epsOld * 0.8) {
                return false;
            }
        }

        return true;
    }

    public static boolean isStableGrowth(List<FinancialsTtm> financials, int year, int offset, double avgGrowthRate) {
        int querterSmooth = 16;
        int oldIndex = findIndexWithOrBeforeDate(financials, NOW.minusYears(year));
        int newIndex = findIndexWithOrBeforeDate(financials, NOW.minusYears(offset));

        if (oldIndex >= financials.size() || oldIndex == -1 || newIndex == -1 || newIndex >= financials.size() || newIndex > oldIndex) {
            return false;
        }

        for (int i = oldIndex - querterSmooth; i >= newIndex; --i) {
            double epsOld = financials.get(i + querterSmooth).incomeStatementTtm.eps;
            double epsNew = financials.get(i).incomeStatementTtm.eps;

            if (epsNew < epsOld * 0.8 || epsNew > epsOld * Math.pow(1.0 + avgGrowthRate / 100.0, querterSmooth / 4) * 2.0) {
                return false;
            }
        }

        return true;
    }

    public static boolean isProfitableEveryYearSince(List<FinancialsTtm> financials, double year, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, NOW.minusMonths((long) (year * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1 || newIndex == -1 || newIndex >= financials.size() || newIndex > oldIndex) {
            return false;
        }

        for (int i = oldIndex; i >= newIndex; --i) {
            if (financials.get(i).incomeStatementTtm.eps <= 0.0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isCashFlowProfitableEveryYearSince(List<FinancialsTtm> financials, double year, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, NOW.minusMonths((long) (year * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex >= financials.size() || oldIndex == -1 || newIndex == -1 || newIndex >= financials.size() || newIndex > oldIndex) {
            return false;
        }

        for (int i = oldIndex; i >= newIndex; --i) {
            if (financials.get(i).cashFlowTtm.freeCashFlow <= 0.0) {
                return false;
            }
        }
        return true;
    }

}
