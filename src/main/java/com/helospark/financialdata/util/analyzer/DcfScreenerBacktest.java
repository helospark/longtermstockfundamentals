package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.AltmanZCalculator.calculateAltmanZScore;
import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.DcfCalculator.doStockDcfAnalysis;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthAnalyzer.isStableGrowth;
import static com.helospark.financialdata.service.GrowthCalculator.getEpsGrowthInInterval;
import static com.helospark.financialdata.service.Helpers.min;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.GrowthStandardDeviationCounter;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;

public class DcfScreenerBacktest implements StockScreeners {
    static double UPSIDE_CUTOFF = 90;
    static double PROFITABLE_YEAR = 5.0;
    static double ALTMAN_CUTOFF = 4.0;

    @Override
    public void analyze(Set<String> symbols) {
        for (int index = 5; index <= 21 * 4; ++index) {
            double yearsAgo = (index / 4.0);
            double growthSum = 0.0;
            int count = 0;
            System.out.println("symbol\t(Growth1, Growth2, Growth3)\t\tDCF\tPE\tUpside%\tfcfUpside%");
            for (var symbol : symbols) {
                CompanyFinancials company = readFinancials(symbol);
                var financials = company.financials;

                if (financials.isEmpty()) {
                    continue;
                }

                int latestElement = index;
                //                System.out.println(financials.size() + " > " + (latestElement + 1));
                if (financials.size() > latestElement + 1) {

                    Optional<Double> tenYearAvgGrowth = getEpsGrowthInInterval(financials, PROFITABLE_YEAR + yearsAgo, yearsAgo);
                    Optional<Double> eightYearAvgGrowth = getEpsGrowthInInterval(financials, 8 + yearsAgo, yearsAgo);
                    Optional<Double> fiveYearAvgGrowth = getEpsGrowthInInterval(financials, 5 + yearsAgo, yearsAgo);
                    Optional<Double> threeYearAvgGrowth = getEpsGrowthInInterval(financials, 3 + yearsAgo, yearsAgo);

                    //                    System.out.println(tenYearAvgGrowth + " " + eightYearAvgGrowth + " " + fiveYearAvgGrowth + " " + threeYearAvgGrowth);

                    double latestPriceThen = financials.get(latestElement).price;

                    boolean continouslyProfitable = isProfitableEveryYearSince(financials, PROFITABLE_YEAR + yearsAgo, yearsAgo);
                    boolean stableGrowth = isStableGrowth(financials, PROFITABLE_YEAR - 2 + yearsAgo, yearsAgo);
                    double altmanZ = financials.size() > latestElement ? calculateAltmanZScore(financials.get(latestElement), latestPriceThen) : 0.0;

                    //                    System.out.println(latestPriceThen + " " + continouslyProfitable + " " + stableGrowth + " " + altmanZ);

                    if (tenYearAvgGrowth.isPresent() &&
                            stableGrowth &&
                            continouslyProfitable &&
                            altmanZ > ALTMAN_CUTOFF &&
                            financials.get(latestElement).incomeStatementTtm.eps > 0.0 &&
                            tenYearAvgGrowth.get() > 0.0) {
                        double growth = tenYearAvgGrowth.get();

                        double dcf = doStockDcfAnalysis(financials.get(latestElement).incomeStatementTtm.eps, min(growth));

                        long fcf = financials.get(latestElement).cashFlowTtm.freeCashFlow;
                        double fcfPerShare = (double) fcf / financials.get(latestElement).incomeStatementTtm.weightedAverageShsOut;
                        double dcfFcf = doStockDcfAnalysis(fcfPerShare, min(growth));

                        double currentPe = latestPriceThen / financials.get(latestElement).incomeStatementTtm.eps;

                        double upside = (dcf / latestPriceThen - 1.0) * 100;
                        double fcfUpside = (dcfFcf / latestPriceThen - 1.0) * 100;

                        Optional<Double> growthDeviation = GrowthStandardDeviationCounter.calculateEpsGrowthDeviation(company.financials, yearsAgo);

                        System.out.println(symbol + "\t" + upside + " " + fcfUpside + " " + growthDeviation);

                        if (upside > UPSIDE_CUTOFF && fcfUpside > UPSIDE_CUTOFF) {
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
                                    growth, eightYearAvgGrowth.orElse(Double.NaN), fiveYearAvgGrowth.orElse(Double.NaN), twoYearGrowth,
                                    dcf, currentPe, upside,
                                    fcfUpside,
                                    growthTillSell, latestPriceThen, sellPrice, negativeCount);
                        }
                    }
                }
            }
            double increase = (growthSum / (count * 1000) - 1.0);
            double annual = Math.pow(growthSum / (count * 1000), (1.0 / yearsAgo)) - 1.0;
            double benchmark = StandardAndPoorPerformanceProvider.getGrowth(yearsAgo);
            System.out.println(
                    "Have " + (int) (growthSum) + " from " + count * 1000 + " (" + (increase * 100.0) + "%, " + (annual * 100.0) + "%) invested "
                            + LocalDate.now().minusMonths((long) (yearsAgo * 12.0))
                            + " benchmark=" + benchmark);
            System.out.println();
        }
    }

}
