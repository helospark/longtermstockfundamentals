package com.helospark.financialdata.util;

import static com.helospark.financialdata.CommonConfig.BASE_FOLDER;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.google.common.util.concurrent.RateLimiter;
import com.helospark.financialdata.domain.BalanceSheet;
import com.helospark.financialdata.domain.CashFlow;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.CompanyListElement;
import com.helospark.financialdata.domain.CurrentPrice;
import com.helospark.financialdata.domain.DateAware;
import com.helospark.financialdata.domain.EconomicPriceElement;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.domain.FxSupportedSymbolsResponse;
import com.helospark.financialdata.domain.HistoricalPrice;
import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.domain.IncomeStatement;
import com.helospark.financialdata.domain.InsiderTradingElement;
import com.helospark.financialdata.domain.InsiderTradingLiveElement;
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
import com.helospark.financialdata.service.MarginCalculator;
import com.helospark.financialdata.service.PietroskyScoreCalculator;
import com.helospark.financialdata.service.ProfitabilityCalculator;
import com.helospark.financialdata.service.RatioCalculator;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.StockBasedCompensationCalculator;
import com.helospark.financialdata.service.TrailingPegCalculator;
import com.helospark.financialdata.service.exchanges.ExchangeRegion;
import com.helospark.financialdata.service.exchanges.Exchanges;
import com.helospark.financialdata.service.exchanges.MarketType;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class StockDataDownloader2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockDataDownloader2.class);
    public static final String SYMBOL_CACHE_FILE = BASE_FOLDER + "/info/symbols/atGlance.json";
    public static final String DOWNLOAD_DATES = BASE_FOLDER + "/info/download-dates.json";
    public static final String SYMBOL_CACHE_HISTORY_FILE = BASE_FOLDER + "/info/symbols/";
    static final ObjectMapper objectMapper = new ObjectMapper();
    static final String API_KEY = System.getProperty("API_KEY");
    static final Integer NUM_YEARS = 100;
    static final Integer NUM_QUARTER = NUM_YEARS * 4;
    static final String FX_BASE_FOLDER = BASE_FOLDER + "/fxratefiles";
    static final String BASE_URL = "https://financialmodelingprep.com/api";
    static final int RATE_LIMIT_PER_MINUTE = 700;

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
        statusMessage = "Downloading symbol list";
        progress = 0.0;
        inProgress = true;

        if (downloadNewData) {
            //List<String> symbols = Arrays.asList(downloadSimpleUrlCached("/v3/financial-statement-symbol-lists", "info/financial-statement-symbol-lists.json", String[].class));
            List<String> symbols = Arrays.asList(downloadSimpleUrlCachedWithoutSaving("/v3/financial-statement-symbol-lists", Map.of(), String[].class));
            List<String> sp500Symbols = downloadCompanyListCached("/v3/sp500_constituent", "info/sp500_constituent.json");
            List<String> nasdaqSymbols = downloadCompanyListCached("/v3/nasdaq_constituent", "info/nasdaq_constituent.json");
            List<String> dowjones_constituent = downloadCompanyListCached("/v3/dowjones_constituent", "info/dowjones_constituent.json");
            statusMessage = "Downloading FX";
            downloadFxRates();
            statusMessage = "Downloading useful info";
            downloadUsefulInfo();

            int threads = 10;
            var executor = Executors.newFixedThreadPool(threads);

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
                putToMultiMap(exchangeToSymbol, "UNKNOWN", symbol);
            }
        }

        if (!inProgress) {
            return;
        }

        for (var entry : exchangeToSymbol.entrySet()) {
            File file = new File(BASE_FOLDER + "/info/exchanges/" + entry.getKey());
            if (!file.exists()) {
                file.getParentFile().mkdirs();

                String valueToWrite = entry.getValue().stream().collect(Collectors.joining("\n"));

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(valueToWrite.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    private static void createSymbolCache() {
        if (!inProgress) {
            return;
        }
        LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache = new LinkedHashMap<>();
        Set<Profile> usCompanies = new TreeSet<>((a, b) -> Double.compare(b.mktCap, a.mktCap));
        long allSymbolSize = DataLoader.provideAllSymbols().size();
        // sort by most often searched regions
        for (var symbol : DataLoader.provideSymbolsIn(Exchanges.getExchangesByRegion(ExchangeRegion.US))) {
            List<Profile> profiles = DataLoader.readFinancialFile(symbol, "profile.json", Profile.class);
            if (profiles.size() > 0 && !symbol.endsWith("-PL")) {
                usCompanies.add(profiles.get(0));
            }
            if (usCompanies.size() % 100 == 0) {
                progress = ((double) usCompanies.size() / allSymbolSize) * 100.0;
                if (!inProgress) {
                    return;
                }
            }
        }
        for (var usCompany : usCompanies) {
            if (!symbolCompanyNameCache.containsKey(usCompany.symbol)) {
                Optional<AtGlanceData> symbolToSearchData = symbolToSearchData(usCompany.symbol, 0);
                if (symbolToSearchData.isPresent()) {
                    symbolCompanyNameCache.put(usCompany.symbol, symbolToSearchData.get());
                }
            }
        }
        for (var symbol : DataLoader.provideSymbolsIn(Exchanges.getExchangesByRegion(ExchangeRegion.US))) {
            if (!symbolCompanyNameCache.containsKey(symbol)) {
                Optional<AtGlanceData> information = symbolToSearchData(symbol, 0);
                if (information.isPresent()) {
                    symbolCompanyNameCache.put(symbol, information.get());
                }
            }
            if (symbolCompanyNameCache.size() % 100 == 0) {
                progress = ((double) symbolCompanyNameCache.size() / allSymbolSize) * 100.0;
                if (!inProgress) {
                    return;
                }
            }
        }
        for (var symbol : DataLoader.provideSymbolsIn(Exchanges.getExchangesByType(MarketType.DEVELOPED_MARKET))) {
            if (!symbolCompanyNameCache.containsKey(symbol)) {
                Optional<AtGlanceData> information = symbolToSearchData(symbol, 0);
                if (information.isPresent()) {
                    symbolCompanyNameCache.put(symbol, information.get());
                }
            }
            if (symbolCompanyNameCache.size() % 100 == 0) {
                progress = ((double) symbolCompanyNameCache.size() / allSymbolSize) * 100.0;
                if (!inProgress) {
                    return;
                }
            }
        }
        for (var symbol : DataLoader.provideSymbolsIn(Exchanges.getExchangesByType(MarketType.DEVELOPING_MARKET))) {
            if (!symbolCompanyNameCache.containsKey(symbol)) {
                Optional<AtGlanceData> information = symbolToSearchData(symbol, 0);
                if (information.isPresent()) {
                    symbolCompanyNameCache.put(symbol, information.get());
                }
            }
            if (symbolCompanyNameCache.size() % 100 == 0) {
                progress = ((double) symbolCompanyNameCache.size() / allSymbolSize) * 100.0;
                if (!inProgress) {
                    return;
                }
            }
        }
        for (var symbol : DataLoader.provideAllSymbols()) {
            if (!symbolCompanyNameCache.containsKey(symbol)) {
                Optional<AtGlanceData> information = symbolToSearchData(symbol, 0);
                if (information.isPresent()) {
                    symbolCompanyNameCache.put(symbol, information.get());
                }
            }
            if (symbolCompanyNameCache.size() % 100 == 0) {
                progress = ((double) symbolCompanyNameCache.size() / allSymbolSize) * 100.0;
                if (!inProgress) {
                    return;
                }
            }
        }

        File file = new File(SYMBOL_CACHE_FILE);
        try {
            objectMapper.writeValue(file, symbolCompanyNameCache);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int numberOfThreads = Runtime.getRuntime().availableProcessors() - 1;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        var allSymbolsSet = DataLoader.provideAllSymbols();

        Queue<String> queue = new ConcurrentLinkedQueue<>();
        queue.addAll(DataLoader.provideAllSymbols());

        statusMessage = "Generate historical data";
        progress = 0.0;

        Map<Integer, Map<String, AtGlanceData>> yearData = new ConcurrentHashMap<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int thread = 0; thread < numberOfThreads; ++thread) {
            futures.add(CompletableFuture.runAsync(() -> {
                while (inProgress) {
                    var entry = queue.poll();
                    if (entry == null) {
                        break;
                    }
                    int queueSize = allSymbolsSet.size() - queue.size();
                    if (queueSize % 1000 == 0) {
                        LOGGER.info("Progress: " + (((double) queueSize / allSymbolsSet.size())) * 100.0);
                        System.out.println("Progress: " + (((double) queueSize / allSymbolsSet.size())) * 100.0);
                        progress = (((double) (allSymbolsSet.size() - queueSize) / allSymbolsSet.size())) * 100.0;
                    }

                    for (int i = 1; i < 35; ++i) {
                        int year = LocalDate.now().minusYears(i).getYear();

                        File offsetFile = getBacktestFileAtYear(year);
                        if (!offsetFile.exists()) {
                            Map<String, AtGlanceData> companyMap = yearData.get(year);
                            if (companyMap == null) {
                                companyMap = new ConcurrentHashMap<>();
                                yearData.put(year, companyMap);
                            }

                            Optional<AtGlanceData> offsetDataOptional = symbolToSearchData(entry, i);
                            if (offsetDataOptional.isPresent()) {
                                AtGlanceData offsetData = offsetDataOptional.get();
                                offsetData.companyName = null;
                                companyMap.put(entry, offsetData);
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
            File backtestFile = getBacktestFileAtYear(entry.getKey());
            if (!backtestFile.exists()) {
                try (GZIPOutputStream fos = new GZIPOutputStream(new FileOutputStream(backtestFile))) {
                    objectMapper.writeValue(fos, entry.getValue());
                    System.out.println(backtestFile.getAbsolutePath() + " written");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static File getBacktestFileAtYear(int year) {
        return new File(SYMBOL_CACHE_HISTORY_FILE + year + ".json.gz");
    }

    public static Optional<AtGlanceData> symbolToSearchData(String symbol, int offsetYeari) {
        AtGlanceData data = new AtGlanceData();
        CompanyFinancials company = DataLoader.readFinancialsWithCacheEnabled(symbol, false);

        LocalDate now = LocalDate.now();
        LocalDate targetDate = LocalDate.of(now.getYear() - offsetYeari, 1, 1);

        if (company.financials.isEmpty() || company.profile == null) {
            return Optional.empty();
        }

        data.companyName = company.profile.companyName;
        data.symbol = company.profile.symbol;

        int index = Helpers.findIndexWithOrBeforeDate(company.financials, targetDate);

        if (index == -1) {
            return Optional.of(data);
        }
        var financial = company.financials.get(index);

        LocalDate actualDate = financial.getDate();
        double offsetYear = (now.getYear() - actualDate.getYear()) + ((now.getDayOfYear() - actualDate.getDayOfYear()) / 365.0);

        double latestPrice = (offsetYeari == 0 ? company.latestPrice : company.financials.get(index).price);
        double latestPriceUsd = (offsetYeari == 0 ? company.latestPriceUsd : company.financials.get(index).priceUsd);

        data.actualDate = actualDate;
        data.marketCapUsd = (latestPriceUsd * financial.incomeStatementTtm.weightedAverageShsOut) / 1_000_000.0;
        data.dividendPaid = (float) (-1.0 * financial.cashFlowTtm.dividendsPaid / financial.incomeStatementTtm.weightedAverageShsOut);
        data.latestStockPrice = latestPrice;
        data.latestStockPriceUsd = latestPriceUsd;
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
        data.pts = (float) RatioCalculator.calculatePriceToSalesRatio(financial, latestPrice);
        data.icr = Optional.ofNullable(RatioCalculator.calculateInterestCoverageRatio(financial)).orElse(Double.NaN).floatValue();
        data.dtoe = (float) RatioCalculator.calculateDebtToEquityRatio(financial);
        data.roe = (float) (RoicCalculator.calculateROE(financial) * 100.0);
        data.fcfPerShare = (double) financial.cashFlowTtm.freeCashFlow / financial.incomeStatementTtm.weightedAverageShsOut;
        data.currentRatio = RatioCalculator.calculateCurrentRatio(financial).orElse(Double.NaN).floatValue();
        data.quickRatio = RatioCalculator.calculateQuickRatio(financial).orElse(Double.NaN).floatValue();

        data.epsGrowth = GrowthCalculator.getEpsGrowthInInterval(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.fcfGrowth = GrowthCalculator.getFcfGrowthInInterval(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.revenueGrowth = GrowthCalculator.getRevenueGrowthInInterval(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.dividendGrowthRate = GrowthCalculator.getDividendGrowthInInterval(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.shareCountGrowth = GrowthCalculator.getShareCountGrowthInInterval(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.netMarginGrowth = MarginCalculator.getNetMarginGrowthRate(company.financials, offsetYear + 5, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.cape = Optional.ofNullable(CapeCalculator.calculateCapeRatioQ(company.financials, 10, index)).orElse(Double.NaN).floatValue();

        data.epsSD = GrowthStandardDeviationCounter.calculateEpsGrowthDeviation(company.financials, 7, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.revSD = GrowthStandardDeviationCounter.calculateRevenueGrowthDeviation(company.financials, 7, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.fcfSD = GrowthStandardDeviationCounter.calculateFcfGrowthDeviation(company.financials, 7, offsetYear + 0).orElse(Double.NaN).floatValue();
        data.epsFcfCorrelation = GrowthCorrelationCalculator.calculateEpsFcfCorrelation(company.financials, offsetYear + 7, offsetYear + 0).orElse(Double.NaN).floatValue();

        data.dividendYield = (float) (DividendCalculator.getDividendYield(company, index) * 100.0);
        data.dividendPayoutRatio = (float) (RatioCalculator.calculatePayoutRatio(financial) * 100.0);
        data.dividendFcfPayoutRatio = (float) (RatioCalculator.calculateFcfPayoutRatio(financial) * 100.0);

        data.profitableYears = ProfitabilityCalculator.calculateNumberOfYearsProfitable(company, offsetYear).map(a -> a.doubleValue()).orElse(Double.NaN).shortValue();
        data.fcfProfitableYears = ProfitabilityCalculator.calculateNumberOfFcfProfitable(company, offsetYear).map(a -> a.doubleValue()).orElse(Double.NaN).shortValue();
        data.stockCompensationPerMkt = StockBasedCompensationCalculator.stockBasedCompensationPerMarketCap(financial).floatValue();
        data.cpxToRev = (float) (((double) financial.cashFlowTtm.capitalExpenditure / financial.incomeStatementTtm.revenue * -1.0) * 100.0);

        data.ideal10yrRevCorrelation = (float) IdealGrowthCorrelationCalculator.calculateRevenueCorrelation(company.financials, 10.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();
        data.ideal10yrEpsCorrelation = (float) IdealGrowthCorrelationCalculator.calculateEpsCorrelation(company.financials, 10.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();
        data.ideal10yrFcfCorrelation = (float) IdealGrowthCorrelationCalculator.calculateFcfCorrelation(company.financials, 10.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();

        data.ideal20yrRevCorrelation = (float) IdealGrowthCorrelationCalculator.calculateRevenueCorrelation(company.financials, 20.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();
        data.ideal20yrEpsCorrelation = (float) IdealGrowthCorrelationCalculator.calculateEpsCorrelation(company.financials, 20.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();
        data.ideal20yrFcfCorrelation = (float) IdealGrowthCorrelationCalculator.calculateFcfCorrelation(company.financials, 20.0 + offsetYear, offsetYear).orElse(Double.NaN).doubleValue();

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
        data.price10Gr = GrowthCalculator.getPriceGrowthWithReinvestedDividendsGrowth(company, offsetYear + 10, offsetYear).orElse(Double.NaN).floatValue();
        data.price15Gr = GrowthCalculator.getPriceGrowthWithReinvestedDividendsGrowth(company, offsetYear + 15, offsetYear).orElse(Double.NaN).floatValue();
        data.price20Gr = GrowthCalculator.getPriceGrowthWithReinvestedDividendsGrowth(company, offsetYear + 20, offsetYear).orElse(Double.NaN).floatValue();

        // margin
        data.grMargin = (float) (RatioCalculator.calculateGrossProfitMargin(financial) * 100.0);
        data.opMargin = (float) (RatioCalculator.calculateOperatingMargin(financial) * 100.0);
        data.fcfMargin = (float) (RatioCalculator.calculateFcfMargin(financial) * 100.0);
        data.opCMargin = (float) (RatioCalculator.calculateOperatingCashflowMargin(financial) * 100.0);

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
                String symbolsUri2 = "https://api.exchangerate.host/symbols";
                System.out.println(symbolsUri2);
                symbolsFile.getParentFile().mkdirs();
                String data = downloadUri(symbolsUri2);

                try (FileOutputStream fos = new FileOutputStream(symbolsFile)) {
                    fos.write(data.getBytes());
                }
            }
            FxSupportedSymbolsResponse symbols = objectMapper.readValue(symbolsFile, FxSupportedSymbolsResponse.class);

            Set<String> currencies = symbols.symbols.keySet();

            int k = 0;
            for (var currency : currencies) {
                if (!inProgress) {
                    return;
                }
                LocalDate localDate = LocalDate.of(2000, 1, 1);
                while (localDate.getYear() <= LocalDate.now().getYear()) {
                    File currencyFile = new File(FX_BASE_FOLDER + "/" + currency + "_" + localDate.getYear() + ".json");
                    if (!currencyFile.exists()) {
                        rateLimiterForFx.acquire();
                        String uri = "https://api.exchangerate.host/timeseries?start_date=" + localDate.toString() + "&end_date=" + localDate.plusYears(1).toString() + "&base=" + currency;
                        System.out.println(uri);
                        String data = downloadUri(uri);

                        try (FileOutputStream fos = new FileOutputStream(currencyFile)) {
                            fos.write(data.getBytes());
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

                    if (Math.abs(ChronoUnit.DAYS.between(lastUpdate, LocalDate.now())) > 10) {
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
                    String uri = "https://api.exchangerate.host/timeseries?start_date=" + localDate.toString() + "&end_date=" + now + "&base=" + currency;
                    System.out.println(uri);
                    String data = downloadUri(uri);

                    try (FileOutputStream fos = new FileOutputStream(currencyFile)) {
                        fos.write(data.getBytes());
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
            if (Math.abs(ChronoUnit.DAYS.between(now, downloadDateData.lastPriceDownload)) > 5 && Math.abs(ChronoUnit.DAYS.between(now, downloadDateData.lastReportDate)) < 500) {
                downloadPricesNeeded = true;
            }
        } else {
            downloadFinancials = true;
            downloadPricesNeeded = true;
            downloadDateData = new DownloadDateData(LocalDate.of(1900, 1, 1), now, now, 90);
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
            }

            //  downloadInsiderTrading("fundamentals/" + symbol, symbol, incomeStatements);

            //            var balanceStatements = ((List<BalanceSheet>) balanceResult.data);
            //            var cashflowStatements = ((List<CashFlow>) cashFlowResult.data);
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

    private static void writeLastAttemptedFile(File lastAttemptedFile, Map<String, DownloadDateData> data) {
        try (FileOutputStream fos = new FileOutputStream(lastAttemptedFile)) {
            fos.write(objectMapper.writeValueAsBytes(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void downloadInsiderTrading(String baseFolder, String symbol, List<IncomeStatement> incomeStatements) {
        // https://financialmodelingprep.com/api/v4/insider-trading?symbol=AAPL&page=0&apikey=API_KEY
        int page = 0;
        String sellFileName = "insider-sells.json";
        String buyFileName = "insider-buys.json";
        File sellFile = new File(baseFolder, sellFileName);
        File buyFile = new File(baseFolder, buyFileName);

        List<InsiderTradingElement> alreadySavedSales = new ArrayList<>();
        List<InsiderTradingElement> alreadySavedBuys = new ArrayList<>();

        if (sellFile.exists()) {
            alreadySavedSales = new ArrayList<>(DataLoader.readListOfClassFromFile(sellFile, InsiderTradingElement.class));
        }
        if (buyFile.exists()) {
            alreadySavedBuys = new ArrayList<>(DataLoader.readListOfClassFromFile(buyFile, InsiderTradingElement.class));
        }

        List<InsiderTradingElement> sales = new ArrayList<>();
        List<InsiderTradingElement> buys = new ArrayList<>();

        boolean finished = false;
        while (true) {
            InsiderTradingLiveElement[] elements = downloadSimpleUrlCachedWithoutSaving("/v4/insider-trading", Map.of("symbol", symbol, "page", String.valueOf(page)),
                    InsiderTradingLiveElement[].class);

            for (var element : elements) {
                InsiderTradingElement insiderTradingElement = new InsiderTradingElement();
                insiderTradingElement.amount = element.price * element.securitiesTransacted;
                insiderTradingElement.date = element.transactionDate;
                insiderTradingElement.only10PercentOwner = "10 percent owner".equals(element.typeOfOwner);

                if (element.transactionType.equals("S-Sale")) {
                    if (!alreadySavedSales.isEmpty() &&
                            alreadySavedSales.get(0).getDate().equals(insiderTradingElement.date) &&
                            Math.abs(alreadySavedSales.get(0).amount - insiderTradingElement.amount) < 1.0) {
                        finished = true;
                        break;
                    }
                    sales.add(insiderTradingElement);
                } else if (element.transactionType.equals("P-Purchase")) {
                    if (!alreadySavedBuys.isEmpty() &&
                            alreadySavedBuys.get(0).getDate().equals(insiderTradingElement.date) &&
                            Math.abs(alreadySavedBuys.get(0).amount - insiderTradingElement.amount) < 1.0) {
                        finished = true;
                        break;
                    }
                    buys.add(insiderTradingElement);
                }
            }

            ++page;
            if (elements.length < 100 || page > 200 || finished) {
                break;
            }
        }
        var salesPerMonth = convertToSameFormatAsIncomeStatements(sales, incomeStatements);
        var buysPerMonth = convertToSameFormatAsIncomeStatements(sales, incomeStatements);
        actuallySaveFile(sellFile, merge(sales, alreadySavedSales));
        actuallySaveFile(buyFile, merge(buys, alreadySavedBuys));
    }

    private static List<InsiderTradingElement> convertToSameFormatAsIncomeStatements(List<InsiderTradingElement> sales, List<IncomeStatement> incomeStatements) {
        List<InsiderTradingElement> result = new ArrayList<>();

        return null;
    }

    private static List<InsiderTradingElement> merge(List<InsiderTradingElement> sales, List<InsiderTradingElement> alreadySavedBuys) {
        List<InsiderTradingElement> result = new ArrayList<>();
        result.addAll(sales);
        result.addAll(alreadySavedBuys);
        return result;
    }

    private static void init() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JSR310Module());
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        restTemplate = new RestTemplate(clientHttpRequestFactory);
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
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
        boolean downloadNeeded = true;
        if (absoluteFile.exists()) {
            try {
                elements = DataLoader.readClassFromFile(absoluteFile, HistoricalPrice.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            lastDate = (elements != null && elements.historical.size() > 0) ? elements.historical.get(0).getDate() : null;
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
            if (lastDate != null && elements != null && elements.historical != null) { // then merge files instead of redownloading everything
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
        }
        return downloadNeeded;
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

        boolean downloadNeeded = true;
        List<? extends DateAware> elements = null;
        if (absoluteFile.exists()) {
            //System.out.println(absoluteFile.getAbsolutePath());
            elements = DataLoader.readListOfClassFromFile(absoluteFile, elementType);
            if (elements.size() > 0) {
                LocalDate lastDate = elements.get(0).getDate();
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
            return new DownloadResult(true, actuallyDownloadAndSaveFile(absoluteFile, uriPath, queryParams, type));
        } else {
            return new DownloadResult(false, elements);
        }
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
            throw new RuntimeException(e);
        }
    }

    public static String downloadUri(String fullUri) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Accept-Encoding", "gzip");
        HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);

        // Create a new RestTemplate instance

        // Add the String message converter

        // Make the HTTP GET request, marshaling the response to a String
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
        values.add(value);
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

    static class DownloadDateData {
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
}
