package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.AltmanZCalculator.calculateAltmanZScore;
import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthCalculator.getRevenueGrowthInInterval;
import static com.helospark.financialdata.service.RevenueProjector.projectRevenue;

import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.RatioCalculator;

public class HighGrowthUnprofitablesScreener {

    public void analyze(Set<String> symbols) {
        for (var symbol : symbols) {
            CompanyFinancials company = readFinancials(symbol);
            var financials = company.financials;

            if (financials.isEmpty()) {
                continue;
            }

            Optional<Double> avgGrowth = getRevenueGrowthInInterval(financials, 5, 0);

            if (avgGrowth.isPresent() && avgGrowth.get() >= 35.0) {
                double growth = avgGrowth.get();

                double startGrowth = growth * 0.6;
                double endGrowth = growth * 0.3;

                double predictedPrice = projectRevenue(financials.get(0), startGrowth, endGrowth);

                Double currentRatio = RatioCalculator.calculateCurrentRatio(financials.get(0));
                double altmanZ = calculateAltmanZScore(financials.get(0), company.latestPrice);
                double cashFlowBurnPerYear = financials.get(0).cashFlowTtm.freeCashFlow;
                long cash = financials.get(0).balanceSheet.cashAndShortTermInvestments;
                double currentPrice = company.latestPrice;

                if (currentPrice < predictedPrice &&
                        currentRatio != null && currentRatio > 1.0 &&
                        altmanZ >= 2.3 &&
                        (cashFlowBurnPerYear >= 0 || (((cash / -cashFlowBurnPerYear) > 3.0)))) {
                    double upside = (predictedPrice / currentPrice) * 100.0;
                    System.out.println(symbol + " " + growth + " " + altmanZ + " " + upside + "%");
                }
            }

        }

    }

}
