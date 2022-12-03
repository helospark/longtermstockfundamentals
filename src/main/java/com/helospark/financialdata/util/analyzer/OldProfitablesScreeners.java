package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthAnalyzer.isStableGrowth;
import static com.helospark.financialdata.service.GrowthCalculator.getGrowthInInterval;

import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.RemoteRatio;

public class OldProfitablesScreeners {

    public void analyze(Set<String> symbols) {
        for (var symbol : symbols) {
            CompanyFinancials company = readFinancials(symbol);
            var financials = company.financials;

            if (financials.isEmpty()) {
                continue;
            }

            Optional<Double> tenYearAvgGrowth = getGrowthInInterval(financials, 15, 0);
            boolean continouslyProfitable = isProfitableEveryYearSince(financials, 15, 0);
            boolean stableGrowth = isStableGrowth(financials, 15, 0);

            if (tenYearAvgGrowth.isPresent() && continouslyProfitable && stableGrowth) {
                //                Double peRatio = financials.get(0).remoteRatio.priceEarningsRatio;
                double currentPe = company.latestPrice / financials.get(0).incomeStatementTtm.eps;
                double growth = tenYearAvgGrowth.get();
                RemoteRatio ratios = financials.get(0).remoteRatio;
                Double currentRatio = ratios.currentRatio;

                if (growth >= 8.0 &&
                        currentPe <= 20.0 &&
                        //growth >= currentPe &&
                        currentRatio != null && currentRatio > 1.0) {
                    System.out.println(symbol + " " + growth + " " + currentPe);
                }
            }

        }
    }

}
