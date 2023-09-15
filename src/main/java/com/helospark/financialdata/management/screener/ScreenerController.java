package com.helospark.financialdata.management.screener;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.management.config.ratelimit.RateLimit;
import com.helospark.financialdata.management.screener.annotation.ScreenerElement;
import com.helospark.financialdata.management.screener.domain.BacktestRequest;
import com.helospark.financialdata.management.screener.domain.BacktestResult;
import com.helospark.financialdata.management.screener.domain.BacktestYearInformation;
import com.helospark.financialdata.management.screener.domain.GenericErrorResponse;
import com.helospark.financialdata.management.screener.domain.ScreenerDescription;
import com.helospark.financialdata.management.screener.domain.ScreenerDescription.Source;
import com.helospark.financialdata.management.screener.domain.ScreenerResult;
import com.helospark.financialdata.management.screener.strategy.ScreenerStrategy;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.service.exchanges.Exchanges;
import com.helospark.financialdata.util.glance.AtGlanceData;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/screener")
public class ScreenerController {
    private static final String ANNUAL_RETURNS_WITH_DIVIDENDS_REINVESTED = "Annual return with divs reinvested (%)";
    private static final String ANNUAL_RETURN_COLUMN = "Annual return (%)";
    private static final String TOTAL_RETURN_COLUMN = "Total return (%)";
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenerController.class);
    public static final int MAX_RESULTS = 101;
    public static double BACKTEST_INVEST_AMOUNT = 1000.0;
    Map<String, ScreenerDescription> idToDescription = new LinkedHashMap<>();
    private static final Set<String> blacklistedStocks = Set.of(
            "CHSCP", // incorrect dividend information
            "WFC-PL" // incorrect data
    );

    private SymbolAtGlanceProvider symbolAtGlanceProvider;
    private List<ScreenerStrategy> screenerStrategies;
    private LoginController loginController;
    static Cache<LocalDate, Double> spPriceCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(2000)
            .build();
    private boolean isHistoricalFilesInitialized;

    @Value("${backtest.multimonth:false}")
    public boolean backtestMultiMonth;

    public ScreenerController(SymbolAtGlanceProvider symbolAtGlanceProvider, List<ScreenerStrategy> screenerStrategies,
            LoginController loginController) {
        this.symbolAtGlanceProvider = symbolAtGlanceProvider;
        this.screenerStrategies = screenerStrategies;
        this.loginController = loginController;
        for (var field : AtGlanceData.class.getDeclaredFields()) {
            ScreenerElement screenerElement = field.getAnnotation(ScreenerElement.class);
            if (screenerElement != null) {
                field.setAccessible(true); // optimization
                String name = screenerElement.name();
                ScreenerDescription description = new ScreenerDescription();
                description.readableName = name;
                description.format = screenerElement.format();
                description.source = Source.FIELD;
                description.data = field;

                String id = screenerElement.id().equals("") ? field.getName() : screenerElement.id();

                idToDescription.put(id, description);
            }
        }
        for (var method : AtGlanceData.class.getDeclaredMethods()) {
            ScreenerElement screenerElement = method.getAnnotation(ScreenerElement.class);
            if (screenerElement != null && method.getParameterCount() == 0) {
                method.setAccessible(true); // optimization
                String name = screenerElement.name();
                ScreenerDescription description = new ScreenerDescription();
                description.readableName = name;
                description.format = screenerElement.format();
                description.source = Source.METHOD;
                description.data = method;

                String id = screenerElement.id().equals("") ? method.getName() : screenerElement.id();

                idToDescription.put(id, description);
            }
        }
    }

    public Map<String, ScreenerDescription> getScreenerDescriptions() {
        return idToDescription;
    }

    public List<ScreenerOperator> getScreenerOperators() {
        List<ScreenerOperator> result = new ArrayList<ScreenerOperator>();
        for (var element : screenerStrategies) {
            ScreenerOperator operator = new ScreenerOperator();
            operator.symbol = element.getSymbol();
            operator.numberOfArguments = element.getNumberOfArguments();

            result.add(operator);
        }
        return result;
    }

    public List<Integer> getBacktestDates() {
        List<Integer> result = new ArrayList<>();
        for (int i = 1994; i <= LocalDate.now().getYear(); ++i) {
            result.add(i);
        }
        return result;
    }

    @PostMapping("/perform")
    @RateLimit(requestPerMinute = 20)
    public ScreenerResult screenStocks(@RequestBody ScreenerRequest request, HttpServletRequest httpRequest) {
        LOGGER.info("Received screener request '{}'", request);
        validateRequest(request, httpRequest);
        return screenStockInternal(request);
    }

    public ScreenerResult screenStockInternal(ScreenerRequest request) {
        boolean nextPageRequested = request.lastItem != null;
        boolean previousPageRequested = request.prevItem != null;

        LinkedHashMap<String, AtGlanceData> data = symbolAtGlanceProvider.getSymbolCompanyNameCache();

        if (nextPageRequested) {
            LinkedHashMap<String, AtGlanceData> filteredData = new LinkedHashMap<>();
            boolean foundElement = false;
            for (var entry : data.entrySet()) {
                if (foundElement) {
                    filteredData.put(entry.getKey(), entry.getValue());
                }
                if (entry.getKey().equals(request.lastItem)) {
                    foundElement = true;
                }
            }
            data = filteredData;
        }
        if (previousPageRequested) {
            LinkedHashMap<String, AtGlanceData> filteredData = new LinkedHashMap<>();
            boolean foundElement = false;
            List<String> keySet = new ArrayList<>(data.keySet());
            for (int i = keySet.size() - 1; i >= 0; --i) {
                String key = keySet.get(i);
                if (foundElement) {
                    var value = data.get(key);
                    filteredData.put(key, value);
                }
                if (key.equals(request.prevItem)) {
                    foundElement = true;
                }
            }
            data = filteredData;
        }
        List<AtGlanceData> matchedStocks = findMatchingStocks(data, request, getSymbolsInExchanges(request.exchanges), false, List.of());

        if (previousPageRequested) {
            Collections.reverse(matchedStocks);
        }

        ScreenerResult result = new ScreenerResult();
        result.hasMoreResults = (matchedStocks.size() >= MAX_RESULTS) || previousPageRequested;
        result.hasPreviousResults = nextPageRequested || (matchedStocks.size() >= MAX_RESULTS && previousPageRequested);

        if (matchedStocks.size() >= MAX_RESULTS && !previousPageRequested) {
            matchedStocks.remove(matchedStocks.size() - 1);
        }
        if (matchedStocks.size() >= MAX_RESULTS && previousPageRequested) {
            matchedStocks.remove(0);
        }

        result.columns.add("Symbol");
        result.columns.add("Company");

        Map<String, ScreenerOperation> dedupedScreenersMap = new LinkedHashMap<>();

        for (var element : request.operations) {
            if (!dedupedScreenersMap.containsKey(element.id)) {
                dedupedScreenersMap.put(element.id, element);
            }
        }
        var dedupedOperations = dedupedScreenersMap.values();

        for (var element : dedupedOperations) {
            result.columns.add(idToDescription.get(element.id).readableName);
        }

        for (var stock : matchedStocks) {
            if (stock.symbol.equals("FB")) { // ticker changed
                continue;
            }
            Map<String, String> columnResult = new HashMap<>();
            columnResult.put("Symbol", createSymbolLink(stock.symbol));
            columnResult.put("Company", stock.companyName);

            for (var element : dedupedOperations) {
                ScreenerDescription screenerDescription = idToDescription.get(element.id);
                String columnName = screenerDescription.readableName;
                Double value = getValue(stock, element, screenerDescription);
                columnResult.put(columnName, screenerDescription.format.format(value));
            }
            result.portfolio.add(columnResult);
        }

        return result;
    }

    public List<AtGlanceData> findMatchingStocks(Map<String, AtGlanceData> data, ScreenerRequest request, List<String> symbolsInExchanges, boolean randomize, List<String> excludedStocks) {
        List<AtGlanceData> matchedStocks = new ArrayList<>();
        List<String> symbols = symbolsInExchanges;

        if (randomize) {
            Collections.shuffle(symbols);
        } else {
            List<String> newSymbols = new ArrayList<>();
            for (var symbol : symbols) {
                if (data.get(symbol) != null) {
                    newSymbols.add(symbol);
                }
            }
            symbols = newSymbols;
            Collections.sort(symbols, (a, b) -> Double.compare(data.get(b).marketCapUsd, data.get(a).marketCapUsd));
        }

        for (var entry : symbols) {
            var atGlanceData = data.get(entry);
            if (atGlanceData == null || blacklistedStocks.contains(entry) || excludedStocks.contains(entry)) {
                continue;
            }

            boolean allMatch = true;
            for (var operation : request.operations) {
                ScreenerDescription source = idToDescription.get(operation.id);
                double value = getValue(atGlanceData, operation, source);
                ScreenerStrategy screenerStrategy = operation.screenerStrategy;
                if (!screenerStrategy.matches(value, operation)) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                matchedStocks.add(atGlanceData);
                if (matchedStocks.size() >= MAX_RESULTS) {
                    break;
                }
            }
        }
        return matchedStocks;
    }

    @PostMapping("/backtest")
    @RateLimit(requestPerMinute = 20)
    public BacktestResult performBacktest(@RequestBody BacktestRequest request, HttpServletRequest httpRequest) {
        LOGGER.info("Received backtest request '{}'", request);
        Optional<DecodedJWT> jwt = loginController.getJwt(httpRequest);
        if (!jwt.isPresent()) {
            AccountType accountType = loginController.getAccountType(jwt.get());
            if (!(accountType == AccountType.ADVANCED || accountType == AccountType.ADMIN)) {
                throw new ScreenerClientSideException("Backtest is only available for users with 'Advanced' plan");
            }
        }
        validateRequest(request, httpRequest);

        return performBacktestInternal(request);
    }

    public BacktestResult performBacktestInternal(BacktestRequest request) {
        if (request.endYear < request.startYear) {
            throw new ScreenerClientSideException("End date must be greater than start time");
        }
        if (request.endYear - request.startYear < 2) {
            throw new ScreenerClientSideException("More than 2 year difference expected");
        }
        if (request.endYear > LocalDate.now().getYear()) {
            throw new ScreenerClientSideException("Unfortunataly our datasource doesn't provide the future stock prices :(");
        }
        if (request.startYear < 1990) {
            throw new ScreenerClientSideException("We only have data from 1990");
        }

        boolean useLatestData = (request.endYear == LocalDate.now().getYear());

        Map<String, BacktestYearInformation> yearResults = new LinkedHashMap<>();

        Set<String> columns = new LinkedHashSet<>();
        columns.add("Symbol");
        columns.add("Name");
        columns.add("Buy price");
        columns.add("Current price");
        columns.add(TOTAL_RETURN_COLUMN);
        columns.add(ANNUAL_RETURN_COLUMN);
        columns.add(ANNUAL_RETURNS_WITH_DIVIDENDS_REINVESTED);
        LocalDate currentDate = LocalDate.now();

        double totalInvested = 0;
        double totalScreenerReturned = 0;
        double totalScreenerReturnedWithDividends = 0.0;
        double totalSpReturned = 0;
        double totalSpReturnedWithDividends = 0;

        List<String> symbolsInExchanges = getSymbolsInExchanges(request.exchanges);

        if (!isHistoricalFilesInitialized) {
            initializeHistoricalFile();
        }

        for (int year = request.startYear; year < request.endYear - 1; ++year) {
            for (int month = 1; month < (backtestMultiMonth ? 12 : 2); month += 3) {
                List<Map<String, String>> bought = new ArrayList<>();
                double yearSp500Sum = 0.0;
                double yearSp500SumWithDividends = 0.0;
                double yearScreenerSum = 0.0;
                double yearScreenerWithDividendSum = 0.0;
                int yearCount = 0;

                LocalDate date = LocalDate.of(year, month, 1);
                LocalDate endDate2 = useLatestData ? currentDate : LocalDate.of(request.endYear, 1, 1);
                double yearAgo = calculateYearsDiff(date, endDate2);

                Map<String, AtGlanceData> data = symbolAtGlanceProvider.loadAtGlanceDataAtYear(year, month).orElse(null);
                List<AtGlanceData> matchedStocks = List.of();
                if (data != null) {
                    matchedStocks = findMatchingStocks(data, request, symbolsInExchanges, true, request.excludedStocks);

                    for (var stockThen : matchedStocks) {
                        var stockNow = getLatestStockData(stockThen, request.endYear, useLatestData);
                        ++yearCount;

                        LocalDate actualDate = stockThen.actualDate;

                        if (stockThen.actualDate == null || stockNow.isEmpty()) {
                            continue;
                        }
                        LocalDate nowDate = stockNow.get().actualDate;
                        double yearAgoExact = calculateYearsDiff(actualDate, nowDate);

                        double sp500PriceThen = spPriceCache.get(actualDate, date2 -> StandardAndPoorPerformanceProvider.getPriceAt(actualDate));
                        double sp500PriceNow;
                        if (useLatestData) {
                            sp500PriceNow = StandardAndPoorPerformanceProvider.getLatestPrice();
                        } else {
                            sp500PriceNow = spPriceCache.get(nowDate, date2 -> StandardAndPoorPerformanceProvider.getPriceAt(nowDate));
                        }

                        double stockPriceThen = stockThen.latestStockPriceUsd;
                        double stockPriceNow = stockNow.get().latestStockPriceUsd;

                        double screenerIncrease = (stockPriceNow / stockPriceThen) * BACKTEST_INVEST_AMOUNT;

                        double initialShareCount = BACKTEST_INVEST_AMOUNT / stockPriceThen;
                        double totalSharesWithDividendsReinvested = calculateTotalSharesWithDividendsReinvested(initialShareCount, year, request.endYear, stockThen.symbol);

                        double finalScreenerCost = stockPriceNow * totalSharesWithDividendsReinvested;

                        double initialSpShareCount = BACKTEST_INVEST_AMOUNT / sp500PriceThen;
                        double totalSpSharesWithDividendsReinvested = calculateTotalSPSharesWithDividendsReinvested(initialSpShareCount, year, request.endYear);

                        double finalSpCost = sp500PriceNow * totalSpSharesWithDividendsReinvested;

                        if (!Double.isFinite(screenerIncrease)) {
                            continue;
                        }

                        yearSp500Sum += (sp500PriceNow / sp500PriceThen) * BACKTEST_INVEST_AMOUNT;
                        yearSp500SumWithDividends += (finalSpCost);
                        yearScreenerSum += screenerIncrease;
                        yearScreenerWithDividendSum += (finalScreenerCost);

                        if (request.addResultTable) {
                            String name = symbolAtGlanceProvider.getAtGlanceData(stockThen.symbol).map(a -> a.companyName).orElse("");
                            Map<String, String> columnResult = new HashMap<>();
                            columnResult.put("Symbol", createSymbolLink(stockNow.get().symbol));
                            columnResult.put("Name", name);
                            columnResult.put("Buy price", formatString(stockPriceThen));
                            columnResult.put("Current price", formatString(stockPriceNow));
                            columnResult.put(TOTAL_RETURN_COLUMN, formatString(((stockPriceNow / stockPriceThen) - 1.0) * 100.0));
                            columnResult.put(ANNUAL_RETURN_COLUMN, formatString((Math.pow((stockPriceNow / stockPriceThen), (1.0 / yearAgoExact)) - 1.0) * 100.0));
                            columnResult.put(ANNUAL_RETURNS_WITH_DIVIDENDS_REINVESTED, formatString((Math.pow((finalScreenerCost / BACKTEST_INVEST_AMOUNT), (1.0 / yearAgoExact)) - 1.0) * 100.0));
                            bought.add(columnResult);
                        }
                    }
                }

                BacktestYearInformation yearInfo = new BacktestYearInformation();
                double yearTotalInvested = yearCount * BACKTEST_INVEST_AMOUNT;
                yearInfo.investedAmount = yearTotalInvested;

                yearInfo.spTotalReturnPercent = ((yearSp500Sum / yearTotalInvested) - 1.0) * 100.0;
                yearInfo.spTotalReturnPercentWithDividends = ((yearSp500SumWithDividends / yearTotalInvested) - 1.0) * 100.0;
                yearInfo.screenerTotalReturnPercent = ((yearScreenerSum / yearTotalInvested) - 1.0) * 100.0;
                yearInfo.screenerTotalReturnPercentWithDividends = ((yearScreenerWithDividendSum / yearTotalInvested) - 1.0) * 100.0;

                yearInfo.spAnnualReturnPercent = (Math.pow(yearSp500Sum / yearTotalInvested, (1.0 / yearAgo)) - 1.0) * 100.0;
                yearInfo.spAnnualReturnPercentWithDividends = (Math.pow(yearSp500SumWithDividends / yearTotalInvested, (1.0 / yearAgo)) - 1.0) * 100.0;
                yearInfo.screenerAnnualReturnPercent = (Math.pow(yearScreenerSum / yearTotalInvested, (1.0 / yearAgo)) - 1.0) * 100.0;
                yearInfo.screenerAnnualReturnPercentWithDividends = (Math.pow(yearScreenerWithDividendSum / yearTotalInvested, (1.0 / yearAgo)) - 1.0) * 100.0;

                yearInfo.spReturnDollar = yearSp500Sum;
                yearInfo.spReturnDollarWithDividends = yearSp500SumWithDividends;
                yearInfo.screenerReturnDollar = yearScreenerSum;
                yearInfo.screenerReturnDollarWithDividends = yearScreenerWithDividendSum;

                yearInfo.investedInAllMatching = matchedStocks.size() < MAX_RESULTS;

                totalInvested += yearTotalInvested;
                totalScreenerReturned += yearScreenerSum;
                totalScreenerReturnedWithDividends += yearScreenerWithDividendSum;
                totalSpReturned += yearSp500Sum;
                totalSpReturnedWithDividends += yearSp500SumWithDividends;

                yearInfo.investedStocks = bought;

                String label = backtestMultiMonth ? String.format("%04d-%02d", year, month) : String.valueOf(year);

                yearResults.put(label, yearInfo);
            }
        }

        int beatCount = 0;
        int investedCount = 0;
        for (var entry : yearResults.values()) {
            if (entry.investedAmount > 0) {
                ++investedCount;
                if (entry.screenerAnnualReturnPercent > entry.spAnnualReturnPercent) {
                    ++beatCount;
                }
            }
        }

        BacktestResult result = new BacktestResult();
        result.beatCount = beatCount;
        result.investedCount = investedCount;
        result.beatPercent = ((double) beatCount / investedCount) * 100.0;
        result.columns = columns;

        result.investedAmount = totalInvested;
        result.sp500Returned = totalSpReturned;
        result.sp500ReturnedWithDividends = totalSpReturnedWithDividends;
        result.screenerReturned = totalScreenerReturned;
        result.screenerReturnedWithDividends = totalScreenerReturnedWithDividends;

        result.screenerAvgPercent = calculateAverage(yearResults, a -> a.screenerAnnualReturnPercent);
        result.screenerMedianPercent = calculateMedian(yearResults, a -> a.screenerAnnualReturnPercent);

        result.sp500AvgPercent = calculateAverage(yearResults, a -> a.spAnnualReturnPercent);
        result.sp500MedianPercent = calculateMedian(yearResults, a -> a.spAnnualReturnPercent);

        result.sp500WithDividendsAvgPercent = calculateAverage(yearResults, a -> a.spAnnualReturnPercentWithDividends);
        result.sp500WithDividendsMedianPercent = calculateMedian(yearResults, a -> a.spAnnualReturnPercentWithDividends);

        result.screenerWithDividendsAvgPercent = calculateAverage(yearResults, a -> a.screenerAnnualReturnPercentWithDividends);
        result.screenerWithDividendsMedianPercent = calculateMedian(yearResults, a -> a.screenerAnnualReturnPercentWithDividends);

        result.yearData = yearResults;
        result.investedInAllMatching = yearResults.values().stream().allMatch(a -> a.investedInAllMatching);

        LOGGER.info("Backtest result invested={} medianReturn={} avgReturn={} medianReturnWithDividends={} avgReturnWithDividends={} beatPercent={}", result.investedAmount,
                result.screenerMedianPercent, result.screenerAvgPercent,
                result.screenerWithDividendsMedianPercent, result.screenerWithDividendsAvgPercent, result.beatPercent);

        return result;
    }

    private void initializeHistoricalFile() {
        synchronized (this) {
            if (!isHistoricalFilesInitialized) {
                try {
                    int processors = Runtime.getRuntime().availableProcessors();
                    if (processors < 1) {
                        processors = 1;
                    }
                    ExecutorService threadPool = Executors.newFixedThreadPool(processors);
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    for (int year = 1993; year < LocalDate.now().getYear() - 1; ++year) {
                        for (int month = 1; month < (backtestMultiMonth ? 12 : 2); month += 3) {
                            int yearConst = year;
                            int monthConst = month;
                            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> symbolAtGlanceProvider.loadAtGlanceDataAtYear(yearConst, monthConst).orElse(null), threadPool);
                            futures.add(future);
                        }
                    }
                    for (var future : futures) {
                        future.join();
                    }
                    isHistoricalFilesInitialized = true;
                    threadPool.shutdownNow();
                } catch (Exception e) {
                    LOGGER.error("Unable to init historical files", e);
                }
            }
        }
    }

    public List<String> getSymbolsInExchanges(List<String> exchangesInput) {
        Set<Exchanges> exchanges = new HashSet<>();
        if (exchangesInput.isEmpty() || exchangesInput.contains("ALL")) {
            exchanges = Arrays.stream(Exchanges.values()).collect(Collectors.toSet());
        } else {
            for (var entry : exchangesInput) {
                exchanges.add(Exchanges.fromString(entry));
            }
        }

        return new ArrayList<>(DataLoader.provideSymbolsIn(exchanges));
    }

    private double calculateYearsDiff(LocalDate date, LocalDate laterDate) {
        return Math.abs(ChronoUnit.DAYS.between(date, laterDate) / 365.0);
    }

    public Optional<AtGlanceData> getLatestStockData(AtGlanceData stockThen, int endYear, boolean useLatestData) {
        if (useLatestData) {
            Optional<AtGlanceData> result = symbolAtGlanceProvider.getAtGlanceData(stockThen.symbol);
            if (result.isPresent()) {
                return result;
            }
        }
        for (int i = endYear; i >= 1991; --i) {
            AtGlanceData data = symbolAtGlanceProvider.loadAtGlanceDataAtYear(i, 1).map(a -> a.get(stockThen.symbol)).orElse(null);
            if (data != null) {
                return Optional.of(data);
            }
        }
        return Optional.empty();
    }

    public double calculateAverage(Map<String, BacktestYearInformation> yearResults, Function<BacktestYearInformation, Double> valueSupplier) {
        return yearResults.values().stream().filter(a -> a.investedAmount > 0).mapToDouble(a -> valueSupplier.apply(a)).average().orElse(0.0);
    }

    public Double calculateMedian(Map<String, BacktestYearInformation> yearResults, Function<BacktestYearInformation, Double> valueSupplier) {
        List<Double> screenerReturns = yearResults.values().stream().filter(a -> a.investedAmount > 0).map(valueSupplier).collect(Collectors.toList());
        Collections.sort(screenerReturns);
        if (screenerReturns.size() > 0) {
            return screenerReturns.get(screenerReturns.size() / 2);
        }
        return 0.0;
    }

    private double calculateTotalSPSharesWithDividendsReinvested(double initialSpShareCount, int year, int endYear) {
        double shareCount = initialSpShareCount;
        for (int i = year + 1; i < endYear; ++i) {
            Double dividendsPaid = StandardAndPoorPerformanceProvider.getDividendsPaidInYear(i);
            LocalDate date = LocalDate.of(i, 1, 1);
            Double priceAtDate = spPriceCache.get(date, date2 -> StandardAndPoorPerformanceProvider.getPriceAt(date2));
            if (dividendsPaid != null) {
                shareCount += (dividendsPaid * shareCount) / priceAtDate;
            }
        }
        return shareCount;
    }

    private double calculateTotalSharesWithDividendsReinvested(double initialShareCount, int year, int endYear, String symbol) {
        double shareCount = initialShareCount;
        for (int i = year + 1; i < endYear; ++i) {
            Map<String, AtGlanceData> data = symbolAtGlanceProvider.loadAtGlanceDataAtYear(i, 1).orElse(null);
            if (data != null) {
                AtGlanceData dataAtYear = data.get(symbol);
                if (dataAtYear != null && dataAtYear.dividendYield < 100.0) { // data issues
                    float dividendPaid = dataAtYear.dividendPaid;
                    shareCount += (dividendPaid * shareCount) / dataAtYear.latestStockPrice;
                }
            }
        }
        if (!Double.isFinite(shareCount)) {
            return initialShareCount;
        }
        return shareCount;
    }

    public String formatString(Double value) {
        if (value == null) {
            return "-";
        } else {
            return String.format("%.2f", value);
        }
    }

    public void validateRequest(ScreenerRequest request, HttpServletRequest httpRequest) {
        if (request.operations.size() > 20) {
            throw new ScreenerClientSideException("Maximum of 20 screeners allowed, " + request.operations.size() + " found");
        }
        if (request.exchanges.size() > Exchanges.values().length) {
            throw new ScreenerClientSideException("Too many exchanges");
        }
        Optional<DecodedJWT> jwt = loginController.getJwt(httpRequest);
        if (!jwt.isPresent() && request.operations.size() > 1) {
            throw new ScreenerClientSideException("Logged out users can add maximum of 1 screeners, " + request.operations.size() + " found");
        }

        for (var element : request.operations) {
            if (!idToDescription.containsKey(element.id)) {
                throw new ScreenerClientSideException(element.id + " is not a valid screener condition");
            }
            ScreenerStrategy screenerStrategy = findScreenerStrategy(element.operation);
            if (screenerStrategies.stream().noneMatch(a -> a.getSymbol().equals(element.operation))) {
                throw new ScreenerClientSideException(element.operation + " is not a valid operation");
            }

            if (screenerStrategy.getNumberOfArguments() == 1 && element.number1 == null) {
                throw new ScreenerClientSideException(element.operation + " requires number1 to be filled");
            }

            if (screenerStrategy.getNumberOfArguments() == 2 && (element.number1 == null || element.number2 == null)) {
                throw new ScreenerClientSideException(element.operation + " requires number1 and number2 to be filled");
            }

            element.screenerStrategy = findScreenerStrategy(element.operation); // cache
        }
    }

    public double getValue(AtGlanceData glance, ScreenerOperation operation, ScreenerDescription screenerDescriptor) {
        try {
            if (screenerDescriptor.source.equals(Source.FIELD)) {
                //                Object value = ((Field) screenerDescriptor.data).get(glance);
                //                return convertNumberToType(value);
                return unreflectiveGetField(operation.id, glance);
            } else if (screenerDescriptor.source.equals(Source.METHOD)) {
                //                Object value = ((Method) screenerDescriptor.data).invoke(glance);
                //                return convertNumberToType(value);
                return unreflectiveGetMethod(operation.id, glance);
            } else {
                throw new RuntimeException("Unknown source");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private double unreflectiveGetMethod(String id, AtGlanceData glance) {
        switch (id) {
            case "fcf_yield":
                return glance.getFreeCashFlowYield();
            case "earnings_yield":
                return glance.getEarningsYield();
            default:
                throw new RuntimeException("Unexpected type " + id);
        }
    }

    private double unreflectiveGetField(String id, AtGlanceData glance) {
        switch (id) {
            case "marketCapUsd":
                return glance.marketCapUsd;
            case "trailingPeg":
                return glance.trailingPeg;
            case "roic":
                return glance.roic;
            case "altman":
                return glance.altman;
            case "pietrosky":
                return glance.pietrosky;
            case "pe":
                return glance.pe;
            case "evToEbitda":
                return glance.evToEbitda;
            case "ptb":
                return glance.ptb;
            case "pts":
                return glance.pts;
            case "icr":
                return glance.icr;
            case "currentRatio":
                return glance.currentRatio;
            case "quickRatio":
                return glance.quickRatio;
            case "dtoe":
                return glance.dtoe;
            case "roe":
                return glance.roe;
            case "epsGrowth":
                return glance.epsGrowth;
            case "fcfGrowth":
                return glance.fcfGrowth;
            case "revenueGrowth":
                return glance.revenueGrowth;
            case "fYrIncomeGr":
                return glance.fYrIncomeGr;
            case "dividendGrowthRate":
                return glance.dividendGrowthRate;
            case "shareCountGrowth":
                return glance.shareCountGrowth;
            case "netMarginGrowth":
                return glance.netMarginGrowth;
            case "cape":
                return glance.cape;
            case "epsSD":
                return glance.epsSD;
            case "revSD":
                return glance.revSD;
            case "fcfSD":
                return glance.fcfSD;
            case "epsFcfCorrelation":
                return glance.epsFcfCorrelation;
            case "dividendYield":
                return glance.dividendYield;
            case "dividendPayoutRatio":
                return glance.dividendPayoutRatio;
            case "dividendFcfPayoutRatio":
                return glance.dividendFcfPayoutRatio;
            case "profitableYears":
                return glance.profitableYears;
            case "fcfProfitableYears":
                return glance.fcfProfitableYears;
            case "stockCompensationPerMkt":
                return glance.stockCompensationPerMkt;
            case "cpxToRev":
                return glance.cpxToRev;
            case "sloan":
                return glance.sloan;
            case "ideal10yrRevCorrelation":
                return glance.ideal10yrRevCorrelation;
            case "ideal10yrEpsCorrelation":
                return glance.ideal10yrEpsCorrelation;
            case "ideal10yrFcfCorrelation":
                return glance.ideal10yrFcfCorrelation;
            case "ideal20yrRevCorrelation":
                return glance.ideal20yrRevCorrelation;
            case "ideal20yrEpsCorrelation":
                return glance.ideal20yrEpsCorrelation;
            case "ideal20yrFcfCorrelation":
                return glance.ideal20yrFcfCorrelation;
            case "fvCalculatorMoS":
                return glance.fvCalculatorMoS;
            case "fvCompositeMoS":
                return glance.fvCompositeMoS;
            case "grahamMoS":
                return glance.grahamMoS;
            case "starFlags":
                return glance.starFlags;
            case "redFlags":
                return glance.redFlags;
            case "yellowFlags":
                return glance.yellowFlags;
            case "greenFlags":
                return glance.greenFlags;
            case "fYrPe":
                return glance.fYrPe;
            case "fYrPFcf":
                return glance.fYrPFcf;
            case "fiveYrRoic":
                return glance.fiveYrRoic;
            case "ltl5Fcf":
                return glance.ltl5Fcf;
            case "price10Gr":
                return glance.price10Gr;
            case "price15Gr":
                return glance.price15Gr;
            case "price20Gr":
                return glance.price20Gr;
            case "grMargin":
                return glance.grMargin;
            case "opMargin":
                return glance.opMargin;
            case "opCMargin":
                return glance.opCMargin;
            case "fcfMargin":
                return glance.fcfMargin;
            default:
                throw new RuntimeException("Unexpected type " + id);
        }
    }

    public double convertNumberToType(Object value) {
        if (value == null) {
            return Double.NaN;
        }
        if (value.getClass().equals(Double.class)) {
            return (Double) value;
        } else if (value.getClass().equals(Float.class)) {
            return ((Float) value).doubleValue();
        } else if (value.getClass().equals(Byte.class)) {
            return ((Byte) value).doubleValue();
        } else if (value.getClass().equals(Integer.class)) {
            return ((Integer) value).doubleValue();
        } else if (value.getClass().equals(Short.class)) {
            return ((Short) value).doubleValue();
        } else {
            throw new RuntimeException("Unknown type");
        }
    }

    public ScreenerStrategy findScreenerStrategy(String operation) {
        return screenerStrategies.stream()
                .filter(a -> a.getSymbol().equals(operation))
                .findFirst()
                .orElse(null);
    }

    public String createSymbolLink(String ticker) {
        return "<a href=\"/stock/" + ticker + "\">" + ticker + "</a>";
    }

    @ExceptionHandler(ScreenerClientSideException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public GenericErrorResponse exceptionHandler(ScreenerClientSideException exception) {
        LOGGER.warn("Client side error while performing screening {}", exception.getMessage());
        return new GenericErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public GenericErrorResponse exceptionHandler(Exception exception) {
        LOGGER.error("Unexpected error while doing screeners", exception);
        return new GenericErrorResponse("Unexpected error while doing screeners");
    }

    public void setBacktestMultiMonth(boolean backtestMultiMonth) {
        this.backtestMultiMonth = backtestMultiMonth;
    }

}
