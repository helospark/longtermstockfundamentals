package com.helospark.financialdata.util.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.helospark.financialdata.management.screener.ScreenerController;
import com.helospark.financialdata.management.screener.ScreenerOperation;
import com.helospark.financialdata.management.screener.domain.BacktestRequest;
import com.helospark.financialdata.management.screener.domain.BacktestResult;
import com.helospark.financialdata.management.screener.strategy.GreaterThanStrategy;
import com.helospark.financialdata.management.screener.strategy.LessThanStrategy;
import com.helospark.financialdata.management.screener.strategy.ScreenerStrategy;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;

public class ParameterFinderBacktest {
    ScreenerController screenerController;
    Set<TestResult> resultSet = Collections.synchronizedSet(new TreeSet<>());

    public static void main(String[] args) {
        ParameterFinderBacktest thisInstance = new ParameterFinderBacktest();
        Set<String> usSymbols = DataLoader.provideSymbolsFromNasdaqNyse();

        thisInstance.analyze(usSymbols);
    }

    public void analyze(Set<String> symbols) {
        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();

        List<ScreenerStrategy> screenerStrategies = List.of(new GreaterThanStrategy(), new LessThanStrategy());
        screenerController = new ScreenerController(symbolAtGlanceProvider, screenerStrategies, null);

        List<ScreenerStrategy> gtList = List.of(new GreaterThanStrategy());
        List<ScreenerStrategy> ltList = List.of(new LessThanStrategy());
        List<RandomParam> params = new ArrayList<>();
        params.add(new RandomParam("altman", 1.0, 10.0, gtList));
        params.add(new RandomParam("roic", 8, 60, gtList));
        params.add(new RandomParam("fiveYrRoic", 0, 20, gtList));
        params.add(new RandomParam("roe", 1, 30, gtList));
        params.add(new RandomParam("trailingPeg", 0.1, 2.5));
        params.add(new RandomParam("cape", 3.0, 80.0));
        params.add(new RandomParam("fYrPe", 0.0, 50.0));
        params.add(new RandomParam("epsGrowth", -10, 50));
        params.add(new RandomParam("fcfGrowth", -10, 50));
        params.add(new RandomParam("revenueGrowth", -10, 50));
        params.add(new RandomParam("shareCountGrowth", -10, 10, ltList));
        params.add(new RandomParam("ltl5Fcf", 0.2, 10));
        params.add(new RandomParam("dtoe", 0.0, 4.0));
        params.add(new RandomParam("opMargin", -10, 40));
        params.add(new RandomParam("pts", 0.2, 2));
        params.add(new RandomParam("ptb", 0.2, 2));
        params.add(new RandomParam("dividendPayoutRatio", 0.0, 120.0));
        params.add(new RandomParam("profitableYears", 0.0, 12.0));
        params.add(new RandomParam("stockCompensationPerMkt", 0.0, 3.0, ltList));
        params.add(new RandomParam("fvCalculatorMoS", -50.0, 1000.0));
        /*
        params.add(new RandomParam("roic", 8, 60));
        params.add(new RandomParam("fiveYrRoic", 0, 20));
        params.add(new RandomParam("trailingPeg", 0.1, 2.5));
        params.add(new RandomParam("shareCountGrowth", -10, 10));
        params.add(new RandomParam("profitableYears", 0.0, 12.0));
        params.add(new RandomParam("opMargin", -10, 40));
        params.add(new RandomParam("ltl5Fcf", 0.2, 10));*/

        ExecutorService tpe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
            var shuffled = new ArrayList<>(symbols);
            Collections.shuffle(shuffled);
            int threadIndex = i;
            futures.add(CompletableFuture.runAsync(() -> process(shuffled, cloneParams(params), threadIndex), tpe));
        }
        futures.stream().map(a -> a.join()).collect(Collectors.toList());

        tpe.shutdownNow();
    }

    private List<RandomParam> cloneParams(List<RandomParam> params) {
        return params.stream()
                .map(a -> a.copy())
                .collect(Collectors.toList());
    }

    private void process(List<String> symbols, List<RandomParam> params, int threadIndex) {
        Random random = new Random();
        int i = 0;
        long startTime = System.currentTimeMillis();
        long count = 0;
        ScreenerStrategy greaterThan = new GreaterThanStrategy();
        while (true) {
            BacktestRequest request = new BacktestRequest();
            request.endYear = 2022;
            request.startYear = 1996;
            request.exchanges = List.of("NASDAQ", "NYSE");
            request.operations = new ArrayList<>();
            request.operations.add(createOperationsWithFixParam("marketCapUsd", greaterThan, 300.0));
            for (var param : params) {
                if (random.nextDouble() > 0.5) {
                    var strategyToUse = param.getOp();
                    request.operations.add(createOperations(param, param.name, strategyToUse));
                }
            }
            if (request.operations.size() < 4) {
                continue;
            }

            BacktestResult result = screenerController.performBacktestInternal(request);

            if (result.investedAmount > 100_000 && result.screenerWithDividendsAvgPercent > 10.0) {
                resultSet.add(new TestResult(result.screenerWithDividendsAvgPercent, result.investedAmount, result.screenerWithDividendsMedianPercent, request.operations));
                while (resultSet.size() > 40) {
                    TestResult elementToRemove = resultSet.iterator().next();
                    resultSet.remove(elementToRemove);
                }
            }

            if (threadIndex == 0 && i % 500 == 0) {
                Set<TestResult> newSet = new TreeSet<>(resultSet);
                for (var element : newSet) {
                    System.out.println(element);
                }
                System.out.println("Throughput: " + (count / ((System.currentTimeMillis() - startTime) / 1000.0)) + " op/s");

                startTime = System.currentTimeMillis();
                count = 0;
                System.out.println("----------------------");
            }
            ++i;
            ++count;
        }
    }

    public ScreenerOperation createOperations(RandomParam param, String name, ScreenerStrategy strategyToUse) {
        ScreenerOperation op = new ScreenerOperation();
        op.id = name;
        op.number1 = param.getValue();
        op.operation = strategyToUse.getSymbol();
        op.screenerStrategy = strategyToUse;
        return op;
    }

    public ScreenerOperation createOperationsWithFixParam(String name, ScreenerStrategy operationStrategy, double value) {
        ScreenerOperation op = new ScreenerOperation();
        op.id = name;
        op.number1 = value;
        op.operation = operationStrategy.getSymbol();
        op.screenerStrategy = operationStrategy;
        return op;
    }

    static class TestResult implements Comparable<TestResult> {
        double avgPercent;
        double invested;
        double medianPercent;
        List<ScreenerOperation> screenerOperations;

        public TestResult(double avgPercent, double invested, double medianPercent, List<ScreenerOperation> screenerOperations) {
            this.avgPercent = avgPercent;
            this.invested = invested;
            this.medianPercent = medianPercent;
            this.screenerOperations = screenerOperations;
        }

        @Override
        public int compareTo(TestResult o) {
            double thisPercent = (avgPercent + medianPercent) / 2.0;
            double otherPercent = (o.avgPercent + o.medianPercent) / 2.0;
            return Double.compare(thisPercent, otherPercent);
        }

        @Override
        public String toString() {
            return "avgPercent=" + avgPercent + ", invested=" + invested + ", medianPercent=" + medianPercent + ", screenerOperations=" + screenerOperations;
        }

    }

    static class RandomParam {
        Random random = new Random();
        String name;
        double min;
        double max;
        List<ScreenerStrategy> ops = List.of(new LessThanStrategy(), new GreaterThanStrategy());

        public RandomParam(String name, double min, double max) {
            this.min = min;
            this.max = max;
            this.name = name;
        }

        public RandomParam(String name, double min, double max, List<ScreenerStrategy> ops) {
            this.min = min;
            this.max = max;
            this.ops = ops;
            this.name = name;
        }

        public double getValue() {
            return random.nextDouble(min, max);
        }

        public ScreenerStrategy getOp() {
            return ops.get(random.nextInt(ops.size()));
        }

        public RandomParam copy() {
            return new RandomParam(name, min, max, ops);
        }
    }

}
