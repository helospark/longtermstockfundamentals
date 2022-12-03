package com.helospark.financialdata.service;

import static com.helospark.financialdata.CommonConfig.BASE_FOLDER;
import static com.helospark.financialdata.CommonConfig.FX_BASE_FOLDER;
import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.helospark.financialdata.domain.BalanceSheet;
import com.helospark.financialdata.domain.CashFlow;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.DateAware;
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

public class DataLoader {
    private static final String CACHE_SAVE_FILE = "/tmp/cache.ser";
    static Object loadObject = new Object();
    static ObjectMapper objectMapper = new ObjectMapper();

    static Map<String, CompanyFinancials> cache = new HashMap<>();
    static Map<String, FxRatesResponse> fxCache = new HashMap<>();
    static List<TresuryRate> tresuryRateCache;

    static {
        init();
    }

    private static void init() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JSR310Module());

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

    public static CompanyFinancials readFinancials(String symbol) {
        CompanyFinancials cachedResult = cache.get(symbol);
        if (cachedResult != null) {
            return cachedResult;
        }
        return loadData(symbol);
    }

    private static CompanyFinancials loadData(String symbol) {
        synchronized (loadObject) {
            CompanyFinancials cachedResult = cache.get(symbol);
            if (cachedResult != null) {
                return cachedResult;
            }

            //            System.out.println("Loading " + symbol);

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

            CompanyFinancials result = createToTtm(symbol, balanceSheet, incomeStatement, cashFlow, remoteRatios, enterpriseValues, historicalPrice, keyMetrics, profile);

            cache.put(symbol, result);

            return result;
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

            if (ChronoUnit.DAYS.between(balanceSheetDate, cashFlowDate) > 100 ||
                    ChronoUnit.DAYS.between(balanceSheetDate, incomeStatementDate) > 100 ||
                    ChronoUnit.DAYS.between(cashFlowDate, incomeStatementDate) > 100) {
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
            currentTtm.profile = profile;

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
            return new CompanyFinancials();
        }
        if (result.isEmpty()) {
            return new CompanyFinancials(0.0, result);
        }

        double price = prices.isEmpty() ? 0 : prices.get(0).close;
        return new CompanyFinancials(convertCurrencyIfNeeded(price, result.get(0), profile), result);
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

    private static int findIndexWithOrBeforeDateSafe(List<? extends DateAware> cashFlows, LocalDate date) {
        for (int i = 0; i < cashFlows.size(); ++i) {
            LocalDate cashFlowDate = cashFlows.get(i).getDate();
            if (ChronoUnit.DAYS.between(date, cashFlowDate) < 20) {
                return i;
            } else if (cashFlowDate.compareTo(date.minusDays(20)) < 0) {
                return i;
            }
        }
        return cashFlows.size() - 1;
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
        Set<String> nasdaqSymbols = readSymbolsFromCsv("info/nasdaq.csv");
        Set<String> nyseSymbols = readSymbolsFromCsv("info/nyse.csv");

        Set<String> symbols = new HashSet<>();
        symbols.addAll(nyseSymbols);
        symbols.addAll(nasdaqSymbols);
        return symbols;
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
            if (fxCache.containsKey(fileName)) {
                return Optional.of(fxCache.get(fileName));
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
        Optional<FxRatesResponse> ratesResponse = loadFxFile(fromCurrency, date);
        if (ratesResponse.isPresent()) {
            Double conversionValue = getConversionValue(toCurrency, date, ratesResponse.get());
            if (conversionValue != null) {
                return Optional.of(value * conversionValue);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private static Double getConversionValue(String toCurrency, LocalDate date, FxRatesResponse fxRatesResponse) {
        Map<String, Double> conversions = getConversionValueAtDate(date, fxRatesResponse, toCurrency);
        if (conversions == null) {
            return null;
        }
        Double conversionValue = conversions.get(toCurrency);
        return conversionValue;
    }

    private static Map<String, Double> getConversionValueAtDate(LocalDate date, FxRatesResponse fxRatesResponse, String toCurrency) {
        Map<String, Double> result = fxRatesResponse.rates.get(date.toString());
        if (result != null && result.containsKey(toCurrency)) {
            return result;
        }
        result = null;
        long minDays = Integer.MAX_VALUE;
        for (var entry : fxRatesResponse.rates.entrySet()) {
            LocalDate entryDate = LocalDate.parse(entry.getKey());
            if (entry.getValue().containsKey(toCurrency) && (result == null || ChronoUnit.DAYS.between(entryDate, date) < minDays)) {
                result = entry.getValue();
                minDays = ChronoUnit.DAYS.between(entryDate, date);
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

}
