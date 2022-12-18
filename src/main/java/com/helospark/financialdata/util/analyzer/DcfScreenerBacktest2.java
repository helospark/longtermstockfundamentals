package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.AltmanZCalculator.calculateAltmanZScore;
import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.DcfCalculator.doStockDcfAnalysis;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthAnalyzer.isStableGrowth;
import static com.helospark.financialdata.service.GrowthCalculator.getEpsGrowthInInterval;
import static com.helospark.financialdata.service.Helpers.min;

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
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;
import com.helospark.financialdata.util.analyzer.parameter.IncrementStepStrategy;
import com.helospark.financialdata.util.analyzer.parameter.Parameter;
import com.helospark.financialdata.util.analyzer.parameter.TestParameterProvider;

public class DcfScreenerBacktest2 implements StockScreeners {
    private static final double INVEST_PER_STOCK = 1000.0;

    @Override
    public void analyze(Set<String> symbols) {
        TestParameterProvider param = new TestParameterProvider();
        param.registerParameter(new Parameter("upside", 5.0, new IncrementStepStrategy(5.0, 100.0, 5.0)));
        param.registerParameter(new Parameter("year", 3.0, new IncrementStepStrategy(3.0, 10.0, 1.0)));
        param.registerParameter(new Parameter("alt", 3.0, new IncrementStepStrategy(3.0, 3.1, 1.0)));
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
    }

    private void process(List<String> symbols, Queue<TestParameterProvider> providerQueue) {
        while (true) {
            TestParameterProvider param = providerQueue.poll();
            if (param == null) {
                return;
            }
            if (providerQueue.size() % 100 == 0) {
                System.out.println(providerQueue.size());
            }
            double growthSum = 0.0;
            double benchmarkSum = 0.0;
            int count = 0;
            int beats = 0;
            for (int index = 5; index <= 21 * 4; ++index) {
                double yearsAgo = index / 4.0;
                double yearGrowth = 0.0;
                double yearBenchmarkGrowth = 0.0;
                for (var symbol : symbols) {
                    CompanyFinancials company = readFinancials(symbol);
                    var financials = company.financials;

                    if (financials.isEmpty()) {
                        continue;
                    }

                    int latestElement = index;
                    //                System.out.println(financials.size() + " > " + (latestElement + 1));
                    if (financials.size() > latestElement + 1) {

                        double yearLimit = param.getValue("year");
                        Optional<Double> tenYearAvgGrowth = getEpsGrowthInInterval(financials, yearLimit + yearsAgo, yearsAgo);

                        //                    System.out.println(tenYearAvgGrowth + " " + eightYearAvgGrowth + " " + fiveYearAvgGrowth + " " + threeYearAvgGrowth);

                        double latestPriceThen = financials.get(latestElement).price;

                        boolean continouslyProfitable = isProfitableEveryYearSince(financials, yearLimit + yearsAgo, yearsAgo);
                        boolean stableGrowth = isStableGrowth(financials, yearLimit - 2 + yearsAgo, yearsAgo);
                        double altmanZ = financials.size() > latestElement ? calculateAltmanZScore(financials.get(latestElement), latestPriceThen) : 0.0;

                        //                    System.out.println(latestPriceThen + " " + continouslyProfitable + " " + stableGrowth + " " + altmanZ);

                        if (tenYearAvgGrowth.isPresent() &&
                                stableGrowth &&
                                continouslyProfitable &&
                                altmanZ > param.getValue("alt") &&
                                financials.get(latestElement).incomeStatementTtm.eps > 0.0 &&
                                tenYearAvgGrowth.get() > 0.0) {
                            double tenYearGrowth = tenYearAvgGrowth.get();

                            double dcf = doStockDcfAnalysis(financials.get(latestElement).incomeStatementTtm.eps, min(tenYearGrowth));

                            long fcf = financials.get(latestElement).cashFlowTtm.freeCashFlow;
                            double fcfPerShare = (double) fcf / financials.get(latestElement).incomeStatementTtm.weightedAverageShsOut;
                            double dcfFcf = doStockDcfAnalysis(fcfPerShare, min(tenYearGrowth));

                            double upside = (dcf / latestPriceThen - 1.0) * 100;
                            double fcfUpside = (dcfFcf / latestPriceThen - 1.0) * 100;

                            //                        System.out.println(symbol + "\t" + upside + " " + fcfUpside);

                            double upsideLimit = param.getValue("upside");
                            if (upside > upsideLimit && fcfUpside > upsideLimit) {
                                int sellQuarter = -1;
                                double sellPrice = sellQuarter == -1 ? company.latestPrice : financials.get(sellQuarter).price;
                                double growthRatio = sellPrice / latestPriceThen;
                                double benchmarkIncrease = INVEST_PER_STOCK
                                        * (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(latestElement).getDate()));
                                double valueGrowth = growthRatio * INVEST_PER_STOCK;

                                growthSum += valueGrowth;
                                benchmarkSum += benchmarkIncrease;
                                yearGrowth += valueGrowth;
                                yearBenchmarkGrowth += benchmarkIncrease;
                                ++count;
                            }
                        }
                    }
                }
                if (yearGrowth > yearBenchmarkGrowth) {
                    ++beats;
                }
            }
            int invested = (int) (count * INVEST_PER_STOCK);
            double annual = (Math.pow(growthSum / invested, (1.0 / 20.0)) - 1.0) * 100.0;
            if (Double.isFinite(annual)) {
                double benchmarkResultSumPerc = (Math.pow((benchmarkSum / invested), 1.0 / 20.0) - 1.0) * 100.0;
                param.addResult(annual, growthSum, benchmarkResultSumPerc, benchmarkSum, beats);
            }
        }
    }

}
