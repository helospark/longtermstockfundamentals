package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.service.FlagsProviderService;
import com.helospark.financialdata.service.GrowthStandardDeviationCounter;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;
import com.helospark.financialdata.service.TrailingPegCalculator;

public class FlagsScreenerBacktest {

    public void analyze(Set<String> symbols) {
        double growthSum = 0.0;
        double benchmarkSum = 0.0;
        int count = 0;
        int beats = 0;
        System.out.printf("Symbol\tStar\tGreen\tYellow\tRed\tGrowth%% (from->to)\n");
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

                List<FlagInformation> flags = FlagsProviderService.giveFlags(company, yearsAgo);

                int numRed = 0;
                int numYellow = 0;
                int numGreen = 0;
                int numStar = 0;
                for (var element : flags) {
                    if (element.type == FlagType.RED) {
                        ++numRed;
                    } else if (element.type == FlagType.YELLOW) {
                        ++numYellow;
                    } else if (element.type == FlagType.GREEN) {
                        ++numGreen;
                    } else if (element.type == FlagType.STAR) {
                        ++numStar;
                    }
                }
                Optional<Double> epsDeviation = GrowthStandardDeviationCounter.calculateEpsGrowthDeviation(company.financials, yearsAgo);
                Optional<Double> revenueDeviation = GrowthStandardDeviationCounter.calculateRevenueGrowthDeviation(company.financials, yearsAgo);
                //                Optional<Double> fcfDeviation = GrowthStandardDeviationCounter.calculateFcfGrowthDeviation(company.financials, yearsAgo);
                Optional<Double> trailingPeg = TrailingPegCalculator.calculateTrailingPeg(company, yearsAgo);
                //                Optional<Double> trailingPeg2 = TrailingPegCalculator.calculateTrailingPeg(company, index + 1);
                //                Optional<Double> trailingPeg3 = TrailingPegCalculator.calculateTrailingPeg(company, index + 2);
                Optional<Double> roic = RoicCalculator.getAverageRoic(company.financials, yearsAgo);

                if (numStar >= 2 && numGreen >= 3 && numYellow <= 1 && numRed < 1 &&
                        epsDeviation.isPresent() && epsDeviation.get() < 10 &&
                        revenueDeviation.isPresent() && revenueDeviation.get() < 20 &&
                        trailingPeg.isPresent() && trailingPeg.get() < 1.5 &&
                        roic.isPresent() && roic.get() > 0.30) {
                    double sellPrice = company.latestPrice;
                    double growthRatio = sellPrice / latestPriceThen;
                    double benchmarkIncrease = (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(index).getDate()));

                    if (Double.isFinite(growthRatio)) {
                        yearGrowthSum += (growthRatio * 1000.0);
                        yearBenchmarkSum += benchmarkIncrease * 1000.0;
                        growthSum += (growthRatio * 1000.0);
                        benchmarkSum += benchmarkIncrease * 1000.0;
                        ++yearCount;

                        System.out.printf("%s\t%d\t%d\t%d\t%d\t%.1f%% (%.1f -> %.1f)\t%s | %s\n", symbol, numStar, numGreen, numYellow, numRed, ((growthRatio - 1.0) * 100.0), latestPriceThen,
                                sellPrice,
                                company.profile.companyName, company.profile.industry);
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
