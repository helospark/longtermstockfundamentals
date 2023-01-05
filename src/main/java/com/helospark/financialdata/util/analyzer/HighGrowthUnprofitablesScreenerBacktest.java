package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.AltmanZCalculator.calculateAltmanZScore;
import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthCalculator.getRevenueGrowthInInterval;
import static com.helospark.financialdata.service.MarginCalculator.getNetMarginGrowthRate;
import static com.helospark.financialdata.service.RevenueProjector.projectRevenue;

import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.RatioCalculator;

public class HighGrowthUnprofitablesScreenerBacktest {

    public void analyze(Set<String> symbols) {
        for (int yearsAgo = 4; yearsAgo <= 21; ++yearsAgo) {
            double growthSum = 0.0;
            int count = 0;
            for (var symbol : symbols) {
                CompanyFinancials company = readFinancials(symbol);
                var financials = company.financials;

                if (financials.isEmpty()) {
                    continue;
                }

                int latestElement = yearsAgo * 4;
                if (financials.size() > latestElement + 1) {

                    Optional<Double> sevenGrowth = getRevenueGrowthInInterval(financials, 7 + yearsAgo, yearsAgo);
                    Optional<Double> avgGrowth = getRevenueGrowthInInterval(financials, 5 + yearsAgo, yearsAgo);
                    Optional<Double> twoYrGrowth = getRevenueGrowthInInterval(financials, 2 + yearsAgo, yearsAgo);

                    Optional<Double> fiveYearMarginGrowth = getNetMarginGrowthRate(financials, 5 + yearsAgo, yearsAgo);

                    double latestPriceThen = financials.get(latestElement).price;

                    if (avgGrowth.isPresent() && avgGrowth.get() >= 28.0 &&
                            twoYrGrowth.isPresent() && twoYrGrowth.get() >= 23.0 &&
                            sevenGrowth.isPresent() && sevenGrowth.get() >= 20.0 &&
                            fiveYearMarginGrowth.isPresent()) {
                        double growth = Math.min(avgGrowth.get(), twoYrGrowth.get());

                        double startGrowth = growth * 0.7;
                        double endGrowth = growth * 0.4;

                        double predictedPrice = projectRevenue(financials.get(latestElement), startGrowth, endGrowth);

                        Double currentRatio = RatioCalculator.calculateCurrentRatio(financials.get(latestElement)).orElse(null);
                        double altmanZ = calculateAltmanZScore(financials.get(latestElement), latestPriceThen);
                        double cashFlowBurnPerYear = financials.get(latestElement).cashFlowTtm.freeCashFlow;
                        long cash = financials.get(latestElement).balanceSheet.cashAndShortTermInvestments;
                        double currentPrice = latestPriceThen;

                        if (currentPrice < predictedPrice &&
                                currentRatio != null && currentRatio > 1.0 &&
                                altmanZ >= 0.8 &&
                                (cashFlowBurnPerYear >= 0 || (((cash / -cashFlowBurnPerYear) > 3.0)))) {

                            int sellYearIndex = -1;

                            /*for (; sellYearIndex >= 0; --sellYearIndex) {

                                double currentGrowth = getRevenueGrowthInInterval(financials, yearsAgo, sellYearIndex).orElse(0.0);

                                if (currentGrowth < -5) {
                                    break;
                                }
                                System.out.print(currentGrowth + " ");
                            }
                            System.out.println();*/

                            double sellPrice = sellYearIndex == -1 ? company.latestPrice : financials.get(sellYearIndex * 4).price;

                            double upside = (predictedPrice / currentPrice) * 100.0;
                            double growthRatio = sellPrice / latestPriceThen;
                            growthSum += (growthRatio * 1000.0);
                            ++count;

                            double growthPcnt = (growthRatio - 1.0) * 100.0;
                            System.out.printf("%s\t(%.2f, %.2f, %.2f)\t\t%.2f\t%.2f\t%.2f%%\t\t%.2f%% (%.2f -> %.2f)\n",
                                    symbol,
                                    avgGrowth.get(), twoYrGrowth.get(), sevenGrowth.get(),
                                    altmanZ, fiveYearMarginGrowth.get(), upside, growthPcnt,
                                    latestPriceThen, company.latestPrice);
                        }
                    }
                }
            }
            double increase = (growthSum / (count * 1000) - 1.0);
            double annual = (Math.pow(growthSum / (count * 1000), (1.0 / yearsAgo)) - 1.0) * 100.0;
            System.out.println("Have " + (growthSum) + " from " + (count * 1000) + " (" + (increase * 100.0) + "%, " + (annual) + "% " + ") invested " + yearsAgo);
            System.out.println();
        }

    }

}
