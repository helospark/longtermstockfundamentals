package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.AltmanZCalculator.calculateAltmanZScore;
import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.DcfCalculator.doDcfAnalysis;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthAnalyzer.isStableGrowth;
import static com.helospark.financialdata.service.GrowthCalculator.getGrowthInInterval;
import static com.helospark.financialdata.service.Helpers.min;

import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;

public class DcfScreenerBacktest implements StockScreeners {

    @Override
    public void analyze(Set<String> symbols) {
        for (int yearsAgo = 5; yearsAgo <= 21; ++yearsAgo) {
            double growthSum = 0.0;
            int count = 0;
            System.out.println("symbol\t(Growth1, Growth2, Growth3)\t\tDCF\tPE\tUpside%\tfcfUpside%");
            for (var symbol : symbols) {
                CompanyFinancials company = readFinancials(symbol);
                var financials = company.financials;

                if (financials.isEmpty()) {
                    continue;
                }

                int latestElement = yearsAgo * 4;
                //                System.out.println(financials.size() + " > " + (latestElement + 1));
                if (financials.size() > latestElement + 1) {

                    Optional<Double> tenYearAvgGrowth = getGrowthInInterval(financials, 10 + yearsAgo, yearsAgo);
                    Optional<Double> eightYearAvgGrowth = getGrowthInInterval(financials, 8 + yearsAgo, yearsAgo);
                    Optional<Double> fiveYearAvgGrowth = getGrowthInInterval(financials, 5 + yearsAgo, yearsAgo);
                    Optional<Double> threeYearAvgGrowth = getGrowthInInterval(financials, 3 + yearsAgo, yearsAgo);

                    //                    System.out.println(tenYearAvgGrowth + " " + eightYearAvgGrowth + " " + fiveYearAvgGrowth + " " + threeYearAvgGrowth);

                    double latestPriceThen = financials.get(latestElement).price;

                    boolean continouslyProfitable = isProfitableEveryYearSince(financials, 10 + yearsAgo, yearsAgo);
                    boolean stableGrowth = isStableGrowth(financials, 8 + yearsAgo, yearsAgo);
                    double altmanZ = financials.size() > latestElement ? calculateAltmanZScore(financials.get(latestElement), latestPriceThen) : 0.0;

                    //                    System.out.println(latestPriceThen + " " + continouslyProfitable + " " + stableGrowth + " " + altmanZ);

                    if (tenYearAvgGrowth.isPresent() &&
                            stableGrowth &&
                            continouslyProfitable &&
                            altmanZ > 2.2 &&
                            financials.get(latestElement).incomeStatementTtm.eps > 0.0 &&
                            tenYearAvgGrowth.get() > 0.0) {
                        double growth = tenYearAvgGrowth.get();
                        double fiveYearGrowth = fiveYearAvgGrowth.get();
                        double tenYearGrowth = tenYearAvgGrowth.get();
                        double eightYearGrowth = eightYearAvgGrowth.get();

                        double dcf = doDcfAnalysis(financials.get(latestElement).incomeStatementTtm.eps, min(tenYearGrowth));

                        long fcf = financials.get(latestElement).cashFlowTtm.freeCashFlow;
                        double fcfPerShare = (double) fcf / financials.get(latestElement).incomeStatementTtm.weightedAverageShsOut;
                        double dcfFcf = doDcfAnalysis(fcfPerShare, min(tenYearGrowth));

                        double currentPe = latestPriceThen / financials.get(latestElement).incomeStatementTtm.eps;

                        double upside = (dcf / latestPriceThen - 1.0) * 100;
                        double fcfUpside = (dcfFcf / latestPriceThen - 1.0) * 100;

                        //                        System.out.println(symbol + "\t" + upside + " " + fcfUpside);

                        if (upside > 25.0 && fcfUpside > 25.0) {
                            double twoYearGrowth = threeYearAvgGrowth.get();

                            int negativeCount = 0;
                            int sellQuarter = 0;
                            for (sellQuarter = latestElement - 1; sellQuarter >= 0; --sellQuarter) {
                                if (financials.get(sellQuarter).incomeStatementTtm.eps < 0.0) {
                                    ++negativeCount;
                                    break;
                                }
                            }
                            double sellPrice = sellQuarter == -1 ? company.latestPrice : financials.get(sellQuarter).price;
                            double growthRatio = sellPrice / latestPriceThen;
                            growthSum += (growthRatio * 1000.0);
                            ++count;

                            double growthTillSell = (growthRatio - 1.0) * 100.0;
                            /*
                            int sellYearIndex = yearsAgo - 1;
                            for (; sellYearIndex >= 0; --sellYearIndex) {
                            
                                double currentGrowth = GrowthCalculator.getGrowthInInterval(financials, yearsAgo, sellYearIndex).orElse(0.0);
                                System.out.print(currentGrowth + " ");
                            }
                            System.out.println();*/

                            System.out.printf("%s\t(%.1f, %.1f, %.1f, %.1f)   \t\t%.1f\t%.1f\t%.1f%%\t%.1f%%\t\t%.1f%% (%.1f -> %.1f)\t%d\n", symbol,
                                    growth, eightYearGrowth, fiveYearGrowth, twoYearGrowth,
                                    dcf, currentPe, upside,
                                    fcfUpside,
                                    growthTillSell, latestPriceThen, sellPrice, negativeCount);
                        }
                    }
                }
            }
            double increase = (growthSum / (count * 1000) - 1.0);
            double annual = Math.pow(growthSum / (count * 1000), (1.0 / yearsAgo)) - 1.0;
            System.out.println("Have " + (growthSum) + " from " + (count * 1000) + " (" + (increase * 100.0) + "%, " + (annual * 100.0) + "%) invested " + yearsAgo);
            System.out.println();
        }
    }

}
