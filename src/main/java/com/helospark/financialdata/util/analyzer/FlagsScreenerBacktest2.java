package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.service.FlagsProviderService;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;
import com.helospark.financialdata.util.analyzer.parameter.IncrementStepStrategy;
import com.helospark.financialdata.util.analyzer.parameter.Parameter;
import com.helospark.financialdata.util.analyzer.parameter.TestParameterProvider;

public class FlagsScreenerBacktest2 {
    private static final double INVEST_PER_STOCK = 1000.0;

    public void analyze(Set<String> symbols) {
        long start = System.currentTimeMillis();
        TestParameterProvider param = new TestParameterProvider();
        param.registerParameter(new Parameter("red", 1.0, new IncrementStepStrategy(0, 7.0, 1.0)));
        param.registerParameter(new Parameter("green", 1.0, new IncrementStepStrategy(0, 7.0, 1.0)));
        param.registerParameter(new Parameter("star", 1.0, new IncrementStepStrategy(0, 5.0, 1.0)));
        param.registerParameter(new Parameter("yellow", 1.0, new IncrementStepStrategy(0, 7.0, 1.0)));
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

                    int redLimit = (int) Math.round(param.getValue("red"));
                    int greenLimit = (int) Math.round(param.getValue("green"));
                    int starLimit = (int) Math.round(param.getValue("star"));
                    int yellowLimit = (int) Math.round(param.getValue("yellow"));

                    if (numStar >= starLimit && numGreen >= greenLimit && numYellow <= yellowLimit && numRed <= redLimit) {
                        double sellPrice = company.latestPrice;
                        double growthRatio = sellPrice / latestPriceThen;
                        double benchmarkIncrease = INVEST_PER_STOCK
                                * (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(index).getDate()));
                        double valueGrowth = growthRatio * INVEST_PER_STOCK;

                        if (Double.isFinite(valueGrowth)) {
                            growthSum += valueGrowth;
                            benchmarkSum += benchmarkIncrease;
                            yearGrowth += valueGrowth;
                            yearBenchmarkGrowth += benchmarkIncrease;
                            ++count;
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
