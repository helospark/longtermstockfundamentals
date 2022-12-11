package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthCalculator.getGrowthInInterval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.GrowthStandardDeviationCounter;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;
import com.helospark.financialdata.service.TrailingPegCalculator;
import com.helospark.financialdata.util.analyzer.parameter.LimitedRandomStrategy;
import com.helospark.financialdata.util.analyzer.parameter.Parameter;
import com.helospark.financialdata.util.analyzer.parameter.TestParameterProvider;

public class CompounderScreenerBacktest2 {
    private static final double INVEST_PER_STOCK = 1000.0;

    public void analyze(Set<String> symbols) {
        long start = System.currentTimeMillis();
        TestParameterProvider param = new TestParameterProvider();
        param.registerParameter(new Parameter("epsSd", 2.0, new LimitedRandomStrategy(10, 1.0, 10.0)));
        param.registerParameter(new Parameter("revSd", 6.0, new LimitedRandomStrategy(10, 4.0, 30.0)));
        param.registerParameter(new Parameter("fcfSd", 30.0, new LimitedRandomStrategy(10, 4.0, 45.0)));
        boolean finished = false;

        List<TestParameterProvider> providerList = new ArrayList<>();
        Queue<TestParameterProvider> elements = new ConcurrentLinkedQueue<>();
        do {
            providerList.add(param.copy());
            finished = param.step();
        } while (!finished);
        elements.addAll(providerList);
        System.out.println("steps: " + providerList.size());

        ExecutorService tpe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
            var shuffled = new ArrayList<>(symbols);
            Collections.shuffle(shuffled);
            futures.add(CompletableFuture.runAsync(() -> process(shuffled, elements), tpe));
        }
        futures.stream().map(a -> a.join()).collect(Collectors.toList());

        TestParameterProvider resultProvider = TestParameterProvider.createFromList(providerList);

        resultProvider.printResult();

        tpe.shutdownNow();
        long end = System.currentTimeMillis();
        System.out.println("Took " + ((end - start) / 1000.0));
    }

    private void process(List<String> symbols, Queue<TestParameterProvider> providerQueue) {
        while (true) {
            TestParameterProvider param = providerQueue.poll();
            if (param == null) {
                return;
            }
            if (providerQueue.size() % 10 == 0) {
                System.out.println(providerQueue.size());
            }
            double growthSum = 0.0;
            double benchmarkSum = 0.0;
            int count = 0;
            int beats = 0;
            int numberOfQInvested = 0;

            for (int index = 5; index <= 25 * 4; ++index) {
                double yearsAgo = (index / 4.0);
                double yearGrowth = 0.0;
                double yearBenchmarkGrowth = 0.0;
                for (var symbol : symbols) {
                    CompanyFinancials company = readFinancials(symbol);
                    var financials = company.financials;

                    if (financials.isEmpty() || financials.size() <= index) {
                        continue;
                    }
                    double latestPriceThen = financials.get(index).price;

                    Optional<Double> tenYearAvgGrowth = getGrowthInInterval(financials, 8 + yearsAgo, yearsAgo);
                    boolean continouslyProfitable = isProfitableEveryYearSince(financials, 8 + yearsAgo, yearsAgo);
                    double altmanZ = AltmanZCalculator.calculateAltmanZScore(financials.get(index), latestPriceThen);

                    if (tenYearAvgGrowth.isPresent() && continouslyProfitable) {
                        Optional<Double> trailingPeg = TrailingPegCalculator.calculateTrailingPeg(company, index);
                        Optional<Double> trailingPeg2 = TrailingPegCalculator.calculateTrailingPeg(company, index + 1);
                        Optional<Double> trailingPeg3 = TrailingPegCalculator.calculateTrailingPeg(company, index + 2);

                        Optional<Double> epsDeviation = GrowthStandardDeviationCounter.calculateEpsGrowthDeviation(company.financials, yearsAgo, 8);
                        Optional<Double> revenueDeviation = GrowthStandardDeviationCounter.calculateRevenueGrowthDeviation(company.financials, yearsAgo, 8);
                        Optional<Double> fcfDeviation = GrowthStandardDeviationCounter.calculateFcfGrowthDeviation(company.financials, yearsAgo, 8);
                        if (epsDeviation.isPresent() && revenueDeviation.isPresent() && fcfDeviation.isPresent()) {
                            double growth = tenYearAvgGrowth.get();
                            Double epsStandardDeviation = epsDeviation.get();
                            Double pegCutoff = 1.1;
                            if (growth >= 10.0 && epsStandardDeviation < param.getValue("epsSd") &&
                                    revenueDeviation.get() < param.getValue("revSd") &&
                                    revenueDeviation.get() < param.getValue("fcfSd") &&
                                    altmanZ > 2.2 &&
                                    trailingPeg.orElse(0.0) < pegCutoff && trailingPeg2.orElse(0.0) < pegCutoff && trailingPeg3.orElse(0.0) < pegCutoff) {
                                double sellPrice = company.latestPrice;
                                double growthRatio = sellPrice / latestPriceThen;
                                double benchmarkIncrease = INVEST_PER_STOCK
                                        * (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(index).getDate()));
                                double valueGrowth = growthRatio * INVEST_PER_STOCK;

                                growthSum += valueGrowth;
                                benchmarkSum += benchmarkIncrease;
                                yearGrowth += valueGrowth;
                                yearBenchmarkGrowth += benchmarkIncrease;
                                ++count;

                                //                            System.out.printf("%s\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f%% (%.1f -> %.1f)\n", symbol, growth, currentPe, epsStandardDeviation, revenueDeviation.orElse(NaN),
                                //                                    fcfDeviation.orElse(NaN), ((growthRatio - 1.0) * 100.0), latestPriceThen, sellPrice);

                            }
                        }
                    }
                }
                if (yearGrowth > yearBenchmarkGrowth) {
                    ++beats;
                }
                if (yearBenchmarkGrowth > 0) {
                    ++numberOfQInvested;
                }
            }
            int invested = (int) (count * INVEST_PER_STOCK);
            double annual = (Math.pow(growthSum / invested, (1.0 / 20.0)) - 1.0) * 100.0;
            if (Double.isFinite(annual)) {
                double benchmarkResultSumPerc = (Math.pow((benchmarkSum / invested), 1.0 / 20.0) - 1.0) * 100.0;
                param.addResult(annual, growthSum, benchmarkResultSumPerc, benchmarkSum, ((double) beats / numberOfQInvested) * 100.0);
            }
        }
    }

}
