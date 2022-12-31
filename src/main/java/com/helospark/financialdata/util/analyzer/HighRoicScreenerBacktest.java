package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.GrowthCorrelationCalculator;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;
import com.helospark.financialdata.service.TrailingPegCalculator;

public class HighRoicScreenerBacktest {
    private static final double PEG = 1.3;
    private static final double ROIC = 0.32;
    private static final double PROFITABLE = 4.0;
    private static final double ALTMAN = 5.2;

    //    private static final double PEG = 1.3;
    //    private static final double ROIC = 0.32;
    //    private static final double PROFITABLE = 4.0;

    public void analyze(Set<String> symbols) {
        double growthSum = 0.0;
        double benchmarkSum = 0.0;
        int count = 0;
        int beats = 0;
        System.out.printf("Symbol\t\tROIC\tPEG\tCorr\tGrowth%% (from->to)\tCompany\n");
        for (int index = 3 * 4; index <= 28 * 4; ++index) {
            double yearsAgo = (index / 4.0);
            double yearGrowthSum = 0.0;
            double yearBenchmarkSum = 0.0;
            int yearCount = 0;
            for (var symbol : symbols) {
                CompanyFinancials company = readFinancials(symbol);
                var financials = company.financials;

                if (financials.isEmpty() || financials.size() <= index) {
                    continue;
                }
                double latestPriceThen = financials.get(index).price;

                boolean continouslyProfitable = isProfitableEveryYearSince(financials, PROFITABLE + yearsAgo, yearsAgo);
                //                boolean continouslyProfitableFcf = GrowthAnalyzer.isCashFlowProfitableEveryYearSince(financials, 7.0 + yearsAgo, yearsAgo);
                double altmanZ = AltmanZCalculator.calculateAltmanZScore(financials.get(index), latestPriceThen);

                if (altmanZ > ALTMAN && continouslyProfitable) {
                    Optional<Double> roic = RoicCalculator.getAverageRoic(company.financials, yearsAgo);
                    Optional<Double> trailingPeg = TrailingPegCalculator.calculateTrailingPeg(company, index);
                    //                    double marketCap = financials.get(index).keyMetrics.enterpriseValue / (1000.0 * 1000.0 * 1000.0);

                    if (roic.isPresent() && roic.get() > ROIC && trailingPeg.isPresent() && trailingPeg.get() < PEG) {
                        double sellPrice = company.latestPrice;
                        double growthRatio = sellPrice / latestPriceThen;
                        double benchmarkIncrease = (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(index).getDate()));

                        yearGrowthSum += (growthRatio * 1000.0);
                        yearBenchmarkSum += benchmarkIncrease * 1000.0;
                        growthSum += (growthRatio * 1000.0);
                        benchmarkSum += benchmarkIncrease * 1000.0;
                        ++yearCount;

                        Double correlation = GrowthCorrelationCalculator.calculateEpsFcfCorrelation(company.financials, yearsAgo + 5.0, yearsAgo).orElse(Double.NaN);

                        System.out.printf("%s\t\t%.2f\t%.2f\t%.2f\t%.1f%% (%.1f -> %.1f)\t%s | %s\n", symbol, roic.get(), trailingPeg.get(), correlation, ((growthRatio - 1.0) * 100.0),
                                latestPriceThen,
                                sellPrice,
                                company.profile.companyName,
                                company.profile.industry);

                    }
                }
            }
            if (yearGrowthSum > yearBenchmarkSum) {
                ++beats;
            }
            if (yearBenchmarkSum > 0) {
                ++count;
            }
            double increase = (yearGrowthSum / (yearCount * 1000) - 1.0);
            double annual = Math.pow(yearGrowthSum / (yearCount * 1000), (1.0 / yearsAgo)) - 1.0;
            double benchmark = (Math.pow(yearBenchmarkSum / (yearCount * 1000), (1.0 / yearsAgo)) - 1.0) * 100.0;
            LocalDate dateThen = LocalDate.now().minusMonths((long) (yearsAgo * 12.0));
            System.out.println(
                    "Have " + (int) (yearGrowthSum) + " from " + yearCount * 1000 + " (" + (increase * 100.0) + "%, " + (annual * 100.0) + "%) invested "
                            + dateThen
                            + " benchmark=" + benchmark);
            System.out.println("CSV " + dateThen + "," + (annual * 100.0) + "," + benchmark + "," + (int) yearGrowthSum + "," + (int) yearBenchmarkSum + "," + (yearCount * 1000));
            System.out.println();
        }
        System.out.println("Total " + growthSum + " b=" + benchmarkSum + " beat " + beats + " / " + count);
    }

}
