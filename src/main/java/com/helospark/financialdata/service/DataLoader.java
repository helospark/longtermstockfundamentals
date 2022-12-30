package com.helospark.financialdata.service;

import static com.helospark.financialdata.CommonConfig.BASE_FOLDER;
import static com.helospark.financialdata.CommonConfig.FX_BASE_FOLDER;
import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;
import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDateSafe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.Striped;
import com.helospark.financialdata.domain.BalanceSheet;
import com.helospark.financialdata.domain.CashFlow;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.EnterpriseValue;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FxRatesResponse;
import com.helospark.financialdata.domain.HistoricalPrice;
import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.domain.IncomeStatement;
import com.helospark.financialdata.domain.KeyMetrics;
import com.helospark.financialdata.domain.NoTtmNeeded;
import com.helospark.financialdata.domain.Profile;
import com.helospark.financialdata.domain.RemoteRatio;
import com.helospark.financialdata.domain.TresuryRate;
import com.helospark.financialdata.service.exchanges.Exchanges;

public class DataLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);
    private static final String CACHE_SAVE_FILE = "/tmp/cache.ser";
    private static Striped<Lock> duplicateLoadLocks = Striped.lock(1000);

    static ObjectMapper objectMapper = new ObjectMapper();

    static Cache<String, CompanyFinancials> cache;

    static Cache<String, FxRatesResponse> fxCache;
    static List<TresuryRate> tresuryRateCache;

    static {
        init();
    }

    private static void init() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JSR310Module());

        int cacheSize = getConfig("STOCK_CACHE_SIZE", 50000);
        int fcCacheSize = getConfig("FX_CACHE_SIZE", 10000);

        cache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .maximumSize(cacheSize)
                .build();

        fxCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .maximumSize(fcCacheSize)
                .build();

        //        File cacheFile = new File(CACHE_SAVE_FILE);
        //        if (cacheFile.exists()) {
        //            try {
        //                cache.putAll(objectMapper.readValue(cacheFile, new TypeReference<Map<String, CompanyFinancials>>() {
        //                }));
        //            } catch (Exception e) {
        //                e.printStackTrace();
        //            }
        //        }
    }

    public static int getConfig(String config, int defaultValue) {
        String cacheSizeInt = System.getProperty(config);
        int cacheSize = defaultValue;
        if (cacheSizeInt != null) {
            cacheSize = Integer.parseInt(cacheSizeInt);
        }
        return cacheSize;
    }

    public static CompanyFinancials readFinancials(String symbol) {
        CompanyFinancials cachedResult = cache.getIfPresent(symbol);
        if (cachedResult != null) {
            return cachedResult;
        }
        Lock lock = duplicateLoadLocks.get(symbol);
        try {
            lock.tryLock(10, TimeUnit.SECONDS);
            return loadData(symbol);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private static CompanyFinancials loadData(String symbol) {
        CompanyFinancials cachedResult = cache.getIfPresent(symbol);
        if (cachedResult != null) {
            return cachedResult;
        }

        //        System.out.println("Loading " + symbol);

        List<BalanceSheet> balanceSheet = readFinancialFile(symbol, "balance-sheet.json", BalanceSheet.class);
        List<IncomeStatement> incomeStatement = readFinancialFile(symbol, "income-statement.json", IncomeStatement.class);
        List<CashFlow> cashFlow = readFinancialFile(symbol, "cash-flow.json", CashFlow.class);
        List<RemoteRatio> remoteRatios = readFinancialFile(symbol, "ratios.json", RemoteRatio.class);
        List<HistoricalPriceElement> historicalPrice = readHistoricalFile(symbol, "historical-price.json");
        List<EnterpriseValue> enterpriseValues = readFinancialFile(symbol, "enterprise-values.json", EnterpriseValue.class);
        List<KeyMetrics> keyMetrics = readFinancialFile(symbol, "key-metrics.json", KeyMetrics.class);
        List<Profile> profiles = readFinancialFile(symbol, "profile.json", Profile.class);

        Profile profile;
        if (profiles.isEmpty()) {
            profile = new Profile();
        } else {
            profile = profiles.get(0);
        }
        if (incomeStatement.size() > 0) {
            profile.reportedCurrency = incomeStatement.get(0).reportedCurrency;
            profile.currencySymbol = getCurrencySymbol(incomeStatement);
        }

        CompanyFinancials result = createToTtm(symbol, balanceSheet, incomeStatement, cashFlow, remoteRatios, enterpriseValues, historicalPrice, keyMetrics, profile);

        cache.put(symbol, result);

        return result;
    }

    public static String getCurrencySymbol(List<IncomeStatement> incomeStatement) {
        try {
            return Currency.getInstance(incomeStatement.get(0).reportedCurrency).getSymbol();
        } catch (Exception e) {
            return "";
        }
    }

    private static CompanyFinancials createToTtm(String symbol, List<BalanceSheet> balanceSheets, List<IncomeStatement> incomeStatements, List<CashFlow> cashFlows, List<RemoteRatio> remoteRatios,
            List<EnterpriseValue> enterpriseValue, List<HistoricalPriceElement> prices, List<KeyMetrics> keyMetrics, Profile profile) {
        List<FinancialsTtm> result = new ArrayList<>();

        if (incomeStatements.isEmpty()) {
            //  System.out.println("Not reported for a long time, reject " + symbol);
            return new CompanyFinancials();
        }

        int dataQualityIssue = 0;

        int i = 0;
        while (true) {
            int ttmEndIndex = i + 3;
            if (ttmEndIndex >= incomeStatements.size()) {
                break;
            }

            LocalDate incomeStatementDate = incomeStatements.get(i).date;
            int cashFlowIndex = findIndexWithOrBeforeDate(cashFlows, incomeStatementDate);
            int balanceSheetIndex = findIndexWithOrBeforeDate(balanceSheets, incomeStatementDate);
            int remoteRatioIndex = findIndexWithOrBeforeDate(remoteRatios, incomeStatementDate);
            int historicalPriceIndex = findIndexWithOrBeforeDateSafe(enterpriseValue, incomeStatementDate);
            int keyMetricsIndex = findIndexWithOrBeforeDateSafe(keyMetrics, incomeStatementDate);

            if (cashFlowIndex == -1 || balanceSheetIndex == -1 ||
                    cashFlowIndex + 3 >= cashFlows.size() ||
                    balanceSheetIndex + 3 >= balanceSheets.size() ||
                    remoteRatioIndex + 3 >= remoteRatios.size()) {
                break;
            }

            LocalDate balanceSheetDate = balanceSheets.get(balanceSheetIndex).date;
            LocalDate cashFlowDate = cashFlows.get(cashFlowIndex).date;

            if (Helpers.daysBetween(balanceSheetDate, cashFlowDate) > 100 ||
                    Helpers.daysBetween(balanceSheetDate, incomeStatementDate) > 100 ||
                    Helpers.daysBetween(cashFlowDate, incomeStatementDate) > 100) {
                //System.out.println("[WARN] Inconsistent data " + symbol + " " + incomeStatementDate + " " + cashFlowDate + " " + balanceSheetDate);
                if (incomeStatementDate.compareTo(LocalDate.of(2010, 1, 1)) > 0) {
                    ++dataQualityIssue;
                }
            }
            double price = historicalPriceIndex >= 0 ? enterpriseValue.get(historicalPriceIndex).stockPrice : 0.1;

            FinancialsTtm currentTtm = new FinancialsTtm();
            currentTtm.date = incomeStatementDate;
            currentTtm.balanceSheet = balanceSheets.get(balanceSheetIndex);
            currentTtm.cashFlow = cashFlows.get(cashFlowIndex);
            currentTtm.keyMetrics = keyMetrics.get(keyMetricsIndex);
            currentTtm.remoteRatio = remoteRatios.get(remoteRatioIndex);
            currentTtm.incomeStatement = incomeStatements.get(i);
            currentTtm.cashFlowTtm = calculateTtm(cashFlows, cashFlowIndex, new CashFlow());
            currentTtm.incomeStatementTtm = calculateTtm(incomeStatements, i, new IncomeStatement());

            price = convertCurrencyIfNeeded(price, currentTtm, profile);
            currentTtm.price = price;

            result.add(currentTtm);

            ++i;
        }

        if (dataQualityIssue > 0) {
            //   System.out.println("Data quality issue: " + symbol);
        }
        if (dataQualityIssue > 7) {
            //    System.out.println("Rejected: " + symbol);
            // return new CompanyFinancials();
        }
        if (result.isEmpty()) {
            return new CompanyFinancials(0.0, result, profile, dataQualityIssue);
        }

        double price = prices.isEmpty() ? 0 : prices.get(0).close;
        return new CompanyFinancials(convertCurrencyIfNeeded(price, result.get(0), profile), result, profile, dataQualityIssue);
    }

    private static double convertCurrencyIfNeeded(double price, FinancialsTtm currentTtm, Profile profile) {
        if (!currentTtm.incomeStatement.reportedCurrency.equals(profile.currency)) {
            Optional<Double> convertedCurrency = convertFx(price, profile.currency, currentTtm.incomeStatement.reportedCurrency, currentTtm.incomeStatement.getDate(), true);
            if (convertedCurrency.isEmpty()) {
                //System.out.println("Cannot convert currency " + currentTtm.incomeStatement.reportedCurrency + " " + currentTtm.incomeStatement.getDate());
            }
            price = convertedCurrency.orElse(price);
        }
        return price;
    }

    private static <T> T calculateTtm(List<T> cashFlows, int current, T result) {
        try {
            for (var field : result.getClass().getFields()) {
                if (field.getAnnotation(NoTtmNeeded.class) != null) {
                    field.set(result, field.get(cashFlows.get(current)));
                } else {
                    if (field.getType().equals(Long.TYPE)) {
                        long value = 0;
                        for (int i = current; i < current + 4; ++i) {
                            value += (long) field.get(cashFlows.get(i));
                        }
                        field.set(result, value);
                    }
                    if (field.getType().equals(Double.TYPE)) {
                        double value = 0;
                        for (int i = current; i < current + 4; ++i) {
                            value += (double) field.get(cashFlows.get(i));
                        }
                        field.set(result, value);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> readFinancialFile(String symbol, String fileName, Class<T> clazz) {
        File dataFile = new File(BASE_FOLDER + "/fundamentals/" + symbol + "/" + fileName);

        return readListOfClassFromFile(dataFile, clazz);
    }

    public static <T> List<T> readListOfClassFromFile(File dataFile, Class<T> clazz) {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
        if (!dataFile.exists()) {
            // System.out.println("File not found " + dataFile);
            return List.of();
        }

        try {
            return objectMapper.readValue(dataFile, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<HistoricalPriceElement> readHistoricalFile(String symbol, String fileName) {
        File dataFile = new File(BASE_FOLDER + "/fundamentals/" + symbol + "/" + fileName);
        return loadHistoricalFile(dataFile);
    }

    public static List<HistoricalPriceElement> loadHistoricalFile(File dataFile) {
        if (!dataFile.exists()) {
            // System.out.println("File not found " + dataFile);
            return List.of();
        }

        try {
            List<HistoricalPriceElement> result = objectMapper.readValue(dataFile, HistoricalPrice.class).historical;
            if (result == null) {
                return List.of();
            } else {
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<String> provideAllSymbolsWithPostfix(String postfix) {
        return Arrays.stream(new File(BASE_FOLDER + "/fundamentals").listFiles())
                .filter(a -> a.isDirectory())
                .map(a -> a.getName())
                .filter(a -> a.endsWith(postfix))
                .collect(Collectors.toSet());
    }

    public static Set<String> provideAllSymbolsWithExclude(String exclude) {
        return Arrays.stream(new File(BASE_FOLDER + "/fundamentals").listFiles())
                .filter(a -> a.isDirectory())
                .map(a -> a.getName())
                .filter(a -> !a.contains(exclude))
                .collect(Collectors.toSet());
    }

    public static Set<String> readSymbolsFromCsv(String path) {
        Set<String> result = new HashSet<>();
        File file = new File(BASE_FOLDER + "/" + path);
        try (FileInputStream fis = new FileInputStream(file)) {
            String[] lines = new String(fis.readAllBytes(), StandardCharsets.UTF_8).split("\n");
            for (var line : lines) {
                result.add(line.split(",")[0]);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static Set<String> provideSymbolsFromNasdaqNyse() {
        return provideSymbolsIn(Set.of(Exchanges.NASDAQ, Exchanges.NYSE));
    }

    public static Set<String> provideUsSymbols() {
        return provideAllSymbols()
                .stream()
                .filter(a -> !a.contains("."))
                .collect(Collectors.toSet());
    }

    public static Set<String> provideAllSymbols() {
        return Arrays.stream(new File(BASE_FOLDER + "/fundamentals").listFiles())
                .filter(a -> a.isDirectory())
                .map(a -> a.getName())
                .collect(Collectors.toSet());
    }

    public static void save() {
        if (!new File(CACHE_SAVE_FILE).exists()) {
            try {
                objectMapper.writeValue(new File(CACHE_SAVE_FILE), cache);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Optional<FxRatesResponse> loadFxFile(String fromCurrency, LocalDate date) {
        String fileName = fromCurrency + "_" + date.getYear() + ".json";
        try {
            if (fxCache.getIfPresent(fileName) != null) {
                return Optional.of(fxCache.getIfPresent(fileName));
            } else {
                FxRatesResponse result = objectMapper.readValue(new File(FX_BASE_FOLDER + "/" + fileName), FxRatesResponse.class);
                fxCache.put(fileName, result);
                return Optional.of(result);
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<Double> convertFx(double value, String fromCurrency, String toCurrency, LocalDate date, boolean ensureDatesInRange) {
        if (ensureDatesInRange) {
            if (date.compareTo(LocalDate.of(2000, 1, 1)) < 0) {
                date = LocalDate.of(2000, 1, 1);
            }
            if (date.compareTo(LocalDate.of(2022, 12, 30)) > 0) {
                date = LocalDate.of(2022, 12, 30);
            }
        }

        var oneYearRate = loadFxFile(fromCurrency, date);
        if (oneYearRate.isPresent()) {
            var simpleConversion = findClosesConversionRate(value, toCurrency, date, oneYearRate.get().rates);
            if (simpleConversion.isPresent()) {
                return simpleConversion;
            }
        }
        // sometimes there are no conversion rates in the entire rate, try finding the closes one on the surrounding 10 years
        Map<String, Map<String, Double>> rates = new LinkedHashMap<>();
        for (int i = 10; i >= -10; --i) {
            LocalDate newDate = date.plusYears(i);
            Optional<FxRatesResponse> ratesResponse = loadFxFile(fromCurrency, newDate);
            if (ratesResponse.isPresent()) {
                rates.putAll(ratesResponse.get().rates);
            }
        }
        return findClosesConversionRate(value, toCurrency, date, rates);
    }

    public static Optional<Double> findClosesConversionRate(double value, String toCurrency, LocalDate date, Map<String, Map<String, Double>> rates) {
        if (rates.size() > 0) {
            Double conversionValue = getConversionValue(toCurrency, date, rates);
            if (conversionValue != null) {
                return Optional.of(value * conversionValue);
            } else {
                LOGGER.warn("No currency conversion for " + toCurrency + " at date " + date);
                return Optional.empty();
            }
        } else {
            LOGGER.warn("No currency conversion for " + toCurrency + " at date " + date);
            return Optional.empty();
        }
    }

    private static Double getConversionValue(String toCurrency, LocalDate date, Map<String, Map<String, Double>> rates) {
        Map<String, Double> conversions = getConversionValueAtDate(date, rates, toCurrency);
        if (conversions == null) {
            return null;
        }
        Double conversionValue = conversions.get(toCurrency);
        return conversionValue;
    }

    private static Map<String, Double> getConversionValueAtDate(LocalDate date, Map<String, Map<String, Double>> rates, String toCurrency) {
        Map<String, Double> result = rates.get(date.toString());
        if (result != null && result.containsKey(toCurrency)) {
            return result;
        }
        result = null;
        long minDays = Integer.MAX_VALUE;
        for (var entry : rates.entrySet()) {
            LocalDate entryDate = LocalDate.parse(entry.getKey());
            if (entry.getValue().containsKey(toCurrency) && (result == null || Helpers.daysBetween(entryDate, date) < minDays)) {
                result = entry.getValue();
                minDays = Helpers.daysBetween(entryDate, date);
            }
        }
        return result;
    }

    public static List<TresuryRate> loadTresuryRates() {
        if (tresuryRateCache != null) {
            return tresuryRateCache;
        } else {
            List<TresuryRate> result = readListOfClassFromFile(new File(BASE_FOLDER + "/info/tresury_rates.json"), TresuryRate.class);
            tresuryRateCache = result;
            return result;
        }
    }

    public static Set<String> provideSymbolsIn(Set<Exchanges> exchanges) {
        Set<String> result = new HashSet<>();

        for (var exchange : exchanges) {
            File file = new File(BASE_FOLDER + "/info/exchanges/" + exchange.name());
            try (FileInputStream fis = new FileInputStream(file)) {
                var lines = new String(fis.readAllBytes()).split("\n");
                Arrays.stream(lines).forEach(a -> result.add(a));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
