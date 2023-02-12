package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.CompanyFinancials;

public class EverythingMoneyCalculator {

    public static Optional<Double> calculateFiveYearPe(CompanyFinancials company, double offsetYear) {
        var financials = company.financials;
        int newIndex = findIndexWithOrBeforeDate(company.financials, CommonConfig.NOW.minusMonths((long) (offsetYear * 12.0)));
        if (newIndex == -1) {
            return Optional.empty();
        }
        int oldIndex = findIndexWithOrBeforeDate(company.financials, financials.get(newIndex).getDate().minusYears(5L));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }
        int yearsBetween = (int) Math.abs(ChronoUnit.YEARS.between(financials.get(oldIndex).getDate(), financials.get(newIndex).getDate()));

        double value = 0.0;
        for (int i = newIndex; i < oldIndex; ++i) {
            value += financials.get(i).incomeStatement.netIncome;
        }
        double latestPrice = financials.get(newIndex).price;
        if (newIndex == 0) {
            latestPrice = company.latestPrice;
        }
        double marketCap = financials.get(newIndex).incomeStatement.weightedAverageShsOut * latestPrice;

        return Optional.of(yearsBetween * marketCap / value);
    }

    public static Optional<Double> calculateFiveYearFcf(CompanyFinancials company, double offsetYear) {
        var financials = company.financials;
        int newIndex = findIndexWithOrBeforeDate(company.financials, CommonConfig.NOW.minusMonths((long) (offsetYear * 12.0)));
        if (newIndex == -1) {
            return Optional.empty();
        }
        int oldIndex = findIndexWithOrBeforeDate(company.financials, financials.get(newIndex).getDate().minusYears(5L));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }
        int yearsBetween = (int) Math.abs(ChronoUnit.YEARS.between(financials.get(oldIndex).getDate(), financials.get(newIndex).getDate()));

        double latestPrice = financials.get(newIndex).price;
        if (newIndex == 0) {
            latestPrice = company.latestPrice;
        }

        double value = 0.0;
        for (int i = newIndex; i < oldIndex; ++i) {
            value += financials.get(i).cashFlow.freeCashFlow;
        }
        double marketCap = financials.get(newIndex).incomeStatement.weightedAverageShsOut * latestPrice;

        return Optional.of(yearsBetween * marketCap / value);
    }

    public static Optional<Double> calculateFiveYearRoic(CompanyFinancials company, double offsetYear) {
        var financials = company.financials;
        int newIndex = findIndexWithOrBeforeDate(company.financials, CommonConfig.NOW.minusMonths((long) (offsetYear * 12.0)));
        if (newIndex == -1) {
            return Optional.empty();
        }
        int oldIndex = findIndexWithOrBeforeDate(company.financials, financials.get(newIndex).getDate().minusYears(5L));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double value = 0.0;
        for (int i = 0; i < 5; ++i) {
            int index = findIndexWithOrBeforeDate(company.financials, CommonConfig.NOW.minusMonths((long) ((offsetYear + i) * 12.0)));
            if (index == -1) {
                return Optional.empty();
            }
            value += RoicCalculator.calculateFcfRoic(company.financials.get(index));
        }
        return Optional.of(((value) / 5.0) * 100.0);
    }

    public static Optional<Double> calculateFiveYearRevenueGrowth(CompanyFinancials company, double offsetYear) {
        return GrowthCalculator.getRevenueGrowthInInterval(company.financials, offsetYear + 5.0, offsetYear);
    }

    public static Optional<Double> calculate5YearNetIncomeGrowth(CompanyFinancials company, double offsetYear) {
        return GrowthCalculator.getNetIncomeGrowthInInterval(company.financials, offsetYear + 5.0, offsetYear);
    }

    public static Optional<Double> calculate5YearShareGrowth(CompanyFinancials company, double offsetYear) {
        return GrowthCalculator.getShareCountGrowthInInterval(company.financials, offsetYear + 5.0, offsetYear);
    }

    public static Optional<Double> calculate5YearFcfGrowth(CompanyFinancials company, double offsetYear) {
        return GrowthCalculator.getFcfGrowthInInterval(company.financials, offsetYear + 5.0, offsetYear);
    }

    public static Optional<Double> calculateLtlPer5YrFcf(CompanyFinancials company, double offsetYear) {
        var financials = company.financials;
        int newIndex = findIndexWithOrBeforeDate(company.financials, CommonConfig.NOW.minusMonths((long) (offsetYear * 12.0)));
        if (newIndex == -1) {
            return Optional.empty();
        }
        int oldIndex = findIndexWithOrBeforeDate(company.financials, financials.get(newIndex).getDate().minusYears(5L));

        if (oldIndex >= financials.size() || oldIndex == -1) {
            return Optional.empty();
        }

        double avgFcf = 0.0;
        for (int i = 0; i < 5; ++i) {
            int index = findIndexWithOrBeforeDate(company.financials, CommonConfig.NOW.minusMonths((long) ((offsetYear + i) * 12.0)));
            if (index == -1) {
                return Optional.empty();
            }
            avgFcf += company.financials.get(index).cashFlowTtm.freeCashFlow;
        }
        avgFcf /= 5.0;
        return Optional.of((financials.get(newIndex).balanceSheet.totalLiabilities - financials.get(newIndex).balanceSheet.totalCurrentLiabilities) / avgFcf);
    }
}
