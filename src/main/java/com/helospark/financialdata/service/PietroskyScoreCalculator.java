package com.helospark.financialdata.service;

import static com.helospark.financialdata.CommonConfig.NOW;
import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.util.Optional;

import com.helospark.financialdata.domain.CompanyFinancials;

public class PietroskyScoreCalculator {

    public static Optional<Integer> calculatePietroskyScore(CompanyFinancials company, double offset) {
        int nowIndex = findIndexWithOrBeforeDate(company.financials, NOW.minusMonths((long) (offset * 12.0)));
        int oneYearAgo = findIndexWithOrBeforeDate(company.financials, NOW.minusMonths((long) ((offset + 1.0) * 12.0)));

        if (nowIndex == -1 || oneYearAgo == -1) {
            return Optional.empty();
        }

        var nowFinancials = company.financials.get(nowIndex);
        var oneYearAgoFinancials = company.financials.get(oneYearAgo);

        int result = 0;

        if (nowFinancials.incomeStatementTtm.netIncome > 0) {
            ++result;
        }
        if (RoicCalculator.calculateROA(nowFinancials) > 0) {
            ++result;
        }
        if (nowFinancials.cashFlowTtm.operatingCashFlow > 0) {
            ++result;
        }
        if (nowFinancials.cashFlowTtm.operatingCashFlow > nowFinancials.incomeStatementTtm.netIncome) {
            ++result;
        }
        if (nowFinancials.balanceSheet.longTermDebt < oneYearAgoFinancials.balanceSheet.longTermDebt) {
            ++result;
        }
        if (nowFinancials.remoteRatio.currentRatio != null && oneYearAgoFinancials.remoteRatio.currentRatio != null
                && nowFinancials.remoteRatio.currentRatio > oneYearAgoFinancials.remoteRatio.currentRatio) {
            ++result;
        }
        if (nowFinancials.incomeStatementTtm.weightedAverageShsOutDil <= oneYearAgoFinancials.incomeStatementTtm.weightedAverageShsOutDil) {
            ++result;
        }
        if (nowFinancials.remoteRatio.grossProfitMargin != null && oneYearAgoFinancials.remoteRatio.grossProfitMargin != null
                && nowFinancials.remoteRatio.grossProfitMargin > oneYearAgoFinancials.remoteRatio.grossProfitMargin) {
            ++result;
        }
        if (nowFinancials.remoteRatio.assetTurnover != null && oneYearAgoFinancials.remoteRatio.assetTurnover != null
                && nowFinancials.remoteRatio.assetTurnover > oneYearAgoFinancials.remoteRatio.assetTurnover) {
            ++result;
        }

        return Optional.of(result);
    }

}
