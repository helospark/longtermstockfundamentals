package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isLargeGrowthEveryYear;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthCalculator.getEpsGrowthInInterval;

import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.RatioCalculator;

public class VeryHighGrowthScreener {

    public void analyze(Set<String> symbols) {
        for (var symbol : symbols) {
            CompanyFinancials company = readFinancials(symbol);
            var financials = company.financials;

            if (financials.isEmpty()) {
                continue;
            }

            Optional<Double> tenYearAvgGrowth = getEpsGrowthInInterval(financials, 6, 0);
            boolean continouslyProfitable = isProfitableEveryYearSince(financials, 6, 0);
            boolean isLargeGrowthEveryYear = isLargeGrowthEveryYear(financials, 6, 10);

            if (tenYearAvgGrowth.isPresent() && continouslyProfitable && isLargeGrowthEveryYear) {
                //                Double peRatio = financials.get(0).remoteRatio.priceEarningsRatio;
                double currentPe = company.latestPrice / financials.get(0).incomeStatementTtm.eps;
                double growth = tenYearAvgGrowth.get();
                Double currentRatio = RatioCalculator.calculateCurrentRatio(financials.get(0)).orElse(null);
                if (growth >= 35.0 &&
                        currentRatio != null && currentRatio > 1.0) {
                    System.out.println(symbol + " " + growth + " " + currentPe);
                }
            }

        }

    }

}
