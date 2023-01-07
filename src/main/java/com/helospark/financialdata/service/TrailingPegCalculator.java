package com.helospark.financialdata.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import com.helospark.financialdata.domain.CompanyFinancials;

public class TrailingPegCalculator {

    public static Optional<Double> calculateTrailingPeg(CompanyFinancials company, double offsetYear) {
        int i = Helpers.findIndexWithOrBeforeDate(company.financials, LocalDate.now().minusMonths((int) (offsetYear * 12.0)));
        if (i == -1 || i >= company.financials.size()) {
            return Optional.empty();
        }
        return calculateTrailingPegWithLatestPrice(company, offsetYear, company.financials.get(i).price);
    }

    public static Optional<Double> calculateTrailingPegWithRevGrowth(CompanyFinancials company, double offsetYear) {
        int i = Helpers.findIndexWithOrBeforeDate(company.financials, LocalDate.now().minusMonths((int) (offsetYear * 12.0)));
        if (i == -1 || i >= company.financials.size()) {
            return Optional.empty();
        }
        return calculateTrailingPegWithLatestPriceAndRevGrowth(company, offsetYear, company.financials.get(i).price);
    }

    public static Optional<Double> calculateTrailingPegWithLatestPrice(CompanyFinancials company, double offsetYear, double latestPrice) {
        return calculateTrailingPegWithPriceAndGrowthRateCalc(company, offsetYear, latestPrice, (company2, offsetYear2) -> getPastEpsGrowthRate(company2, offsetYear2));
    }

    public static Optional<Double> calculateTrailingPegWithLatestPriceAndRevGrowth(CompanyFinancials company, double offsetYear, double latestPrice) {
        return calculateTrailingPegWithPriceAndGrowthRateCalc(company, offsetYear, latestPrice, (company2, offsetYear2) -> getPastRevenueGrowthRate(company2, offsetYear2));
    }

    public static Optional<Double> calculateTrailingPegWithPriceAndGrowthRateCalc(CompanyFinancials company, double offsetYear, double latestPrice,
            BiFunction<CompanyFinancials, Double, Double> growthCalculator) {
        int i = Helpers.findIndexWithOrBeforeDate(company.financials, LocalDate.now().minusMonths((int) (offsetYear * 12.0)));
        if (i == -1 || i >= company.financials.size()) {
            return Optional.empty();
        }
        var financialsTtm = company.financials.get(i);
        double growthRate = growthCalculator.apply(company, offsetYear);
        double eps = financialsTtm.incomeStatementTtm.eps;
        if (eps <= 0.0) {
            return Optional.empty();
        }
        double value = ((latestPrice / eps) / growthRate);

        if (!Double.isFinite(value)) {
            return Optional.empty();
        }
        if (value < 0.0) {
            return Optional.empty();
        }
        if (value > 50) {
            return Optional.of(50.0);
        }

        return Optional.of(value);
    }

    public static Optional<Double> calculateTrailingCAPegWithLatestPrice(CompanyFinancials company, double offsetYear, double latestPrice) {
        int i = Helpers.findIndexWithOrBeforeDate(company.financials, LocalDate.now().minusMonths((int) (offsetYear * 12.0)));
        if (i == -1 || i >= company.financials.size()) {
            return Optional.empty();
        }
        double growthRate = getPastRevenueGrowthRate(company, offsetYear);
        if (growthRate < 0) {
            return Optional.empty();
        }
        double value = CapeCalculator.calculateCapeRatioQ(company.financials, 4, i) / growthRate;

        if (!Double.isFinite(value)) {
            return Optional.empty();
        }
        if (value < 0.0) {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    public static double getPastEpsGrowthRate(CompanyFinancials company, double offsetYears) {
        double offsetYear = offsetYears;
        double growthRate = 0.0;
        List<Double> growthRates = new ArrayList<>();
        for (int i = 7; i > 0; --i) {
            Optional<Double> growthInYear = GrowthCalculator.getEpsGrowthInInterval(company.financials, i + offsetYear, offsetYear);
            if (growthInYear.isPresent() && !growthInYear.get().isNaN() && !growthInYear.get().isInfinite()) {
                growthRates.add(growthInYear.get());
            }
        }
        Collections.sort(growthRates);
        if (!growthRates.isEmpty()) {
            growthRate = growthRates.get(growthRates.size() / 2);
            //            growthRate = growthRates.stream().mapToDouble(a -> a).average().getAsDouble();
        }
        if (growthRate == 0.0) {
            growthRate = 0.001;
        }

        return growthRate;
    }

    public static Optional<Double> calculateTrailingCAPeg(CompanyFinancials company, double offsetYear) {
        int i = Helpers.findIndexWithOrBeforeDate(company.financials, LocalDate.now().minusMonths((int) (offsetYear * 12.0)));
        if (i == -1 || i >= company.financials.size()) {
            return Optional.empty();
        }
        return calculateTrailingCAPegWithLatestPrice(company, offsetYear, company.financials.get(i).price);
    }

    public static double getPastRevenueGrowthRate(CompanyFinancials company, double offsetYears) {
        double offsetYear = offsetYears;
        double growthRate = 0.0;
        List<Double> growthRates = new ArrayList<>();
        for (int i = 7; i > 0; --i) {
            Optional<Double> growthInYear = GrowthCalculator.getRevenueGrowthInInterval(company.financials, i + offsetYear, offsetYear);
            if (growthInYear.isPresent() && !growthInYear.get().isNaN() && !growthInYear.get().isInfinite()) {
                growthRates.add(growthInYear.get());
            }
        }
        Collections.sort(growthRates);
        if (!growthRates.isEmpty()) {
            growthRate = growthRates.get(growthRates.size() / 2);
            //            growthRate = growthRates.stream().mapToDouble(a -> a).average().getAsDouble();
        }
        if (growthRate == 0.0) {
            growthRate = 0.001;
        }

        return growthRate;
    }
}
