package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthCalculator.getEpsGrowthInInterval;
import static java.lang.Double.NaN;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.GrowthStandardDeviationCounter;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;
import com.helospark.financialdata.service.TrailingPegCalculator;

public class CompounderRoicScreenerBacktest {
    //    static double PEG_CUTOFF = 1.1;
    //    static double EPS_SD = 18.0;
    //    static double REV_SD = 7.0;
    //    static double GROWTH = 12.0;

    static double EPS_SD = 5.0;
    static double REV_SD = 21.0;
    static double GROWTH = 12.0;
    static double PEG_CUTOFF = 1.4;
    static double ROIC_CUTOFF = 0.31;
    static int PROF_YEAR = 5;
    static int DEVIATION_YEAR = 6;

    public void analyze(Set<String> symbols) {
        double growthSum = 0.0;
        double benchmarkSum = 0.0;
        int count = 0;
        int beats = 0;
        System.out.printf("Symbol\tGrowth\ttPEG\tEPS_SD\tREV_SD\tFCF_SD\tGrowth%% (from->to)\n");
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

                Optional<Double> tenYearAvgGrowth = getEpsGrowthInInterval(financials, PROF_YEAR + yearsAgo, yearsAgo);
                boolean continouslyProfitable = isProfitableEveryYearSince(financials, PROF_YEAR + yearsAgo, yearsAgo);
                Optional<Double> epsDeviation = GrowthStandardDeviationCounter.calculateEpsGrowthDeviation(company.financials, yearsAgo, DEVIATION_YEAR);
                Optional<Double> revenueDeviation = GrowthStandardDeviationCounter.calculateRevenueGrowthDeviation(company.financials, yearsAgo, DEVIATION_YEAR);

                if (tenYearAvgGrowth.isPresent() && continouslyProfitable && epsDeviation.isPresent() && revenueDeviation.isPresent()) {
                    double altmanZ = AltmanZCalculator.calculateAltmanZScore(financials.get(index), latestPriceThen);
                    Optional<Double> trailingPeg = TrailingPegCalculator.calculateTrailingPeg(company, index);

                    Optional<Double> roic = RoicCalculator.getAverageRoic(company.financials, yearsAgo);
                    double growth = tenYearAvgGrowth.get();
                    Double epsStandardDeviation = epsDeviation.get();
                    if (roic.isPresent() && roic.get() > ROIC_CUTOFF && growth >= GROWTH && epsDeviation.get() < EPS_SD && revenueDeviation.get() < REV_SD && altmanZ > 2.0 &&
                            trailingPeg.isPresent() && trailingPeg.get() < PEG_CUTOFF) {
                        double sellPrice = company.latestPrice;
                        double growthRatio = sellPrice / latestPriceThen;
                        double benchmarkIncrease = (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(index).getDate()));

                        yearGrowthSum += (growthRatio * 1000.0);
                        yearBenchmarkSum += benchmarkIncrease * 1000.0;
                        growthSum += (growthRatio * 1000.0);
                        benchmarkSum += benchmarkIncrease * 1000.0;
                        ++yearCount;

                        System.out.printf("%s\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f%% (%.1f -> %.1f)\t%s | %s\n", symbol, growth, trailingPeg.get(), epsStandardDeviation, revenueDeviation.orElse(NaN),
                                0.0, ((growthRatio - 1.0) * 100.0), latestPriceThen, sellPrice, company.profile.companyName, company.profile.industry);

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
