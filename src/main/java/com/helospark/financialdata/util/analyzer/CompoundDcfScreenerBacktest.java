package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.DcfCalculator;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;

public class CompoundDcfScreenerBacktest {

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
                    int numberOfDcfMatch = 0;
                    double avg = 0.0;
                    Optional<Double> firstDcf = DcfCalculator.doFullDcfAnalysisWithGrowth(company.financials, yearsAgo);
                    for (int i = 0; i < 15; ++i) {
                        double yearOffset = i / 4.0;
                        Optional<Double> dcf = DcfCalculator.doFullDcfAnalysisWithGrowth(company.financials, yearsAgo + yearOffset);
                        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) ((yearsAgo + yearOffset) * 12.0)));
                        if (dcf.isPresent() && dcf.get() > financials.get(oldIndex).price) {
                            ++numberOfDcfMatch;
                            avg += dcf.get();
                        }
                    }

                    avg /= numberOfDcfMatch;
                    double dcfUpside = (avg / latestPriceThen - 1.0) * 100.0;

                    if (numberOfDcfMatch >= 13 && dcfUpside > 200) {
                        double sellPrice = company.latestPrice;
                        double growthRatio = sellPrice / latestPriceThen;
                        double benchmarkIncrease = (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(index).getDate()));

                        yearGrowthSum += (growthRatio * 1000.0);
                        yearBenchmarkSum += benchmarkIncrease * 1000.0;
                        growthSum += (growthRatio * 1000.0);
                        benchmarkSum += benchmarkIncrease * 1000.0;
                        ++yearCount;

                        System.out.printf("%s\t%.2f\t%.1f%% (%.1f -> %.1f)\t%s | %s\n", symbol, dcfUpside, ((growthRatio - 1.0) * 100.0), latestPriceThen, sellPrice,
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
