package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.CapeCalculator;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;

public class LowCapeScreenerBacktest {

    static double CAPE_LIMIT = 10;
    static int PROF_YEAR = 5;
    static int DEVIATION_YEAR = 6;

    public void analyze(Set<String> symbols) {
        double growthSum = 0.0;
        double benchmarkSum = 0.0;
        int count = 0;
        int beats = 0;
        System.out.printf("Symbol\tCAPE\troic\tGrowth%% (from->to)\n");
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

                boolean continouslyProfitable = isProfitableEveryYearSince(financials, 5 + yearsAgo, yearsAgo);
                double cape = CapeCalculator.calculateCapeRatioQ(company.financials, 5, index);
                double altmanZ = AltmanZCalculator.calculateAltmanZScore(financials.get(index), latestPriceThen);
                Optional<Double> roic = RoicCalculator.getAverageRoic(company.financials, yearsAgo);

                if (cape < 20 && cape > 8 && altmanZ > 2.2 && continouslyProfitable && roic.isPresent() && roic.get() > 0.3 && !company.profile.industry.contains("Oil")) {
                    double sellPrice = company.latestPrice;
                    double growthRatio = sellPrice / latestPriceThen;
                    double benchmarkIncrease = (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(index).getDate()));

                    yearGrowthSum += (growthRatio * 1000.0);
                    yearBenchmarkSum += benchmarkIncrease * 1000.0;
                    growthSum += (growthRatio * 1000.0);
                    benchmarkSum += benchmarkIncrease * 1000.0;
                    ++yearCount;

                    System.out.printf("%s\t%.1f\t%.2f%%\t%.1f%% (%.1f -> %.1f)\t%s | %s\n", symbol, cape, (roic.get() * 100.0), ((growthRatio - 1.0) * 100.0), latestPriceThen, sellPrice,
                            company.profile.companyName,
                            company.profile.industry);

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
