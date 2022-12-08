package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;

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
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;
import com.helospark.financialdata.service.TrailingPegCalculator;
import com.helospark.financialdata.util.analyzer.parameter.IncrementStepStrategy;
import com.helospark.financialdata.util.analyzer.parameter.Parameter;
import com.helospark.financialdata.util.analyzer.parameter.TestParameterProvider;

public class HighRoicScreenerBacktest2 {
    private static final double INVEST_PER_STOCK = 1000.0;

    public void analyze(Set<String> symbols) {
        long start = System.currentTimeMillis();
        TestParameterProvider param = new TestParameterProvider();
        param.registerParameter(new Parameter("roic", 0.05, new IncrementStepStrategy(0.05, 0.25, 0.01)));
        param.registerParameter(new Parameter("peg", 0.8, new IncrementStepStrategy(0.8, 2.0, 0.1)));
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

    public void process(List<String> symbols, Queue<TestParameterProvider> providerQueue) {
        while (true) {
            TestParameterProvider param = providerQueue.poll();
            if (param == null) {
                return;
            }
            if (providerQueue.size() % 10 == 0) {
                System.out.println(providerQueue.size());
            }
            double roicLimit = param.getValue("roic");
            double pegLimit = param.getValue("peg");

            double growthSum = 0.0;
            double benchmarkSum = 0.0;
            int count = 0;
            int beats = 0;
            int numberOfQInvested = 0;
            for (int index = 5; index <= 25 * 4; ++index) {
                double yearsAgo = (index / 4.0);
                double yearGrowthSum = 0.0;
                double yearBenchmarkSum = 0.0;
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

                        if (roic.isPresent() && roic.get() > roicLimit && trailingPeg.isPresent() && trailingPeg.get() < pegLimit) {
                            double sellPrice = company.latestPrice;
                            double growthRatio = sellPrice / latestPriceThen;
                            double benchmarkIncrease = (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(index).getDate()));

                            yearGrowthSum += (growthRatio * INVEST_PER_STOCK);
                            yearBenchmarkSum += benchmarkIncrease * INVEST_PER_STOCK;
                            growthSum += (growthRatio * INVEST_PER_STOCK);
                            benchmarkSum += benchmarkIncrease * INVEST_PER_STOCK;
                            ++count;
                        }
                    }
                }
                if (yearGrowthSum > yearBenchmarkSum) {
                    ++beats;
                }
                if (yearBenchmarkSum > 0) {
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
