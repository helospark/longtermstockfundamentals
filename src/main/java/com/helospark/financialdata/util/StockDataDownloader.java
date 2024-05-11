package com.helospark.financialdata.util;

import static com.helospark.financialdata.CommonConfig.BASE_FOLDER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.google.common.util.concurrent.RateLimiter;
import com.helospark.financialdata.domain.ApiLayerCurrencies;
import com.helospark.financialdata.domain.ApiLayerRates;
import com.helospark.financialdata.domain.AuxilaryInformation;
import com.helospark.financialdata.domain.BalanceSheet;
import com.helospark.financialdata.domain.CashFlow;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.CompanyListElement;
import com.helospark.financialdata.domain.CurrentPrice;
import com.helospark.financialdata.domain.DateAware;
import com.helospark.financialdata.domain.EconomicPriceElement;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.domain.FxRatesResponse;
import com.helospark.financialdata.domain.FxSupportedSymbolsResponse;
import com.helospark.financialdata.domain.FxSymbol;
import com.helospark.financialdata.domain.HistoricalPrice;
import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.domain.IncomeStatement;
import com.helospark.financialdata.domain.Profile;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.CapeCalculator;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.DcfCalculator;
import com.helospark.financialdata.service.DividendCalculator;
import com.helospark.financialdata.service.EnterpriseValueCalculator;
import com.helospark.financialdata.service.EverythingMoneyCalculator;
import com.helospark.financialdata.service.FlagsProviderService;
import com.helospark.financialdata.service.GrahamNumberCalculator;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.GrowthCorrelationCalculator;
import com.helospark.financialdata.service.GrowthStandardDeviationCounter;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.IdealGrowthCorrelationCalculator;
import com.helospark.financialdata.service.InvestmentScoreCalculator;
import com.helospark.financialdata.service.MarginCalculator;
import com.helospark.financialdata.service.PietroskyScoreCalculator;
import com.helospark.financialdata.service.ProfitabilityCalculator;
import com.helospark.financialdata.service.RatioCalculator;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.StockBasedCompensationCalculator;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.service.TrailingPegCalculator;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class StockDataDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockDataDownloader.class);
    public static final String SYMBOL_CACHE_FILE = BASE_FOLDER + "/info/symbols/atGlance.kryo.bin";
    public static final String DOWNLOAD_DATES = BASE_FOLDER + "/info/download-dates.json";
    public static final String SYMBOL_CACHE_HISTORY_FILE = BASE_FOLDER + "/info/symbols/";
    static final ObjectMapper objectMapper = new ObjectMapper();
    static final Kryo kryo = new Kryo();
    static final String API_KEY = System.getProperty("API_KEY");
    static final Integer NUM_YEARS = 100;
    static final Integer NUM_QUARTER = NUM_YEARS * 4;
    static final String FX_BASE_FOLDER = BASE_FOLDER + "/fxratefiles";
    static final String BASE_URL = "https://financialmodelingprep.com/api";
    static final int RATE_LIMIT_PER_MINUTE = 300;
    private static final String API_LAYER_API_KEY = System.getProperty("API_LAYER_API_KEY");

    static RateLimiter rateLimiter = RateLimiter.create(RATE_LIMIT_PER_MINUTE / 60.0);
    static RateLimiter rateLimiterForFx = RateLimiter.create(RATE_LIMIT_PER_MINUTE / 30.0);

    static RestTemplate restTemplate;
    static volatile boolean inProgress = false;
    static volatile String statusMessage = "N/A";
    static volatile double progress = Double.NaN;

    static {
        init();
    }

    public static void main(String[] args) throws StreamReadException, DatabindException, IOException {
        boolean downloadNewData = true;
        boolean downloadFx = true;
        boolean downloadJustSp = false;
        statusMessage = "Downloading symbol list";
        progress = 0.0;
        inProgress = true;

        if (downloadNewData) {
            //List<String> symbols = Arrays.asList(downloadSimpleUrlCached("/v3/financial-statement-symbol-lists", "info/financial-statement-symbol-lists.json", String[].class));

            List<String> sp500Symbols = downloadCompanyListCached("/v3/sp500_constituent", "info/sp500_constituent.json");
            List<String> nasdaqSymbols = downloadCompanyListCached("/v3/nasdaq_constituent", "info/nasdaq_constituent.json");
            List<String> dowjones_constituent = downloadCompanyListCached("/v3/dowjones_constituent", "info/dowjones_constituent.json");
            statusMessage = "Downloading FX";
            if (downloadFx) {
                downloadFxRates();
            }
            statusMessage = "Downloading useful info";
            downloadUsefulInfo();

            int threads = 10;
            var executor = Executors.newFixedThreadPool(threads);

            List<String> symbols;

            if (downloadJustSp) {
                symbols = sp500Symbols;
            } else {
                symbols = Arrays.asList(downloadSimpleUrlCachedWithoutSaving("/v3/financial-statement-symbol-lists", Map.of(), String[].class));
            }

            List<String> newSymbols = new ArrayList<>(symbols);
            Collections.shuffle(newSymbols);
            Queue<String> symbolsQueue = new ConcurrentLinkedQueue<>(newSymbols);

            File downloadDates = new File(DOWNLOAD_DATES);

            Map<String, DownloadDateData> symbolToDates = loadDateData(downloadDates);

            statusMessage = "Downloading financial data";
            progress = 0.0;
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; ++i) {
                int threadIndex = i;
                futures.add(CompletableFuture.runAsync(() -> {
                    int k = 0;
                    while (inProgress) {
                        String symbol = symbolsQueue.poll();
                        if (symbol == null) {
                            break;
                        }
                        if (symbol.contains("\n")) {
                            continue;
                        }
                        try {
                            downloadStockData(symbol, symbolToDates);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        ++k;

                        if (threadIndex == 0 && k % 1000 == 0) {
                            writeLastAttemptedFile(downloadDates, symbolToDates);
                        }
                        if (threadIndex == 0 && k % 100 == 0) {
                            progress = (((double) (symbols.size() - symbolsQueue.size()) / symbols.size()) * 100.0);
                        }
                    }
                }, executor));
            }

            for (int i = 0; i < futures.size(); ++i) {
                futures.get(i).join();
            }
            executor.shutdownNow();
            writeLastAttemptedFile(downloadDates, symbolToDates);
        }
        if (!inProgress) {
            return;
        }
        progress = 0.0;
        statusMessage = "Creating exchange cache";
        createExchangeCache();
        statusMessage = "Creating symbol cache";
        createSymbolCache();

        statusMessage = "Finished";
        progress = Double.NaN;
        inProgress = false;
    }

    public static DownloadDateData downloadOneStock(String symbol, SymbolAtGlanceProvider symbolAtGlanceProvider, boolean forceRenew) {
        try {
            File downloadDates = new File(DOWNLOAD_DATES);
            Map<String, DownloadDateData> symbolToDates = loadDateData(downloadDates);
            var lastDownloaded = symbolToDates.remove(symbol);
            downloadStockData(symbol, symbolToDates);
            writeLastAttemptedFile(downloadDates, symbolToDates);
            DataLoader.clearCache(symbol);
            DownloadDateData newDownloaded = symbolToDates.get(symbol);

            if (!lastDownloaded.equals(newDownloaded) || forceRenew) {
                int currentMonth = LocalDate.now().getMonthValue();
                Optional<AtGlanceData> information = symbolToSearchData(symbol, 0, currentMonth);

                if (information.isPresent()) {
                    LinkedHashMap<String, AtGlanceData> companies = new LinkedHashMap<>(symbolAtGlanceProvider.getSymbolCompanyNameCache());
                    companies.put(symbol, information.get());
                    saveSymbolCache(companies);
                    symbolAtGlanceProvider.initCache();
                }
            }

            return newDownloaded;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static void downloadMultiStock(List<String> symbols, SymbolAtGlanceProvider symbolAtGlanceProvider) {
        LinkedHashMap<String, AtGlanceData> companies = new LinkedHashMap<>(symbolAtGlanceProvider.getSymbolCompanyNameCache());
        int currentMonth = LocalDate.now().getMonthValue();
        try {
            for (var symbol : symbols) {
                File downloadDates = new File(DOWNLOAD_DATES);
                Map<String, DownloadDateData> symbolToDates = loadDateData(downloadDates);
                var lastDownloaded = symbolToDates.remove(symbol);
                downloadStockData(symbol, symbolToDates);
                writeLastAttemptedFile(downloadDates, symbolToDates);
                DataLoader.clearCache(symbol);
                DownloadDateData newDownloaded = symbolToDates.get(symbol);

                if (!lastDownloaded.equals(newDownloaded)) {
                    Optional<AtGlanceData> information = symbolToSearchData(symbol, 0, currentMonth);

                    if (information.isPresent()) {
                        companies.put(symbol, information.get());
                    }
                }
            }
            saveSymbolCache(companies);
            symbolAtGlanceProvider.initCache();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static Map<String, DownloadDateData> loadDateData(File downloadDates) throws IOException, StreamReadException, DatabindException {
        Map<String, DownloadDateData> symbolToDates = new ConcurrentHashMap<>();
        try {
            if (downloadDates.exists()) {
                symbolToDates = new ConcurrentHashMap<>(objectMapper.readValue(downloadDates, new TypeReference<Map<String, DownloadDateData>>() {
                }));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return symbolToDates;
    }

    private static <T> T downloadSimpleUrlCached(String urlPath, String folder, Class<T> clazz) {
        return downloadCachedUrlInternal(folder, urlPath, clazz, Map.of());
    }

    private static <T> T downloadSimpleUrlCachedWithoutSaving(String uriPath, Map<String, String> queryParams, Class<T> clazz) {
        try {
            String fullUri = BASE_URL + uriPath + "?apikey=" + API_KEY;
            for (var entry : queryParams.entrySet()) {
                fullUri += ("&" + entry.getKey() + "=" + entry.getValue());
            }

            System.out.println(fullUri);
            rateLimiter.acquire();
            String data = downloadUri(fullUri);
            ;

            return objectMapper.readValue(data, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void createExchangeCache() {
        Map<String, List<String>> exchangeToSymbol = new HashMap<>();
        Map<String, String> exchangeToName = new HashMap<>();
        Set<String> allSymbols = DataLoader.provideAllSymbols();

        for (var symbol : allSymbols) {
            List<Profile> profiles = DataLoader.readFinancialFile(symbol, "profile.json", Profile.class);
            if (profiles.size() > 0 && profiles.get(0).exchangeShortName != null) {
                putToMultiMap(exchangeToSymbol, profiles.get(0).exchangeShortName, symbol);
                exchangeToName.put(profiles.get(0).exchangeShortName, profiles.get(0).exchange);
            } else {
                if (symbol.endsWith(".BD")) {
                    putToMultiMap(exchangeToSymbol, "BUD", symbol);
                } else if (symbol.endsWith(".AX")) {
                    putToMultiMap(exchangeToSymbol, "ASX", symbol);
                } else if (symbol.endsWith(".HK")) {
                    putToMultiMap(exchangeToSymbol, "HKSE", symbol);
                } else {
                    putToMultiMap(exchangeToSymbol, "UNKNOWN", symbol);
                }
            }
        }

        if (!inProgress) {
            return;
        }

        for (var entry : exchangeToSymbol.entrySet()) {
            if (entry == null || entry.getKey().isBlank()) {
                continue;
            }
            File file = new File(BASE_FOLDER + "/info/exchanges/" + entry.getKey());
            file.getParentFile().mkdirs();

            String valueToWrite = entry.getValue().stream().collect(Collectors.joining("\n"));

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(valueToWrite.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static void createSymbolCache() {
        if (!inProgress) {
            return;
        }
        int currentMonth = LocalDate.now().getMonthValue();
        boolean regenerateCurrentCompanyCache = true;
        int numberOfThreads = Runtime.getRuntime().availableProcessors() - 1;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        Set<String> symbols = DataLoader.provideAllSymbols();
        if (regenerateCurrentCompanyCache) {

            ConcurrentHashMap<String, AtGlanceData> companies = new ConcurrentHashMap<String, AtGlanceData>();

            statusMessage = "Generate symbol cache";
            progress = 0.0;
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (var symbol : symbols) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        Optional<AtGlanceData> information = symbolToSearchData(symbol, 0, currentMonth);
                        if (information.isPresent()) {
                            companies.put(symbol, information.get());
                        }
                        progress = (companies.size() / (double) symbols.size()) * 100.0;
                        if (companies.size() % 1000 == 0) {
                            System.out.println("Symbolcache progress: " + progress + "%");
                        }
                    } catch (Exception e) {
                        System.out.println("Unable to load " + symbol);
                        e.printStackTrace();
                    }
                }, executorService);
                futures.add(future);
            }
            for (var future : futures) {
                future.join();
            }

            saveSymbolCache(companies);
        }

        var allSymbolsSet = symbols;

        Queue<String> queue = new ConcurrentLinkedQueue<>();
        queue.addAll(symbols);

        statusMessage = "Generate historical data";
        progress = 0.0;

        Map<YearMonthPair, Map<String, AtGlanceData>> yearData = new ConcurrentHashMap<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int thread = 0; thread < numberOfThreads; ++thread) {
            futures.add(CompletableFuture.runAsync(() -> {
                while (inProgress) {
                    var entry = queue.poll();
                    if (entry == null) {
                        break;
                    }
                    CompanyFinancials company = DataLoader.readFinancialsWithCacheEnabled(entry, false);
                    int queueSize = allSymbolsSet.size() - queue.size();
                    if (queueSize % 1000 == 0) {
                        LOGGER.info("Progress: " + (((double) queueSize / allSymbolsSet.size())) * 100.0);
                        System.out.println("Progress: " + (((double) queueSize / allSymbolsSet.size())) * 100.0);
                        progress = (((double) (allSymbolsSet.size() - queueSize) / allSymbolsSet.size())) * 100.0;
                    }

                    for (int i = 1; i < 35; ++i) {
                        int year = LocalDate.now().minusYears(i).getYear();
                        for (int month = 1; month < 12; month += 3) {
                            File offsetFile = getBacktestFileAtYear(year, month);
                            YearMonthPair mapIndex = YearMonthPair.of(year, month);
                            if (!offsetFile.exists()) {
                                Map<String, AtGlanceData> companyMap = yearData.get(mapIndex);
                                if (companyMap == null) {
                                    companyMap = new ConcurrentHashMap<>();
                                    yearData.put(mapIndex, companyMap);
                                }

                                Optional<AtGlanceData> offsetDataOptional = symbolToSearchData(entry, company, i, month);
                                if (offsetDataOptional.isPresent()) {
                                    AtGlanceData offsetData = offsetDataOptional.get();
                                    offsetData.companyName = null;
                                    companyMap.put(entry, offsetData);
                                }
                            }
                        }
                    }
                }
            }, executorService));
        }
        for (int i = 0; i < futures.size(); ++i) {
            futures.get(i).join();
        }
        if (!inProgress) {
            return;
        }

        try {
            executorService.shutdownNow();
            executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        for (var entry : yearData.entrySet()) {
            File backtestFile = getBacktestFileAtYear(entry.getKey().year, entry.getKey().month);
            if (!backtestFile.exists()) {

                try {
                    Output output = new Output(new FileOutputStream(backtestFile));
                    kryo.writeObject(output, entry.getValue());
                    output.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void saveWithKryo(LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache, Kryo kryo, String filename) {
        Output output;
        try {
            output = new Output(new FileOutputStream(filename));
            kryo.writeObject(output, symbolCompanyNameCache);
            output.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void saveSymbolCache(Map<String, AtGlanceData> companies) {
        TreeSet<AtGlanceData> orderedCompaniesSet = new TreeSet<>((a, b) -> Double.compare(b.marketCapUsd, a.marketCapUsd));
        orderedCompaniesSet.addAll(companies.values());

        LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache = new LinkedHashMap<>();
        for (var element : orderedCompaniesSet) {
            symbolCompanyNameCache.put(element.symbol, element);
        }

        try {
            Output output = new Output(new FileOutputStream(SYMBOL_CACHE_FILE));
            kryo.writeObject(output, symbolCompanyNameCache);
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static File getBacktestFileAtYear(int year, int month) {
        return new File(SYMBOL_CACHE_HISTORY_FILE + year + "-" + month + ".kryo.bin");
    }

    public static Optional<AtGlanceData> symbolToSearchData(String symbol, int offsetYeari, int month) {
        CompanyFinancials company = DataLoader.readFinancialsWithCacheEnabled(symbol, false);

        return symbolToSearchData(symbol, company, offsetYeari, month);
    }

    public static Optional<AtGlanceData> symbolToSearchData(String symbol, CompanyFinancials company, int offsetYeari, int month) {
        AtGlanceData data = new AtGlanceData();
        LocalDate now = LocalDate.now();
        LocalDate targetDate = LocalDate.of(now.getYear() - offsetYeari, month, 1);

        if (company.financials.isEmpty() || company.profile == null) {
            return Optional.empty();
        }

        data.companyName = company.profile.companyName;
        data.symbol = symbol;

        int index = Helpers.findIndexWithOrBeforeDate(company.financials, targetDate);

        if (index == -1) {
            return Optional.empty();
        }
        var financial = company.financials.get(index);

        LocalDate actualDate = financial.getDate();
        double offsetYear = (now.getYear() - actualDate.getYear()) + ((now.getDayOfYear() - actualDate.getDayOfYear()) / 365.0);

        double latestPrice = (offsetYeari == 0 ? company.latestPrice : company.financials.get(index).price);
        double latestPriceUsd = (offsetYeari == 0 ? company.latestPriceUsd : company.financials.get(index).priceUsd);
        double latestPriceTradingCurrency = (offsetYeari == 0 ? company.latestPriceTradingCurrency : company.financials.get(index).priceTradingCurrency);

        data.actualDate = actualDate;
        data.marketCapUsd = (latestPriceUsd * financial.incomeStatementTtm.weightedAverageShsOut) / 1_000_000.0;
        data.dividendPaid = (float) (-1.0 * financial.cashFlowTtm.dividendsPaid / financial.incomeStatementTtm.weightedAverageShsOut);
        data.latestStockPrice = latestPrice;
        data.latestStockPriceUsd = latestPriceUsd;
        data.latestStockPriceTradingCur = company.latestPriceTradingCurrency;
        data.shareCount = financial.incomeStatementTtm.weightedAverageShsOut;
        data.trailingPeg = TrailingPegCalculator.calculateTrailingPegWithLatestPrice(company, offsetYear, latestPrice).orElse(Double.NaN).floatValue();
        data.roic = (float) (RoicCalculator.calculateRoic(financial) * 100.0);
        data.altman = (float) AltmanZCalculator.calculateAltmanZScore(financial, latestPrice);
        data.pietrosky = PietroskyScoreCalculator.calculatePietroskyScore(company, financial).map(a -> a.doubleValue()).orElse(Double.NaN).floatValue();
        data.sloan = (float) (RatioCalculator.calculateSloanPercent(financial));

        data.eps = financial.incomeStatementTtm.eps;
        data.pe = Optional.ofNullable(RatioCalculator.calculatePriceToEarningsRatio(financial)).orElse(Double.NaN).floatValue();
        data.evToEbitda = (float) (EnterpriseValueCalculator.calculateEv(financial, latestPrice) / financial.incomeStatementTtm.ebitda);
        data.ptb = (float) RatioCalculator.calculatePriceToBookRatio(financial, latestPrice);
        data.priceToGrossProfit = (float) ((latestPrice * financial.incomeStatementTtm.weightedAverageShsOut) / financial.incomeStatementTtm.grossProfit);
        data.pts = (float) RatioCalculator.calculatePriceToSalesRatio(financial, latestPrice);
        data.cape = Optional.ofNullable(CapeCalculator.calculateCapeRatioQ(company.financials, 10, index)).orElse(Double.NaN).floatValue();
        data.icr = Optional.ofNullable(RatioCalculator.calculateInterestCoverageRatio(financial)).orElse(Double.NaN).floatValue();
        data.dtoe = (float) RatioCalculator.calculateDebtToEquityRatio(financial);
        data.roe = (float) (RoicCalculator.calculateROE(financial) * 100.0);
        data.roa = (float) (RoicCalculator.calculateROA(financial) * 100.0);
        data.rota = (float) (RoicCalculator.calculateROTA(financial) * 100.0);
        data.fcfPerShare = (double) financial.cashFlowTtm.freeCashFlow / financial.incomeStatementTtm.weightedAverageShsOut;
        data.currentRatio = RatioCalculator.calculateCurrentRatio(financial).orElse(Double.NaN).floatValue();
        data.quickRatio = RatioCalculator.calculateQuickRatio(financial).orElse(Double.NaN).floatValue();
        data.assetTurnoverRatio = (float) ((double) financial.incomeStatementTtm.revenue / financial.balanceSheet.totalAssets);

        data.epsGrowth = GrowthCalculator.getEpsGrowthInInterval(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.fcfGrowth = GrowthCalculator.getFcfGrowthInInterval(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.revenueGrowth = GrowthCalculator.getRevenueGrowthInInterval(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.dividendGrowthRate = GrowthCalculator.getDividendGrowthInInterval(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.shareCountGrowth = GrowthCalculator.getShareCountGrowthInInterval(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.netMarginGrowth = MarginCalculator.getNetMarginGrowthRate(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.equityGrowth = GrowthCalculator.getEquityPerShareGrowthInInterval(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();

        data.epsGrowth2yr = GrowthCalculator.getEpsGrowthInInterval(company.financials, offsetYear + 2, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.fcfGrowth2yr = GrowthCalculator.getFcfGrowthInInterval(company.financials, offsetYear + 2, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.revenueGrowth2yr = GrowthCalculator.getRevenueGrowthInInterval(company.financials, offsetYear + 2, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.shareCountGrowth2yr = GrowthCalculator.getShareCountGrowthInInterval(company.financials, offsetYear + 2, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.equityGrowth2yr = GrowthCalculator.getEquityPerShareGrowthInInterval(company.financials, offsetYear + 2, offsetYear + 0).orElse(Double.NaN).floatValue();

        data.epsSD = GrowthStandardDeviationCounter.calculateEpsGrowthDeviation(company.financials, 7, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.revSD = GrowthStandardDeviationCounter.calculateRevenueGrowthDeviation(company.financials, 7, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.fcfSD = GrowthStandardDeviationCounter.calculateFcfGrowthDeviation(company.financials, 7, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.epsFcfCorrelation = GrowthCorrelationCalculator.calculateEpsFcfCorrelation(company.financials, offsetYear + 7, offsetYear + 0).orElse(Double.NaN).floatValue();

        data.dividendYield = (float) (DividendCalculator.getDividendYield(company, index) * 100.0);
        data.dividendPayoutRatio = (float) (RatioCalculator.calculatePayoutRatio(financial) * 100.0);
        data.dividendFcfPayoutRatio = (float) (RatioCalculator.calculateFcfPayoutRatio(financial) * 100.0);

        data.tpr = (float) RatioCalculator.calculateTotalPayoutRatio(financial);

        data.profitableYears = ProfitabilityCalculator.calculateNumberOfYearsProfitable(company, offsetYear).map(a -> a.doubleValue()).orElse(Double.NaN).shortValue();
        data.fcfProfitableYears = ProfitabilityCalculator.calculateNumberOfFcfProfitable(company, offsetYear).map(a -> a.doubleValue()).orElse(Double.NaN).shortValue();
        data.stockCompensationPerMkt = StockBasedCompensationCalculator.stockBasedCompensationPerMarketCap(financial).floatValue();
        data.cpxToRev = (float) (((double) financial.cashFlowTtm.capitalExpenditure / financial.incomeStatementTtm.revenue * -1.0) * 100.0);

        data.ideal10yrRevCorrelation = (float) IdealGrowthCorrelationCalculator.calculateRevenueCorrelation(company.financials, 10.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();
        data.ideal10yrEpsCorrelation = (float) IdealGrowthCorrelationCalculator.calculateEpsCorrelation(company.financials, 10.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();
        data.ideal10yrFcfCorrelation = (float) IdealGrowthCorrelationCalculator.calculateFcfCorrelation(company.financials, 10.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();

        /*
        data.ideal20yrRevCorrelation = (float) IdealGrowthCorrelationCalculator.calculateRevenueCorrelation(company.financials, 20.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();
        data.ideal20yrEpsCorrelation = (float) IdealGrowthCorrelationCalculator.calculateEpsCorrelation(company.financials, 20.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();
        data.ideal20yrFcfCorrelation = (float) IdealGrowthCorrelationCalculator.calculateFcfCorrelation(company.financials, 20.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();
        */

        data.fvCalculatorMoS = (float) ((DcfCalculator.doDcfAnalysisRevenueWithDefaultParameters(company, offsetYear).orElse(Double.NaN) / latestPrice - 1.0) * 100.0);
        data.fvCompositeMoS = (float) ((DcfCalculator.doFullDcfAnalysisWithGrowth(company.financials, offsetYear).orElse(Double.NaN) / latestPrice - 1.0) * 100.0);
        data.grahamMoS = (float) ((GrahamNumberCalculator.calculateGrahamNumber(financial).orElse(Double.NaN) / latestPrice - 1.0) * 100.0);

        // EM
        data.fYrPe = EverythingMoneyCalculator.calculateFiveYearPe(company, offsetYear).orElse(Double.NaN).floatValue();
        data.fYrPFcf = EverythingMoneyCalculator.calculateFiveYearFcf(company, offsetYear).orElse(Double.NaN).floatValue();
        data.fYrIncomeGr = EverythingMoneyCalculator.calculate5YearNetIncomeGrowth(company, offsetYear).orElse(Double.NaN).floatValue();
        data.fiveYrRoic = EverythingMoneyCalculator.calculateFiveYearRoic(company, offsetYear).orElse(Double.NaN).floatValue();
        data.ltl5Fcf = EverythingMoneyCalculator.calculateLtlPer5YrFcf(company, offsetYear).orElse(Double.NaN).floatValue();

        // price
        data.price5Gr = GrowthCalculator.getPriceGrowthWithReinvestedDividendsGrowth(company, offsetYear + 5, offsetYear).orElse(Double.NaN).floatValue();
        data.price10Gr = GrowthCalculator.getPriceGrowthWithReinvestedDividendsGrowth(company, offsetYear + 10, offsetYear).orElse(Double.NaN).floatValue();
        data.price15Gr = GrowthCalculator.getPriceGrowthWithReinvestedDividendsGrowth(company, offsetYear + 15, offsetYear).orElse(Double.NaN).floatValue();
        data.price20Gr = GrowthCalculator.getPriceGrowthWithReinvestedDividendsGrowth(company, offsetYear + 20, offsetYear).orElse(Double.NaN).floatValue();

        // margin
        data.grMargin = (float) (RatioCalculator.calculateGrossProfitMargin(financial) * 100.0);
        data.opMargin = (float) (RatioCalculator.calculateOperatingMargin(financial) * 100.0);
        data.fcfMargin = (float) (RatioCalculator.calculateFcfMargin(financial) * 100.0);
        data.opCMargin = (float) (RatioCalculator.calculateOperatingCashflowMargin(financial) * 100.0);
        data.ebitdaMargin = (float) (((double) financial.incomeStatementTtm.ebitda / financial.incomeStatementTtm.revenue) * 100.0);

        data.investmentScore = InvestmentScoreCalculator.calculate(company, offsetYear).orElse(Double.NaN).floatValue();

        List<FlagInformation> flags = FlagsProviderService.giveFlags(company, offsetYear);

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

        data.starFlags = (byte) numStar;
        data.redFlags = (byte) numRed;
        data.yellowFlags = (byte) numYellow;
        data.greenFlags = (byte) numGreen;
        data.dataQualityIssue = company.dataQualityIssue;

        return Optional.of(data);
    }

    private static void downloadUsefulInfo() {
        //https: //financialmodelingprep.com/api/v4/treasury?from=2021-06-30&to=2021-09-30&apikey=API_KEY
        downloadUrlIfNeeded("info/tresury_rates.json", "/v4/treasury", Map.of("from", "1990-01-01", "to", LocalDate.now().toString()));
        // https://financialmodelingprep.com/api/v3/historical-price-full/%5EGSPC?serietype=line&apikey=API_KEY

        //downloadHistoricalJsonUrlIfNeeded("info/s&p500_price.json", "/v3/historical-price-full/%5EGSPC", Map.of("serietype", "line"), 5);

        // https://financialmodelingprep.com/api/v3/historical/sp500_constituent?apikey=API_KEY
        //downloadEconomicJsonUrlIfNeeded("info/s&p500_historical_constituent.json", "/v3/historical/sp500_constituent", Map.of(), 10);
        // https://financialmodelingprep.com/api/v3/historical/dowjones_constituent?apikey=API_KEY
        //downloadUrlIfNeeded("info/dowjones_constituent_historical_constituent.json", "/v3/historical/dowjones_constituent", Map.of());

        downloadEconomicJsonUrlIfNeeded("info/CPI.json", "/v4/economic", Map.of("name", "CPI", "from", "1947-01-01", "to", LocalDate.now().toString()), 35);
        downloadEconomicJsonUrlIfNeeded("info/15YearFixedRateMortgageAverage.json", "/v4/economic",
                Map.of("name", "15YearFixedRateMortgageAverage", "from", "1947-01-01", "to", LocalDate.now().toString()), 35);
        downloadEconomicJsonUrlIfNeeded("info/30YearFixedRateMortgageAverage.json", "/v4/economic",
                Map.of("name", "30YearFixedRateMortgageAverage", "from", "1947-01-01", "to", LocalDate.now().toString()), 35);
        downloadEconomicJsonUrlIfNeeded("info/unemploymentRate.json", "/v4/economic", Map.of("name", "unemploymentRate", "from", "1947-01-01", "to", LocalDate.now().toString()), 35);
        downloadEconomicJsonUrlIfNeeded("info/consumerSentiment.json", "/v4/economic", Map.of("name", "consumerSentiment", "from", "1947-01-01", "to", LocalDate.now().toString()), 35);
        downloadEconomicJsonUrlIfNeeded("info/realGDP.json", "/v4/economic", Map.of("name", "realGDP", "from", "1947-01-01", "to", LocalDate.now().toString()), 95);
        downloadEconomicJsonUrlIfNeeded("info/GDP.json", "/v4/economic", Map.of("name", "GDP", "from", "1947-01-01", "to", LocalDate.now().toString()), 95);

        /*
        for (var element : List.of("GDP", "realGDP", "nominalPotentialGDP", "realGDPPerCapita", "federalFunds", "CPI",
                "inflationRate", "inflation", "retailSales", "consumerSentiment", "durableGoods",
                "unemploymentRate", "totalNonfarmPayroll", "initialClaims", "industrialProductionTotalIndex",
                "newPrivatelyOwnedHousingUnitsStartedTotalUnits", "totalVehicleSales", "retailMoneyFunds",
                "smoothedUSRecessionProbabilities", "3MonthOr90DayRatesAndYieldsCertificatesOfDeposit",
                "commercialBankInterestRateOnCreditCardPlansAllAccounts", "30YearFixedRateMortgageAverage",
                "15YearFixedRateMortgageAverage")) {
            downloadUrlIfNeeded("info/" + element + ".json", "/v4/economic", Map.of("name", element, "from", "1920-01-01", "to", LocalDate.now().format(ISO_DATE)));
        }*/

        //https://financialmodelingprep.com/api/v3/form-thirteen-date/0001035674?apikey=API_KEY
        //https://financialmodelingprep.com/api/v3/form-thirteen/0001067983?date=2022-12-10&apikey=API_KEY
        downloadUrlIfNeeded("info/portfolios/warren_buffett.json", "/v3/form-thirteen/0001067983", Map.of("date", "2022-09-30"));
        downloadUrlIfNeeded("info/portfolios/seth_klarman.json", "/v3/form-thirteen/0001061768", Map.of("date", "2022-09-30"));
        downloadUrlIfNeeded("info/portfolios/li_lu.json", "/v3/form-thirteen/0001709323", Map.of("date", "2022-09-30"));
        downloadUrlIfNeeded("info/portfolios/li_lu.json", "/v3/form-thirteen/0001709323", Map.of("date", "2022-09-30"));

    }

    private static void downloadFxRates() {
        try {
            progress = 0.0;
            File symbolsFile = new File(FX_BASE_FOLDER + "/symbols.json");
            if (!symbolsFile.exists()) {
                String symbolsUri2 = "https://api.apilayer.com/currency_data/list";
                System.out.println(symbolsUri2);
                symbolsFile.getParentFile().mkdirs();
                String data = downloadUriWithHeaders(symbolsUri2, Map.of("apikey", API_LAYER_API_KEY));
                ApiLayerCurrencies apiLayerCurrencies = objectMapper.readValue(data, ApiLayerCurrencies.class);

                FxSupportedSymbolsResponse symbolsFileData = convertApiLayerCurrencyToFxSupportedCurrencies(apiLayerCurrencies);

                objectMapper.writeValue(symbolsFile, symbolsFileData);
            }
            FxSupportedSymbolsResponse symbols = objectMapper.readValue(symbolsFile, FxSupportedSymbolsResponse.class);

            Set<String> currencies = symbols.symbols.keySet();

            int k = 0;
            for (var currency : currencies) {
                if (!inProgress) {
                    return;
                }
                LocalDate localDate = LocalDate.of(2000, 1, 1);
                while (localDate.getYear() < LocalDate.now().getYear()) {
                    File currencyFile = new File(FX_BASE_FOLDER + "/" + currency + "_" + localDate.getYear() + ".json");
                    if (!currencyFile.exists()) {
                        rateLimiterForFx.acquire();
                        String uri = "https://api.apilayer.com/currency_data/timeframe?start_date=" + localDate.toString() + "&end_date=" + localDate.plusYears(1).toString() + "&source=" + currency;
                        System.out.println(uri);
                        String data = downloadUriWithHeaders(uri, Map.of("apikey", API_LAYER_API_KEY));

                        ApiLayerRates apiLayerRates = objectMapper.readValue(data, ApiLayerRates.class);
                        if (apiLayerRates.success == true) {
                            FxRatesResponse fxRatesResponse = convertApiLayerRatesToFxRates(apiLayerRates);

                            objectMapper.writeValue(currencyFile, fxRatesResponse);
                        }
                    }
                    localDate = localDate.plusYears(1);
                }
                ++k;
                progress = (((double) k / currencies.size()) * 100.0);
            }

            boolean needsUpdate = false;
            File lastUpdatedFile = new File(FX_BASE_FOLDER + "/last_updates.txt");

            if (lastUpdatedFile.exists()) {
                try (FileInputStream fis = new FileInputStream(lastUpdatedFile)) {
                    LocalDate lastUpdate = LocalDate.parse(new String(fis.readAllBytes()));

                    if (Math.abs(ChronoUnit.DAYS.between(lastUpdate, LocalDate.now())) > 30) {
                        needsUpdate = true;
                    }
                }
            } else {
                needsUpdate = true;
            }
            if (needsUpdate) {
                String now = LocalDate.now().toString();
                for (var currency : currencies) {
                    LocalDate localDate = LocalDate.of(LocalDate.now().getYear(), 1, 1);
                    File currencyFile = new File(FX_BASE_FOLDER + "/" + currency + "_" + localDate.getYear() + ".json");
                    rateLimiterForFx.acquire();
                    String uri = "https://api.apilayer.com/currency_data/timeframe?start_date=" + localDate.toString() + "&end_date=" + now + "&source=" + currency;

                    System.out.println(uri);
                    String data = downloadUriWithHeaders(uri, Map.of("apikey", API_LAYER_API_KEY));

                    ApiLayerRates apiLayerRates = objectMapper.readValue(data, ApiLayerRates.class);
                    if (apiLayerRates.success == true) {
                        FxRatesResponse fxRatesResponse = convertApiLayerRatesToFxRates(apiLayerRates);

                        objectMapper.writeValue(currencyFile, fxRatesResponse);
                    }

                }
                try (FileOutputStream fos = new FileOutputStream(lastUpdatedFile)) {
                    fos.write(now.toString().getBytes());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static FxRatesResponse convertApiLayerRatesToFxRates(ApiLayerRates apiLayerRates) {
        FxRatesResponse response = new FxRatesResponse();
        response.success = apiLayerRates.success;
        response.rates = new HashMap<>();
        for (var entry : apiLayerRates.quotes.entrySet()) {
            Map<String, Double> currencyToValuePairsOrig = entry.getValue();
            Map<String, Double> currencyToValuePairsNew = new HashMap<>();
            for (var entry2 : currencyToValuePairsOrig.entrySet()) {
                currencyToValuePairsNew.put(entry2.getKey().substring(3), entry2.getValue());
            }

            response.rates.put(entry.getKey(), currencyToValuePairsNew);
        }

        return response;
    }

    public static FxSupportedSymbolsResponse convertApiLayerCurrencyToFxSupportedCurrencies(ApiLayerCurrencies apiLayerCurrencies) {
        FxSupportedSymbolsResponse symbolsFileData = new FxSupportedSymbolsResponse();
        symbolsFileData.success = apiLayerCurrencies.success;
        symbolsFileData.symbols = new HashMap<>();
        for (var entry : apiLayerCurrencies.currencies.entrySet()) {
            symbolsFileData.symbols.put(entry.getKey(), new FxSymbol(entry.getKey(), entry.getValue()));
        }
        return symbolsFileData;
    }

    private static void downloadStockData(String symbol, Map<String, DownloadDateData> symbolToDates) {
        String originalSymbol = symbol;
        DownloadDateData downloadDateData = symbolToDates.get(originalSymbol);
        LocalDate now = LocalDate.now();

        boolean downloadFinancials = false;
        boolean downloadPricesNeeded = false;
        if (downloadDateData != null) {
            LocalDate expectedDownloadFinancialDate = downloadDateData.lastReportDate.plusDays(downloadDateData.previousReportPeriod);

            if (now.compareTo(expectedDownloadFinancialDate) > 0) {
                if (Math.abs(ChronoUnit.DAYS.between(now, downloadDateData.lastAttemptedDownload)) > 5) {
                    downloadFinancials = true;
                }
                if (Math.abs(ChronoUnit.DAYS.between(now, downloadDateData.lastReportDate)) > 500) {
                    downloadFinancials = false;
                }
            }
            if (Math.abs(ChronoUnit.DAYS.between(now, downloadDateData.lastPriceDownload)) > 10 && Math.abs(ChronoUnit.DAYS.between(now, downloadDateData.lastReportDate)) < 500) {
                downloadPricesNeeded = true;
            }
        } else {
            downloadFinancials = true;
            downloadPricesNeeded = true;
            LocalDate never = LocalDate.of(1900, 1, 1);
            downloadDateData = new DownloadDateData(never, now, never, 90);
        }

        symbol = symbol.replace("^", "%5E");

        if (downloadFinancials) {
            Map<String, String> queryMap = new HashMap<>(Map.of("limit", asString(NUM_QUARTER)));

            queryMap.put("period", "quarter");

            DownloadResult incomeResult = downloadJsonListUrlIfNeededWithoutRetry("fundamentals/" + symbol + "/income-statement.json",
                    "/v3/income-statement/" + symbol, queryMap, IncomeStatement.class, 100);
            var incomeStatements = ((List<IncomeStatement>) incomeResult.data);

            boolean downloadHappened = false;
            if (incomeStatements.size() > 0 && !incomeStatements.get(0).getDate().equals(downloadDateData.lastReportDate)) {
                downloadHappened = true;
            }

            if (downloadHappened) {
                downloadJsonListUrlIfNeededWithoutRetry("fundamentals/" + symbol + "/balance-sheet.json", "/v3/balance-sheet-statement/" + symbol, queryMap,
                        BalanceSheet.class, 100);
                downloadJsonListUrlIfNeededWithoutRetry("fundamentals/" + symbol + "/cash-flow.json", "/v3/cash-flow-statement/" + symbol, queryMap, CashFlow.class,
                        100);
                //downloadAuxilaryInformation(symbol, incomeStatements);
            }

            long numberOfDaysDiff = 30;
            if (incomeStatements.size() > 1) {
                numberOfDaysDiff = Math.abs(ChronoUnit.DAYS.between(incomeStatements.get(1).getDate(), incomeStatements.get(0).getDate()));
                if (numberOfDaysDiff < 90) {
                    numberOfDaysDiff = 90;
                }
                if (numberOfDaysDiff > 370) {
                    numberOfDaysDiff = 370;
                }
            }
            LocalDate lastReportDate = now;
            if (incomeStatements.size() > 0) {
                lastReportDate = incomeStatements.get(0).getDate();
            }
            downloadDateData.lastReportDate = lastReportDate;
            downloadDateData.previousReportPeriod = (int) numberOfDaysDiff;
            downloadDateData.lastAttemptedDownload = now;
        }
        if (downloadPricesNeeded) {
            boolean downloaded = downloadHistoricalJsonUrlIfNeeded("fundamentals/" + symbol + "/historical-price.json", "/v3/historical-price-full/" + symbol, Map.of("serietype", "line"), 10);
            if (downloaded) {
                downloadDateData.lastPriceDownload = now;
            }
        }

        downloadUrlIfNeeded("fundamentals/" + symbol + "/profile.json", "/v3/profile/" + symbol, Map.of());

        symbolToDates.put(originalSymbol, downloadDateData);
    }

    private static void downloadAuxilaryInformation(String symbol, List<IncomeStatement> incomeStatements) {
        JavaType insiderType = objectMapper.getTypeFactory().constructParametricType(List.class, InsiderRoaster.class);
        List<InsiderRoaster> insiders = (List<InsiderRoaster>) actuallyDownloadFileWithotSaving("/v4/insider-roaster-statistic", Map.of("symbol", symbol), insiderType);

        JavaType senateType = objectMapper.getTypeFactory().constructParametricType(List.class, SenateTrading.class);
        List<SenateTrading> senateBuy = (List<SenateTrading>) actuallyDownloadFileWithotSaving("/v4/senate-trading", Map.of("symbol", symbol), senateType);
        List<SenateTrading> houseBuy = (List<SenateTrading>) actuallyDownloadFileWithotSaving("/v4/senate-trading", Map.of("symbol", symbol), senateType);

        // /v4/historical/employee_count
        JavaType employeeCountType = objectMapper.getTypeFactory().constructParametricType(List.class, EmployeeCount.class);
        List<EmployeeCount> employeeCount = (List<EmployeeCount>) actuallyDownloadFileWithotSaving("/v4/historical/employee_count", Map.of("symbol", symbol), employeeCountType);

        // /v3/earnings-surprises/AAPL
        JavaType earningsSurpriseType = objectMapper.getTypeFactory().constructParametricType(List.class, EarningsSurprise.class);
        List<EarningsSurprise> earningsSurprises = (List<EarningsSurprise>) actuallyDownloadFileWithotSaving("/v3/earnings-surprises/" + symbol, Map.of(), earningsSurpriseType);

        List<AuxilaryInformation> auxilaryInfos = incomeStatements.stream().map(a -> {
            var info = new AuxilaryInformation();
            info.date = a.getDate();
            return info;
        }).collect(Collectors.toList());

        for (var insider : insiders) {
            LocalDate date = LocalDate.of(insider.year, (insider.quarter - 1) * 3 + 1, 1);
            int index = Helpers.findIndexWithOrBeforeDate(auxilaryInfos, date);
            if (index != -1) {
                auxilaryInfos.get(index).insiderBoughtShares += insider.totalBought;
                auxilaryInfos.get(index).insiderSoldShares += insider.totalSold;
            }
        }
        boolean firstEmployeeData = true;
        for (var employee : employeeCount) {
            int index = Helpers.findIndexWithOrBeforeDate(auxilaryInfos, employee.periodOfReport);
            if (index != -1) {
                auxilaryInfos.get(index).employeeCount = employee.employeeCount;
                if (firstEmployeeData) {
                    for (int i = index; i >= 0; --i) {
                        auxilaryInfos.get(i).employeeCount = employee.employeeCount;
                    }
                }
            }
            firstEmployeeData = false;
        }
        for (var earningSurprise : earningsSurprises) {
            int index = Helpers.findIndexWithOrBeforeDate(auxilaryInfos, earningSurprise.date);
            if (index != -1 && earningSurprise.actualEarningResult != null && earningSurprise.estimatedEarning != null) {
                auxilaryInfos.get(index).earnSurprisePercent = (int) ((((earningSurprise.actualEarningResult / earningSurprise.estimatedEarning) - 1.0) * 100));
            }
        }
        for (var senateTrade : senateBuy) {
            if (senateTrade.transactionDate != null && senateTrade.amount != null) {
                int index = Helpers.findIndexWithOrBeforeDate(auxilaryInfos, senateTrade.transactionDate);
                if (index != -1 && senateTrade.type != null) {
                    if (senateTrade.type.toLowerCase().contains("purchase")) {
                        auxilaryInfos.get(index).senateBoughtDollar += convertSenateTradeAmount(senateTrade.amount);
                    } else if (senateTrade.type.toLowerCase().contains("sale")) {
                        auxilaryInfos.get(index).senateSoldDollar += convertSenateTradeAmount(senateTrade.amount);
                    }
                }
            }
        }
        for (var houseTrade : houseBuy) {
            if (houseTrade.transactionDate != null && houseTrade.amount != null) {
                int index = Helpers.findIndexWithOrBeforeDate(auxilaryInfos, houseTrade.transactionDate);
                if (index != -1 && houseTrade.type != null) {
                    if (houseTrade.type.toLowerCase().contains("purchase")) {
                        auxilaryInfos.get(index).senateBoughtDollar += convertSenateTradeAmount(houseTrade.amount);
                    } else if (houseTrade.type.toLowerCase().contains("sale")) {
                        auxilaryInfos.get(index).senateSoldDollar += convertSenateTradeAmount(houseTrade.amount);
                    }
                }
            }
        }

        actuallySaveFile(new File(BASE_FOLDER + "/fundamentals/" + symbol + "/auxilary.json"), auxilaryInfos);
    }

    private static int convertSenateTradeAmount(String amount) {
        try {
            String[] dollarRange = amount.replace("$", "").replace(",", "").split(" - ");
            int lowerRange = Integer.parseInt(dollarRange[0]);
            int upperRange = Integer.parseInt(dollarRange[1]);
            return (lowerRange + upperRange) / 2;

        } catch (Exception e) {
            System.out.println("Cannot convert " + amount);
            e.printStackTrace();
            return 0;
        }
    }

    private static void writeLastAttemptedFile(File lastAttemptedFile, Map<String, DownloadDateData> data) {
        try (FileOutputStream fos = new FileOutputStream(lastAttemptedFile)) {
            fos.write(objectMapper.writeValueAsBytes(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void init() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JSR310Module());
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        restTemplate = new RestTemplate(clientHttpRequestFactory);
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        kryo.register(AtGlanceData.class);
        kryo.register(LinkedHashMap.class);
        kryo.register(LocalDate.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.setDefaultSerializer(VersionFieldSerializer.class);
    }

    private static List<String> downloadCompanyListCached(String urlPath, String folder) {
        CompanyListElement[] elements = downloadCachedUrlInternal(folder, urlPath, CompanyListElement[].class, Map.of("limit", asString(NUM_QUARTER)));

        return Arrays.stream(elements)
                .map(e -> e.getSymbol())
                .collect(Collectors.toList());
    }

    private static String asString(Integer numQuarter) {
        return String.valueOf(numQuarter);
    }

    private static <T> T downloadCachedUrlInternal(String folder, String uriPath, Class<T> clazz, Map<String, String> queryParams) {
        File absoluteFile = downloadUrlIfNeeded(folder, uriPath, queryParams);
        try (FileInputStream fis = new FileInputStream(absoluteFile)) {
            return objectMapper.readValue(fis.readAllBytes(), clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static File downloadUrlIfNeeded(String folderAndfile, String uriPath, Map<String, String> queryParams) {
        File result = null;
        int counter = 0;

        while (result == null && counter++ < 3) {
            try {
                result = downloadUrlIfNeededWithoutRetry(folderAndfile, uriPath, queryParams);
            } catch (Exception e) {
                e.printStackTrace();
                noExceptionSleep();
            }
        }
        if (result == null) {
            throw new RuntimeException("Couldn't download file: " + uriPath);
        }
        return result;
    }

    private static void noExceptionSleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static File downloadUrlIfNeededWithoutRetry(String folderAndfile, String uriPath, Map<String, String> queryParams) {
        File absoluteFile = new File(BASE_FOLDER + "/" + folderAndfile);
        if (!absoluteFile.exists()) {
            try {
                absoluteFile.getParentFile().mkdirs();

                String fullUri = BASE_URL + uriPath + "?apikey=" + API_KEY;
                for (var entry : queryParams.entrySet()) {
                    fullUri += ("&" + entry.getKey() + "=" + entry.getValue());
                }

                System.out.println(fullUri);
                rateLimiter.acquire();
                String data = downloadUri(fullUri);

                if (data.contains("Error Message")) {
                    throw new RuntimeException("Couldn't read data " + data);
                }

                try (FileOutputStream fos = new FileOutputStream(absoluteFile)) {
                    fos.write(data.getBytes());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return absoluteFile;
    }

    private static <T> void downloadJsonUrlIfNeeded(String folderAndfile, String uriPath, Map<String, String> queryParams, Class<T> elementType, int updateIntervalInDays,
            Function<T, LocalDate> objectToDateMapper) {
        File absoluteFile = new File(BASE_FOLDER + "/" + folderAndfile);
        JavaType type = objectMapper.getTypeFactory().constructParametricType(elementType, TypeBindings.create(elementType, (JavaType[]) null));

        boolean downloadNeeded = true;
        if (absoluteFile.exists()) {
            T elements = DataLoader.readClassFromFile(absoluteFile, elementType);
            LocalDate lastDate = objectToDateMapper.apply(elements);
            if (lastDate != null) {
                long daysDiff = Math.abs(ChronoUnit.DAYS.between(LocalDate.now(), lastDate));
                if (daysDiff > updateIntervalInDays) {
                    downloadNeeded = true;
                } else {
                    downloadNeeded = false;
                }
            } else {
                downloadNeeded = true;
            }
        }

        if (downloadNeeded) {
            actuallyDownloadAndSaveFile(absoluteFile, uriPath, queryParams, type);
        }
    }

    private static boolean downloadHistoricalJsonUrlIfNeeded(String folderAndfile, String uriPath, Map<String, String> queryParams, int updateIntervalInDays) {
        File absoluteFile = new File(BASE_FOLDER + "/" + folderAndfile);
        JavaType type = objectMapper.getTypeFactory().constructParametricType(HistoricalPrice.class, TypeBindings.create(HistoricalPrice.class, (JavaType[]) null));
        // https://financialmodelingprep.com/api/v3/historical-price-full/AAPL?from=2018-03-12&to=2019-03-12&apikey=API_KEY&serietype=line

        LocalDate lastDate = null;
        HistoricalPrice elements = null;
        if (absoluteFile.exists()) {
            try {
                elements = DataLoader.readClassFromFile(absoluteFile, HistoricalPrice.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            lastDate = (elements != null && elements.historical.size() > 0) ? elements.historical.get(0).getDate() : null;
        }

        boolean mergeFiles = false; // due to splits

        if (lastDate != null && elements != null && elements.historical != null && mergeFiles) { // then merge files instead of redownloading everything
            HashMap<String, String> newQueryParams = new HashMap<>(queryParams);
            newQueryParams.put("from", lastDate.plusDays(1L).toString());
            newQueryParams.put("to", LocalDate.now().toString());

            HistoricalPrice downloadedData = (HistoricalPrice) actuallyDownloadFileAndGet(absoluteFile, uriPath, newQueryParams, type);
            List<HistoricalPriceElement> newElements = new ArrayList<>(downloadedData.historical);
            if (newElements.size() > 0) {
                newElements.addAll(elements.historical);
                downloadedData.historical = newElements;
                actuallySaveFile(absoluteFile, downloadedData);
            }
        } else {
            actuallyDownloadAndSaveFile(absoluteFile, uriPath, queryParams, type);
        }
        return true;
    }

    // TODO: almost same as above, but with different types
    private static void downloadEconomicJsonUrlIfNeeded(String folderAndfile, String uriPath, Map<String, String> queryParams, int updateIntervalInDays) {
        File absoluteFile = new File(BASE_FOLDER + "/" + folderAndfile);
        JavaType type = objectMapper.getTypeFactory().constructParametricType(List.class, EconomicPriceElement.class);

        LocalDate lastDate = null;
        List<EconomicPriceElement> elements = null;
        boolean downloadNeeded = true;
        if (absoluteFile.exists()) {
            elements = DataLoader.readListOfClassFromFile(absoluteFile, EconomicPriceElement.class);
            lastDate = elements.size() > 0 ? elements.get(0).getDate() : null;
            if (lastDate != null) {
                long daysDiff = Math.abs(ChronoUnit.DAYS.between(LocalDate.now(), lastDate));
                if (daysDiff > updateIntervalInDays) {
                    downloadNeeded = true;
                } else {
                    downloadNeeded = false;
                }
            } else {
                downloadNeeded = true;
            }
        }

        if (downloadNeeded) {
            if (lastDate != null && elements != null) { // then merge files instead of redownloading everything
                HashMap<String, String> newQueryParams = new HashMap<>(queryParams);
                newQueryParams.put("from", lastDate.plusDays(1L).toString());
                newQueryParams.put("to", LocalDate.now().toString());

                List<EconomicPriceElement> downloadedData = (List<EconomicPriceElement>) actuallyDownloadFileAndGet(absoluteFile, uriPath, newQueryParams, type);
                List<EconomicPriceElement> newElements = new ArrayList<>(downloadedData);
                newElements.addAll(elements);
                actuallySaveFile(absoluteFile, newElements);
            } else {
                actuallyDownloadAndSaveFile(absoluteFile, uriPath, queryParams, type);
            }
        }
    }

    public static DownloadResult downloadJsonListUrlIfNeededWithoutRetry(String folderAndfile, String uriPath, Map<String, String> queryParams, Class<? extends DateAware> elementType,
            int updateIntervalInDays) {
        File absoluteFile = new File(BASE_FOLDER + "/" + folderAndfile);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);

        return new DownloadResult(true, actuallyDownloadAndSaveFile(absoluteFile, uriPath, queryParams, type));
    }

    public static Object actuallyDownloadAndSaveFile(File absoluteFile, String uriPath, Map<String, String> queryParams, JavaType javaType) {
        Object result = actuallyDownloadFileAndGet(absoluteFile, uriPath, queryParams, javaType);
        actuallySaveFile(absoluteFile, result);
        return result;
    }

    public static void actuallySaveFile(File absoluteFile, Object result) {
        try (FileOutputStream fos = new FileOutputStream(absoluteFile)) {
            objectMapper.writeValue(fos, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object actuallyDownloadFileAndGet(File absoluteFile, String uriPath, Map<String, String> queryParams, JavaType javaType) {
        try {
            absoluteFile.getParentFile().mkdirs();

            String fullUri = BASE_URL + uriPath + "?apikey=" + API_KEY;
            for (var entry : queryParams.entrySet()) {
                fullUri += ("&" + entry.getKey() + "=" + entry.getValue());
            }

            System.out.println(fullUri + " " + absoluteFile.getAbsolutePath());
            rateLimiter.acquire();

            String data = downloadUri(fullUri);

            if (data.contains("Error Message")) {
                throw new RuntimeException("Couldn't read data " + data);
            }

            return objectMapper.readValue(data, javaType); // need to unprettify downloaded data
        } catch (Exception e) {
            throw new RuntimeException("Error loading file, " + absoluteFile, e);
        }
    }

    public static Object actuallyDownloadFileWithotSaving(String uriPath, Map<String, String> queryParams, JavaType javaType) {
        try {
            String fullUri = BASE_URL + uriPath + "?apikey=" + API_KEY;
            for (var entry : queryParams.entrySet()) {
                fullUri += ("&" + entry.getKey() + "=" + entry.getValue());
            }

            rateLimiter.acquire();

            String data = downloadUri(fullUri);

            if (data.contains("Error Message")) {
                throw new RuntimeException("Couldn't read data " + data);
            }

            return objectMapper.readValue(data, javaType); // need to unprettify downloaded data
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String downloadUri(String fullUri) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Accept-Encoding", "gzip");
        HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);

        ResponseEntity<String> response = restTemplate.exchange(fullUri, HttpMethod.GET, requestEntity, String.class);

        String data = response.getBody();
        return data;
    }

    public static String downloadUriWithHeaders(String fullUri, Map<String, String> additionalHeaders) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Accept-Encoding", "gzip");
        for (var entry : additionalHeaders.entrySet()) {
            requestHeaders.put(entry.getKey(), List.of(entry.getValue()));
        }
        HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);

        ResponseEntity<String> response = restTemplate.exchange(fullUri, HttpMethod.GET, requestEntity, String.class);

        String data = response.getBody();
        return data;
    }

    private static void putToMultiMap(Map<String, List<String>> exchangeToSymbol, String key, String value) {
        List<String> values = exchangeToSymbol.get(key);
        if (values == null) {
            values = new ArrayList<>();
            exchangeToSymbol.put(key, values);
        }
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    static class DownloadResult {
        boolean newDataDownloaded;
        Object data;

        public DownloadResult(boolean newDataDownloaded, Object data) {
            this.newDataDownloaded = newDataDownloaded;
            this.data = data;
        }

    }

    public static double loadLatestPrice(String symbol) {
        // https://financialmodelingprep.com/api/v3/quote-short/AAPL?apikey=API_KEY
        return downloadSimpleUrlCachedWithoutSaving("/v3/quote-short/" + symbol, Map.of(), CurrentPrice[].class)[0].price;
    }

    public static class DownloadDateData {
        public LocalDate lastReportDate;
        public LocalDate lastAttemptedDownload;
        public LocalDate lastPriceDownload;
        public int previousReportPeriod;

        public DownloadDateData() {

        }

        public DownloadDateData(LocalDate lastReportDate, LocalDate lastAttemptedDownload, LocalDate lastPriceDownload, int previousReportPeriod) {
            this.lastReportDate = lastReportDate;
            this.lastAttemptedDownload = lastAttemptedDownload;
            this.lastPriceDownload = lastPriceDownload;
            this.previousReportPeriod = previousReportPeriod;
        }

        @Override
        public int hashCode() {
            return Objects.hash(lastPriceDownload, lastReportDate);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DownloadDateData other = (DownloadDateData) obj;
            return Objects.equals(lastPriceDownload, other.lastPriceDownload) && Objects.equals(lastReportDate, other.lastReportDate);
        }

    }

    public static boolean isRunning() {
        return inProgress;
    }

    public static void stop() {
        inProgress = false;
    }

    public static String getStatusMessage() {
        return statusMessage;
    }

    public static double getProgress() {
        // TODO Auto-generated method stub
        return progress;
    }

    public static class YearMonthPair {
        int year;
        int month;

        public YearMonthPair(int year, int month) {
            this.year = year;
            this.month = month;
        }

        public static YearMonthPair of(int year, int month) {
            return new YearMonthPair(year, month);
        }

        @Override
        public int hashCode() {
            return Objects.hash(month, year);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            YearMonthPair other = (YearMonthPair) obj;
            return month == other.month && year == other.year;
        }

        @Override
        public String toString() {
            return "YearMonthPair [year=" + year + ", month=" + month + "]";
        }

    }

    static class InsiderRoaster {
        public int year; //: 2023,
        public int quarter; //"quarter": 3,
        public int totalBought; //": 31896,
        public int totalSold; //": 63792,
    }

    static class SenateTrading {
        public LocalDate transactionDate; //": "2023-08-07",
        public String type; //": "purchase",
        public String amount; //": "$1,001 - $15,000",
    }

    static class EmployeeCount {
        public LocalDate periodOfReport;
        public int employeeCount;
    }

    static class EarningsSurprise {
        public LocalDate date; //": "2023-08-03",
        public Double actualEarningResult;//": 1.26,
        public Double estimatedEarning; //": 1.19
    }
}
