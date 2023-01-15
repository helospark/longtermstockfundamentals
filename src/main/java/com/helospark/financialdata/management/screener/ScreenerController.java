package com.helospark.financialdata.management.screener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
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
    private static final String ANNUAL_RETURN_COLUMN = "Annual return (%)";
    private static final String TOTAL_RETURN_COLUMN = "Total return (%)";
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenerController.class);
    public static final int MAX_RESULTS = 101;
    public static double BACKTEST_INVEST_AMOUNT = 1000.0;
    Map<String, ScreenerDescription> idToDescription = new LinkedHashMap<>();

    @Autowired
    private SymbolAtGlanceProvider symbolAtGlanceProvider;
    @Autowired
    private List<ScreenerStrategy> screenerStrategies;
    @Autowired
    private LoginController loginController;

    public ScreenerController() {
        for (var field : AtGlanceData.class.getDeclaredFields()) {
            ScreenerElement screenerElement = field.getAnnotation(ScreenerElement.class);
            if (screenerElement != null) {
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
        for (int i = 1994; i <= LocalDate.now().getYear() - 1; ++i) {
            result.add(i);
        }
        return result;
    }

    @PostMapping("/perform")
    @RateLimit(requestPerMinute = 20)
    public ScreenerResult screenStocks(@RequestBody ScreenerRequest request, HttpServletRequest httpRequest) {
        LOGGER.info("Received screener request '{}'", request);
        validateRequest(request, httpRequest);
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
        List<AtGlanceData> matchedStocks = findMatchingStocks(data, request, false);

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

    public List<AtGlanceData> findMatchingStocks(Map<String, AtGlanceData> data, ScreenerRequest request, boolean randomize) {
        List<AtGlanceData> matchedStocks = new ArrayList<>();

        Set<Exchanges> exchanges = new HashSet<>();
        if (request.exchanges.isEmpty() || request.exchanges.contains("ALL")) {
            exchanges = Arrays.stream(Exchanges.values()).collect(Collectors.toSet());
        } else {
            for (var entry : request.exchanges) {
                exchanges.add(Exchanges.fromString(entry));
            }
        }

        Set<String> symbols = DataLoader.provideSymbolsIn(exchanges);

        List<Entry<String, AtGlanceData>> entrySet = new ArrayList<>(data.entrySet());
        if (randomize) {
            Collections.shuffle(entrySet);
        }

        for (var entry : entrySet) {
            if (!symbols.contains(entry.getKey())) {
                continue;
            }
            var atGlanceData = entry.getValue();

            boolean allMatch = true;
            for (var operation : request.operations) {
                ScreenerDescription source = idToDescription.get(operation.id);
                Double value = getValue(atGlanceData, operation, source);
                if (value == null) {
                    allMatch = false;
                    break;
                }
                ScreenerStrategy screenerStrategy = findScreenerStrategy(operation.operation);
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

        if (request.endYear < request.startYear) {
            throw new ScreenerClientSideException("End date must be greater than start time");
        }
        if (request.endYear - request.startYear < 2) {
            throw new ScreenerClientSideException("More than 2 year difference expected");
        }
        if (request.endYear >= LocalDate.now().getYear()) {
            throw new ScreenerClientSideException("Unfortunataly our datasource doesn't provide the future stock prices :(");
        }
        if (request.startYear < 1990) {
            throw new ScreenerClientSideException("We only have data from 1990");
        }

        Map<Integer, BacktestYearInformation> yearResults = new LinkedHashMap<>();

        Set<String> columns = new LinkedHashSet<>();
        columns.add("Symbol");
        columns.add("Name");
        columns.add("Buy price");
        columns.add("Current price");
        columns.add(TOTAL_RETURN_COLUMN);
        columns.add(ANNUAL_RETURN_COLUMN);
        LocalDate currentDate = LocalDate.now();

        double totalInvested = 0;
        double totalScreenerReturned = 0;
        double totalSpReturned = 0;
        for (int year = request.startYear; year < request.endYear; ++year) {
            List<Map<String, String>> bought = new ArrayList<>();
            double yearSp500Sum = 0.0;
            double yearScreenerSum = 0.0;
            int yearCount = 0;

            LocalDate date = LocalDate.of(year, 1, 1);
            double yearAgo = (currentDate.getYear() - year) + (date.getMonthValue() - 1) / 12.0;

            Map<String, AtGlanceData> data = symbolAtGlanceProvider.loadAtGlanceDataAtYear(year).orElse(null);
            List<AtGlanceData> matchedStocks = List.of();
            if (data != null) {
                matchedStocks = findMatchingStocks(data, request, true);

                for (var stockThen : matchedStocks) {
                    var stockNow = symbolAtGlanceProvider.getAtGlanceData(stockThen.symbol);
                    ++yearCount;

                    LocalDate actualDate = stockThen.actualDate;

                    double sp500PriceThen = StandardAndPoorPerformanceProvider.getPriceAt(actualDate);
                    double sp500PriceNow = StandardAndPoorPerformanceProvider.getLatestPrice();

                    double stockPriceThen = stockThen.latestStockPriceUsd;
                    double stockPriceNow = stockNow.get().latestStockPriceUsd;

                    double screenerIncrease = (stockPriceNow / stockPriceThen) * BACKTEST_INVEST_AMOUNT;
                    if (!Double.isFinite(screenerIncrease)) {
                        continue;
                    }

                    yearSp500Sum += (sp500PriceNow / sp500PriceThen) * BACKTEST_INVEST_AMOUNT;
                    yearScreenerSum += screenerIncrease;

                    Map<String, String> columnResult = new HashMap<>();
                    columnResult.put("Symbol", createSymbolLink(stockNow.get().symbol));
                    columnResult.put("Name", stockNow.get().companyName);
                    columnResult.put("Buy price", formatString(stockPriceThen));
                    columnResult.put("Current price", formatString(stockPriceNow));
                    columnResult.put(TOTAL_RETURN_COLUMN, formatString(((stockPriceNow / stockPriceThen) - 1.0) * 100.0));
                    columnResult.put(ANNUAL_RETURN_COLUMN, formatString((Math.pow((stockPriceNow / stockPriceThen), (1.0 / yearAgo)) - 1.0) * 100.0));

                    bought.add(columnResult);
                }
            }

            BacktestYearInformation yearInfo = new BacktestYearInformation();
            double yearTotalInvested = yearCount * BACKTEST_INVEST_AMOUNT;
            yearInfo.investedAmount = yearTotalInvested;

            yearInfo.spTotalReturnPercent = ((yearSp500Sum / yearTotalInvested) - 1.0) * 100.0;
            yearInfo.screenerTotalReturnPercent = ((yearScreenerSum / yearTotalInvested) - 1.0) * 100.0;

            yearInfo.spAnnualReturnPercent = (Math.pow(yearSp500Sum / yearTotalInvested, (1.0 / yearAgo)) - 1.0) * 100.0;
            yearInfo.screenerAnnualReturnPercent = (Math.pow(yearScreenerSum / yearTotalInvested, (1.0 / yearAgo)) - 1.0) * 100.0;

            yearInfo.spReturnDollar = yearSp500Sum;
            yearInfo.screenerReturnDollar = yearScreenerSum;

            yearInfo.investedInAllMatching = matchedStocks.size() < MAX_RESULTS;

            totalInvested += yearTotalInvested;
            totalScreenerReturned += yearScreenerSum;
            totalSpReturned += yearSp500Sum;

            yearInfo.investedStocks = bought;

            yearResults.put(year, yearInfo);
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
        result.screenerReturned = totalScreenerReturned;

        result.screenerAvgPercent = yearResults.values().stream().filter(a -> a.investedAmount > 0).mapToDouble(a -> a.screenerAnnualReturnPercent).average().orElse(0.0);
        result.sp500AvgPercent = yearResults.values().stream().filter(a -> a.investedAmount > 0).mapToDouble(a -> a.spAnnualReturnPercent).average().orElse(0.0);

        List<Double> screenerReturns = yearResults.values().stream().filter(a -> a.investedAmount > 0).map(a -> a.screenerAnnualReturnPercent).collect(Collectors.toList());
        List<Double> spReturns = yearResults.values().stream().filter(a -> a.investedAmount > 0).map(a -> a.spAnnualReturnPercent).collect(Collectors.toList());
        Collections.sort(screenerReturns);
        Collections.sort(spReturns);

        if (screenerReturns.size() > 0 && spReturns.size() > 0) {
            result.screenerMedianPercent = screenerReturns.get(screenerReturns.size() / 2);
            result.sp500MedianPercent = spReturns.get(spReturns.size() / 2);
        }

        result.yearData = yearResults;
        result.investedInAllMatching = yearResults.values().stream().allMatch(a -> a.investedInAllMatching);

        LOGGER.info("Backtest result invested={} medianReturn={} avgReturn={} beatPercent={}", result.investedAmount, result.screenerMedianPercent, result.screenerAvgPercent, result.beatPercent);

        return result;
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

        }
    }

    public Double getValue(AtGlanceData glance, ScreenerOperation operation, ScreenerDescription screenerDescriptor) {
        try {
            if (screenerDescriptor.source.equals(Source.FIELD)) {
                Object value = ((Field) screenerDescriptor.data).get(glance);
                return convertNumberToType(value);
            } else if (screenerDescriptor.source.equals(Source.METHOD)) {
                Object value = ((Method) screenerDescriptor.data).invoke(glance);
                return convertNumberToType(value);
            } else {
                throw new RuntimeException("Unknown source");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Double convertNumberToType(Object value) {
        if (value.getClass().equals(Double.class)) {
            return (Double) value;
        } else if (value.getClass().equals(Float.class)) {
            return ((Float) value).doubleValue();
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

}
