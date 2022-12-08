package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;
import com.helospark.financialdata.service.TrailingPegCalculator;

public class HighRoicScreenerBacktest {

    //    private static final double PEG = 1.5;
    //    private static final double ROIC = 0.12;

    private static final double PEG = 1.3; // 1.7; 1.3
    private static final double ROIC = 0.08; // 0.21; 0.08

    public void analyze(Set<String> symbols) {
        double growthSum = 0.0;
        double benchmarkSum = 0.0;
        int count = 0;
        int beats = 0;
        System.out.printf("Symbol\tGrowth\tPE\tEPS_SD\tREV_SD\tFCF_SD\tGrowth%% (from->to)\n");
        for (int index = 0; index <= 25 * 4; ++index) {
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

                boolean continouslyProfitable = isProfitableEveryYearSince(financials, 8.0 + yearsAgo, yearsAgo);
                double altmanZ = AltmanZCalculator.calculateAltmanZScore(financials.get(index), company.latestPrice);

                if (altmanZ > 2.0 && continouslyProfitable) {
                    Optional<Double> roic = RoicCalculator.getAverageRoic(company.financials, yearsAgo);
                    Optional<Double> trailingPeg = TrailingPegCalculator.calculateTrailingPeg(company, index);

                    if (roic.isPresent() && roic.get() > ROIC && trailingPeg.isPresent() && trailingPeg.get() < PEG) {
                        double sellPrice = company.latestPrice;
                        double growthRatio = sellPrice / latestPriceThen;
                        double benchmarkIncrease = (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(index).getDate()));

                        yearGrowthSum += (growthRatio * 1000.0);
                        yearBenchmarkSum += benchmarkIncrease * 1000.0;
                        growthSum += (growthRatio * 1000.0);
                        benchmarkSum += benchmarkIncrease * 1000.0;
                        ++yearCount;

                        System.out.printf("%s\t%.2f\t%.2f\t%.1f%% (%.1f -> %.1f)\t%s | %s\n", symbol, roic.get(), trailingPeg.get(), ((growthRatio - 1.0) * 100.0), latestPriceThen, sellPrice,
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
            double benchmark = StandardAndPoorPerformanceProvider.getGrowth(yearsAgo);
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
