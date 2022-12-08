package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthCalculator.getGrowthInInterval;
import static java.lang.Double.NaN;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.DcfCalculator;
import com.helospark.financialdata.service.GrowthStandardDeviationCounter;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;
import com.helospark.financialdata.service.TrailingPegCalculator;

public class CompounderDcfScreenerBacktest {
    static double EPS_SD = 10.0;
    static double REV_SD = 20.0;
    static double GROWTH = 12.0;
    static double PEG_CUTOFF = 1.5;

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

                Optional<Double> tenYearAvgGrowth = getGrowthInInterval(financials, 8 + yearsAgo, yearsAgo);
                boolean continouslyProfitable = isProfitableEveryYearSince(financials, 8 + yearsAgo, yearsAgo);
                Optional<Double> epsDeviation = GrowthStandardDeviationCounter.calculateEpsGrowthDeviation(company.financials, yearsAgo);
                Optional<Double> revenueDeviation = GrowthStandardDeviationCounter.calculateRevenueGrowthDeviation(company.financials, yearsAgo);
                Optional<Double> fcfDeviation = GrowthStandardDeviationCounter.calculateFcfGrowthDeviation(company.financials, yearsAgo);
                double altmanZ = AltmanZCalculator.calculateAltmanZScore(financials.get(index), latestPriceThen);

                Optional<Double> trailingPeg = TrailingPegCalculator.calculateTrailingPeg(company, index);
                Optional<Double> trailingPeg2 = TrailingPegCalculator.calculateTrailingPeg(company, index + 1);
                Optional<Double> trailingPeg3 = TrailingPegCalculator.calculateTrailingPeg(company, index + 2);

                if (tenYearAvgGrowth.isPresent() && continouslyProfitable && epsDeviation.isPresent() && revenueDeviation.isPresent()) {
                    double currentPe = latestPriceThen / financials.get(index).incomeStatementTtm.eps;
                    double growth = tenYearAvgGrowth.get();
                    Double epsStandardDeviation = epsDeviation.get();

                    Optional<Double> dcf = DcfCalculator.doFullDcfAnalysisWithGrowth(financials, yearsAgo);
                    double upside = dcf.isPresent() ? dcf.get() / latestPriceThen : 0.0;

                    if (growth >= GROWTH && epsStandardDeviation < EPS_SD && revenueDeviation.get() < REV_SD && altmanZ > 2.2 &&
                            upside >= 2.0) {
                        double sellPrice = company.latestPrice;
                        double growthRatio = sellPrice / latestPriceThen;
                        double benchmarkIncrease = (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(index).getDate()));

                        yearGrowthSum += (growthRatio * 1000.0);
                        yearBenchmarkSum += benchmarkIncrease * 1000.0;
                        growthSum += (growthRatio * 1000.0);
                        benchmarkSum += benchmarkIncrease * 1000.0;
                        ++yearCount;

                        System.out.printf("%s\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f%% (%.1f -> %.1f)\t%s | %s\n", symbol, growth, currentPe, epsStandardDeviation, revenueDeviation.orElse(NaN),
                                upside * 100.0, ((growthRatio - 1.0) * 100.0), latestPriceThen, sellPrice, company.profile.companyName, company.profile.industry);

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
        System.out.println("Total " + growthSum + " b=" + benchmarkSum + " (" + (int) (growthSum / benchmarkSum * 100) + ") beat " + beats + " / " + count);
    }

}
