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
        Double nowCurrentRatio = RatioCalculator.calculateCurrentRatio(nowFinancials);
        Double thenCurrentRatio = RatioCalculator.calculateCurrentRatio(oneYearAgoFinancials);
        if (nowCurrentRatio != null && nowCurrentRatio != null && nowCurrentRatio > thenCurrentRatio) {
            ++result;
        }
        if (nowFinancials.incomeStatementTtm.weightedAverageShsOutDil <= oneYearAgoFinancials.incomeStatementTtm.weightedAverageShsOutDil) {
            ++result;
        }
        double nowGrossMargin = RatioCalculator.calculateGrossProfitMargin(nowFinancials);
        double thenGrossMargin = RatioCalculator.calculateGrossProfitMargin(oneYearAgoFinancials);
        if (nowGrossMargin > thenGrossMargin) {
            ++result;
        }

        double nowAssetTurnover = RatioCalculator.calculateAssetTurnover(nowFinancials);
        double thenAssetTurnover = RatioCalculator.calculateAssetTurnover(oneYearAgoFinancials);
        if (nowAssetTurnover > thenAssetTurnover) {
            ++result;
        }

        return Optional.of(result);
    }

}
