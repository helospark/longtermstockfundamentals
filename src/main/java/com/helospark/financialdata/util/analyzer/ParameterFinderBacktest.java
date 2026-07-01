package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.helospark.financialdata.management.screener.ScreenerController;
import com.helospark.financialdata.management.screener.ScreenerOperation;
import com.helospark.financialdata.management.screener.ScreenerOperation.AtGlanceField;
import com.helospark.financialdata.management.screener.ScreenerRequest;
import com.helospark.financialdata.management.screener.domain.BacktestRequest;
import com.helospark.financialdata.management.screener.domain.BacktestResult;
import com.helospark.financialdata.management.screener.domain.ScreenerResult;
import com.helospark.financialdata.management.screener.strategy.ContainsStrategy;
import com.helospark.financialdata.management.screener.strategy.GreaterThanStrategy;
import com.helospark.financialdata.management.screener.strategy.LessThanStrategy;
import com.helospark.financialdata.management.screener.strategy.NotStrategy;
import com.helospark.financialdata.management.screener.strategy.ScreenerColumnListProvider;
import com.helospark.financialdata.management.screener.strategy.ScreenerStrategy;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;

public class ParameterFinderBacktest {
    //    private static final List<String> EXCHANGES = List.of("NASDAQ", "NYSE", "TSX", "STO");
    private static final List<String> EXCHANGES = List.of("NASDAQ", "NYSE");
    private static final double MINIMUM_MARKET_CAP = 100.0;

    private static final YearIntervalGeneratorStrategy INTERVAL_GENERATOR_STRATEGY = new IntervalBasedRandomYearGeneratorStrategy(new YearRange(2013, 2014), new YearRange(2022, 2023));

    private static final double MINIMUM_BEAT_PERCENT = 95.0;
    private static final double MINIMUM_INVEST_COUNT_PERCENT = 85.0;

    private static final double MINIMUM_TRANSACTION_COUNT_AVG_QUARTER = 4;
    private static final double MAXIMUM_TRANSACTION_COUNT_AVG_QUARTER = 40;

    private static final int MIN_PARAMS = 4;
    private static final int MAX_PARAMS = 10;

    private static final int RESULT_QUEUE_SIZE = 60;

    // Test what if this program was run on date, or null for latest date
    private static final LocalDate TEST_RUN_ON_DATE = LocalDate.of(INTERVAL_GENERATOR_STRATEGY.getYearRange().end, 1, 1); // or null

    private static final List<String> EXCLUDED_STOCKS = List.of();
    private static final List<RandomParam> PARAMS = getAllParams();
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
        screenerController.setBacktestMultiMonth(true);

        int numThreads = Math.max(Runtime.getRuntime().availableProcessors() - 2, 1);
        ExecutorService tpe = Executors.newFixedThreadPool(numThreads);

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; ++i) {
            var shuffled = new ArrayList<>(symbols);
            Collections.shuffle(shuffled);
            int threadIndex = i;
            futures.add(CompletableFuture.runAsync(() -> process(shuffled, cloneParams(PARAMS), threadIndex, numThreads), tpe));
        }
        futures.stream().map(a -> a.join()).collect(Collectors.toList());

        tpe.shutdownNow();
    }

    public static List<RandomParam> getAllParams() {
        List<ScreenerStrategy> gtList = List.of(new GreaterThanStrategy());
        List<ScreenerStrategy> ltList = List.of(new LessThanStrategy());
        List<RandomParam> params = new ArrayList<>();
        params.add(new DoubleRandomParam("altman", 1.0, 10.0, gtList));
        params.add(new DoubleRandomParam("pietrosky", 1.0, 8.0, gtList));
        params.add(new DoubleRandomParam("investmentScore", 0.0, 10.0));

        params.add(new DoubleRandomParam("roic", 8, 60, gtList));
        params.add(new DoubleRandomParam("fiveYrRoic", 0, 20, gtList));
        params.add(new DoubleRandomParam("roe", 1, 50));
        params.add(new DoubleRandomParam("roa", 1, 50));
        params.add(new DoubleRandomParam("rota", 1, 50));
        params.add(new DoubleRandomParam("icr", 1, 30, gtList));

        params.add(new DoubleRandomParam("trailingPeg", 0.1, 2.5));
        params.add(new DoubleRandomParam("cape", 3.0, 80.0));
        params.add(new DoubleRandomParam("fYrPe", 0.0, 50.0));
        params.add(new DoubleRandomParam("fYrPFcf", 0.0, 50));

        params.add(new DoubleRandomParam("pe", 0.0, 50));
        params.add(new DoubleRandomParam("peExRnd", 0.0, 50));
        params.add(new DoubleRandomParam("peExMnS", 0.0, 50));
        params.add(new DoubleRandomParam("epsGrExRnd", 0.0, 50));
        params.add(new DoubleRandomParam("epsGrExMnS", 0.0, 50));

        params.add(new DoubleRandomParam("epsGrowth", -10, 100));
        params.add(new DoubleRandomParam("fcfGrowth", -10, 100));
        params.add(new DoubleRandomParam("revenueGrowth", -10, 100));
        params.add(new DoubleRandomParam("shareCountGrowth", -10, 10, ltList));
        params.add(new DoubleRandomParam("netMarginGrowth", -5, 5));
        params.add(new DoubleRandomParam("dividendGrowthRate", -10, 30));
        params.add(new DoubleRandomParam("equityGrowth", -10, 30));

        params.add(new DoubleRandomParam("epsGrowth2yr", -10, 100));
        params.add(new DoubleRandomParam("fcfGrowth2yr", -10, 100));
        params.add(new DoubleRandomParam("revenueGrowth2yr", -10, 100));
        params.add(new DoubleRandomParam("shareCountGrowth2yr", -10, 10, ltList));
        params.add(new DoubleRandomParam("equityGrowth2yr", -10, 30));

        params.add(new DoubleRandomParam("ltl5Fcf", 0.2, 10));
        params.add(new DoubleRandomParam("dtoe", 0.0, 4.0));
        params.add(new DoubleRandomParam("currentRatio", 0.0, 2.0));
        params.add(new DoubleRandomParam("assetTurnoverRatio", 0.0, 1.5));

        params.add(new DoubleRandomParam("opMargin", -10, 40));
        params.add(new DoubleRandomParam("opCMargin", -10, 40));
        params.add(new DoubleRandomParam("fcfMargin", -10, 30));
        params.add(new DoubleRandomParam("ebitdaMargin", -10, 30));

        params.add(new DoubleRandomParam("pts", 0.2, 2));
        params.add(new DoubleRandomParam("ptb", 0.2, 2));
        params.add(new DoubleRandomParam("priceToGrossProfit", 0.2, 2));

        params.add(new DoubleRandomParam("dividendPayoutRatio", 0.0, 120.0));
        params.add(new DoubleRandomParam("profitableYears", 0.0, 12.0));
        params.add(new DoubleRandomParam("stockCompensationPerMkt", 0.0, 3.0, ltList));
        params.add(new DoubleRandomParam("fvCalculatorMoS", -50.0, 1000.0));
        params.add(new DoubleRandomParam("ideal10yrRevCorrelation", 0.0, 1.0));

        params.add(new DoubleRandomParam("price10Gr", -20.0, 30.0));
        params.add(new DoubleRandomParam("price5Gr", -20.0, 30.0));

        params.add(new DoubleRandomParam("fcf_yield", 0.0, 30.0));
        params.add(new DoubleRandomParam("starFlags", 0.0, 10.0, gtList));
        params.add(new DoubleRandomParam("greenFlags", 0.0, 10.0, gtList));
        params.add(new DoubleRandomParam("yellowFlags", 0.0, 10.0));
        params.add(new DoubleRandomParam("redFlags", 0.0, 10.0, ltList));
        params.add(new DoubleRandomParam("revSD", 0.0, 1.0));
        params.add(new DoubleRandomParam("epsSD", 0.0, 1.0));
        params.add(new DoubleRandomParam("tpr", -0.5, 1.2));
        params.add(new DoubleRandomParam("dividendYield", 0.0, 10.0));
        params.add(new DoubleRandomParam("marketCapUsd", 400.0, 4_000_000.0, ltList));

        params.add(new DoubleRandomParam("peCheapestYears", 0.0, 15.0));
        params.add(new DoubleRandomParam("pfcfCheapestYears", 0.0, 15.0));
        params.add(new DoubleRandomParam("evRevenueCheapestYears", 0.0, 15.0));
        params.add(new DoubleRandomParam("evFcfCheapestYears", 0.0, 15.0));

        params.add(new DoubleRandomParam("smoothRevenue5yr", 0.0, 100.0, gtList));
        params.add(new DoubleRandomParam("smoothEps5yr", 0.0, 100.0, gtList));
        params.add(new DoubleRandomParam("smoothFcf5yr", 0.0, 100.0, gtList));
        params.add(new DoubleRandomParam("smoothEquity5yr", 0.0, 100.0, gtList));

        params.add(new ListProviderRandomParam("sector", ScreenerColumnListProvider.provideValues().get(ScreenerColumnListProvider.SECTOR_MAPPING), 4));

        return params;
    }

    public static List<RandomParam> getBestParams() {
        List<ScreenerStrategy> gtList = List.of(new GreaterThanStrategy());
        List<ScreenerStrategy> ltList = List.of(new LessThanStrategy());
        List<RandomParam> params = new ArrayList<>();
        params.add(new DoubleRandomParam("altman", 1.0, 10.0, gtList));
        params.add(new DoubleRandomParam("pietrosky", 1.0, 8.0, gtList));
        params.add(new DoubleRandomParam("roic", 8, 60, gtList));
        params.add(new DoubleRandomParam("fiveYrRoic", 0, 20, gtList));
        params.add(new DoubleRandomParam("roe", 1, 30, gtList));
        params.add(new DoubleRandomParam("trailingPeg", 0.1, 4.5));
        params.add(new DoubleRandomParam("fYrPe", 0.0, 50.0, ltList));
        params.add(new DoubleRandomParam("shareCountGrowth", -10, 10, ltList));
        params.add(new DoubleRandomParam("ltl5Fcf", 0.2, 10, ltList));
        params.add(new DoubleRandomParam("dtoe", 0.0, 4.0, ltList));
        params.add(new DoubleRandomParam("opCMargin", -10, 40, gtList));
        params.add(new DoubleRandomParam("dividendPayoutRatio", 0.0, 120.0));
        params.add(new DoubleRandomParam("dividendYield", 0.0, 10.0));
        params.add(new DoubleRandomParam("profitableYears", 0.0, 12.0));
        params.add(new DoubleRandomParam("revenueGrowth", 0.0, 50.0, gtList));
        params.add(new DoubleRandomParam("investmentScore", 0.0, 10.0));
        params.add(new DoubleRandomParam("pfcfCheapestYears", 0.0, 15.0));
        params.add(new DoubleRandomParam("smoothEps5yr", 0.0, 100.0, gtList));
        params.add(new ListProviderRandomParam("sector", ScreenerColumnListProvider.provideValues().get(ScreenerColumnListProvider.SECTOR_MAPPING), 4, List.of(new ContainsStrategy())));
        return params;
    }

    private List<RandomParam> cloneParams(List<RandomParam> params) {
        return params.stream()
                .map(a -> a.copy())
                .collect(Collectors.toList());
    }

    private void process(List<String> symbols, List<RandomParam> params, int threadIndex, int numThreads) {
        Random random = new Random();
        int i = 0;
        long startTime = System.currentTimeMillis();
        long count = 0;
        ScreenerStrategy greaterThan = new GreaterThanStrategy();
        Set<TestResult> previousSet = Set.of();
        while (true) {
            YearRange yearRange = INTERVAL_GENERATOR_STRATEGY.getYearRange();
            int startYear = yearRange.start;
            int endYear = yearRange.end;

            BacktestRequest request = new BacktestRequest();
            request.endYear = endYear;
            request.startYear = startYear;
            request.exchanges = EXCHANGES;
            request.randomizeOrSort = false;
            request.operations = new ArrayList<>();
            request.operations.add(createOperationsWithFixParam("marketCapUsd", greaterThan, MINIMUM_MARKET_CAP));

            List<Integer> randomIndices = IntStream.range(0, params.size()).mapToObj(a -> a).collect(Collectors.toList());
            Collections.shuffle(randomIndices);
            int startIndex = params.size() > MIN_PARAMS ? MIN_PARAMS : 1;
            int endIndex = params.size() > MAX_PARAMS ? MAX_PARAMS : params.size();

            for (int j = 0; j < random.nextInt(startIndex, endIndex); ++j) {
                var param = params.get(randomIndices.get(j));
                var strategyToUse = param.getOp();
                request.operations.add(createOperations(param, param.getName(), strategyToUse));
            }
            request.addResultTable = false;
            request.excludedStocks = EXCLUDED_STOCKS;

            BacktestResult result = screenerController.performBacktestInternal(request);
            int numberOfQuarters = (endYear - startYear - 1) * 4;
            int minimumInvestCount = (int) (numberOfQuarters * (MINIMUM_INVEST_COUNT_PERCENT / 100.0));
            int minTransactionCount = (int) (MINIMUM_TRANSACTION_COUNT_AVG_QUARTER * numberOfQuarters);
            int maxTransactionCount = (int) (MAXIMUM_TRANSACTION_COUNT_AVG_QUARTER * numberOfQuarters);

            if (result.investedAmount > (minTransactionCount * 1000)
                    && result.investedAmount < (maxTransactionCount * 1000)
                    && result.screenerWithDividendsAvgPercent > 10.0
                    && result.investedCount > minimumInvestCount
                    && result.beatPercent >= MINIMUM_BEAT_PERCENT) {
                resultSet.add(new TestResult(result.screenerWithDividendsAvgPercent, result.investedAmount, result.screenerWithDividendsMedianPercent, result.beatCount, result.investedCount,
                        request.operations, startYear, endYear));
                while (resultSet.size() > RESULT_QUEUE_SIZE) {
                    TestResult elementToRemove = resultSet.iterator().next();
                    resultSet.remove(elementToRemove);
                }
            }

            if (threadIndex == 0 && i % 500 == 0 && !resultSet.equals(previousSet)) {
                previousSet = new TreeSet<>(resultSet);
                printMostCommonStocks(previousSet);
                printMostCommonOperands(previousSet);
                for (var element : previousSet) {
                    System.out.println(element);
                }
                System.out.println("Throughput: " + ((count / ((System.currentTimeMillis() - startTime) / 1000.0)) * numThreads) + " op/s");

                startTime = System.currentTimeMillis();
                count = 0;
                System.out.println("----------------------");
            }
            ++i;
            ++count;
        }
    }

    private void printMostCommonStocks(Set<TestResult> previousSet) {
        Map<String, Integer> stockToCount = new HashMap<>();
        for (var entry : previousSet) {
            boolean hasMorePage = true;
            String lastItem = null;
            while (hasMorePage) {
                ScreenerRequest request = new ScreenerRequest();
                request.exchanges = EXCHANGES;
                request.operations = entry.screenerOperations;
                request.lastItem = lastItem;
                request.onDate = TEST_RUN_ON_DATE;

                ScreenerResult result = screenerController.screenStockInternal(request);

                for (var stockList : result.portfolio) {
                    String symbol = stockList.get("Symbol").replaceAll("<.*?>", "");
                    Integer count = stockToCount.get(symbol);
                    if (count == null) {
                        count = 1;
                    } else {
                        count += 1;
                    }
                    stockToCount.put(symbol, count);
                    lastItem = symbol;
                }
                hasMorePage = result.hasMoreResults;
            }
        }
        List<Map.Entry<String, Integer>> stockToCountList = new ArrayList<>(stockToCount.entrySet());
        Collections.sort(stockToCountList, (a, b) -> b.getValue().compareTo(a.getValue()));

        System.out.print("[ ");
        for (int i = 0; i < 400 && i < stockToCountList.size(); ++i) {
            var entry = stockToCountList.get(i);
            System.out.print(entry.getKey() + "(" + entry.getValue() + "), ");
        }
        System.out.println("]");
        System.out.println();
    }

    private void printMostCommonOperands(Set<TestResult> previousSet) {
        Map<String, Integer> opToCount = new HashMap<>();
        for (var entry : previousSet) {
            for (var op : entry.screenerOperations) {
                String opName = op.id + op.operation;
                Integer count = opToCount.get(opName);
                if (count == null) {
                    count = 1;
                } else {
                    count += 1;
                }
                opToCount.put(opName, count);
            }
        }

        List<Map.Entry<String, Integer>> opToCountList = new ArrayList<>(opToCount.entrySet());
        Collections.sort(opToCountList, (a, b) -> b.getValue().compareTo(a.getValue()));

        System.out.print("[ ");
        for (int i = 0; i < 30 && i < opToCountList.size(); ++i) {
            var entry = opToCountList.get(i);
            System.out.print(entry.getKey() + "(" + entry.getValue() + "), ");
        }
        System.out.println("]");
        System.out.println();
    }

    public ScreenerOperation createOperations(RandomParam param, String name, ScreenerStrategy strategyToUse) {
        ScreenerOperation op = new ScreenerOperation();
        op.id = AtGlanceField.fromString(name);
        op.number1 = param.getNumber1();
        op.numberList = param.getNumberList();
        op.operation = strategyToUse.getSymbol();
        op.screenerStrategy = strategyToUse;
        return op;
    }

    public ScreenerOperation createOperationsWithFixParam(String name, ScreenerStrategy operationStrategy, double value) {
        ScreenerOperation op = new ScreenerOperation();
        op.id = AtGlanceField.fromString(name);
        op.number1 = value;
        op.numberList = null;
        op.operation = operationStrategy.getSymbol();
        op.screenerStrategy = operationStrategy;
        return op;
    }

    static class TestResult implements Comparable<TestResult> {
        double avgPercent;
        double invested;
        double medianPercent;
        int beatCount;
        int investedCount;
        List<ScreenerOperation> screenerOperations;
        int startYear;
        int endYear;

        public TestResult(double avgPercent, double invested, double medianPercent, int beatCount, int investedCount, List<ScreenerOperation> screenerOperations, int startYear,
                int endYear) {
            this.avgPercent = avgPercent;
            this.invested = invested;
            this.medianPercent = medianPercent;
            this.screenerOperations = screenerOperations;
            this.beatCount = beatCount;
            this.investedCount = investedCount;
            this.startYear = startYear;
            this.endYear = endYear;
        }

        @Override
        public int compareTo(TestResult o) {
            double thisPercent = (avgPercent + medianPercent) / 2.0;
            double otherPercent = (o.avgPercent + o.medianPercent) / 2.0;
            return Double.compare(thisPercent, otherPercent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(avgPercent, beatCount, invested, medianPercent);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TestResult other = (TestResult) obj;
            return Double.doubleToLongBits(avgPercent) == Double.doubleToLongBits(other.avgPercent) && beatCount == other.beatCount
                    && Double.doubleToLongBits(invested) == Double.doubleToLongBits(other.invested) && Double.doubleToLongBits(medianPercent) == Double.doubleToLongBits(other.medianPercent);
        }

        @Override
        public String toString() {
            return "avgPercent=" + avgPercent + ", medianPercent=" + medianPercent + ", invested=" + invested + ", beat=" + beatCount + " / " + investedCount + ", " + startYear + "->" + endYear
                    + "\nscreenerOperations="
                    + screenerOperations + "\n\n";
        }

    }

    static interface RandomParam {

        public double getNumber1();

        public int[] getNumberList();

        public RandomParam copy();

        public ScreenerStrategy getOp();

        public String getName();
    }

    static class DoubleRandomParam implements RandomParam {
        Random random = new Random();
        String name;
        double min;
        double max;
        List<ScreenerStrategy> ops = List.of(new LessThanStrategy(), new GreaterThanStrategy());

        public DoubleRandomParam(String name, double min, double max) {
            this.min = min;
            this.max = max;
            this.name = name;
        }

        public DoubleRandomParam(String name, double min, double max, List<ScreenerStrategy> ops) {
            this.min = min;
            this.max = max;
            this.ops = ops;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public double getNumber1() {
            return random.nextDouble(min, max);
        }

        @Override
        public int[] getNumberList() {
            return null;
        }

        @Override
        public ScreenerStrategy getOp() {
            return ops.get(random.nextInt(ops.size()));
        }

        @Override
        public RandomParam copy() {
            return new DoubleRandomParam(name, min, max, ops);
        }
    }

    static class ListProviderRandomParam implements RandomParam {
        Random random = new Random();
        String name;
        int maxElements;
        Map<String, Integer> listProvider;
        List<Integer> allowedValues;
        List<ScreenerStrategy> ops = List.of(new ContainsStrategy(), new NotStrategy());

        public ListProviderRandomParam(String name, Map<String, Integer> listProvider, int maxElement) {
            this.allowedValues = new ArrayList<>(listProvider.values());
            this.listProvider = listProvider;
            this.maxElements = maxElement;
            this.name = name;
        }

        public ListProviderRandomParam(String name, Map<String, Integer> listProvider, int maxElement, List<ScreenerStrategy> ops) {
            this.allowedValues = new ArrayList<>(listProvider.values());
            this.maxElements = maxElement;
            this.listProvider = listProvider;
            this.ops = ops;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public double getNumber1() {
            return 0.0; // ignored
        }

        @Override
        public int[] getNumberList() {
            if (allowedValues == null || allowedValues.isEmpty()) {
                return new int[0];
            }

            int totalSize = allowedValues.size();

            int n = 1 + random.nextInt(Math.min(maxElements, totalSize));

            if (n >= totalSize) {
                return allowedValues.stream().mapToInt(Integer::intValue).toArray();
            }

            List<Integer> pool = new ArrayList<>(allowedValues);
            int[] result = new int[n];

            // Partial Fisher-Yates Shuffle: Only shuffle the 'n' elements we need
            for (int i = 0; i < n; i++) {
                int randomIndex = i + random.nextInt(totalSize - i);

                Collections.swap(pool, i, randomIndex);

                result[i] = pool.get(i);
            }

            return result;
        }

        @Override
        public ScreenerStrategy getOp() {
            return ops.get(random.nextInt(ops.size()));
        }

        @Override
        public RandomParam copy() {
            return new ListProviderRandomParam(name, listProvider, maxElements, ops);
        }
    }

    static class YearRange {
        int start;
        int end;

        public YearRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

    }

    static interface YearIntervalGeneratorStrategy {
        public YearRange getYearRange();
    }

    static class IntervalBasedRandomYearGeneratorStrategy implements YearIntervalGeneratorStrategy {
        YearRange startRange;
        YearRange endRange;

        public IntervalBasedRandomYearGeneratorStrategy(YearRange startRange, YearRange endRange) {
            this.startRange = startRange;
            this.endRange = endRange;
        }

        @Override
        public YearRange getYearRange() {
            Random random = new Random();
            int startYear = random.nextInt(startRange.start, startRange.end);
            int endYear = random.nextInt(endRange.start, endRange.end);

            return new YearRange(startYear, endYear);
        }
    }

    static class YearDifferenceBasedRandomYearGeneratorStrategy implements YearIntervalGeneratorStrategy {
        YearRange yearRange;
        int numYear;

        public YearDifferenceBasedRandomYearGeneratorStrategy(YearRange yearRange, int numYear) {
            this.yearRange = yearRange;
            this.numYear = numYear;
        }

        @Override
        public YearRange getYearRange() {
            Random random = new Random();
            int startYear = random.nextInt(yearRange.start, yearRange.end);
            int endYear = startYear + numYear;

            return new YearRange(startYear, endYear);
        }
    }
}
