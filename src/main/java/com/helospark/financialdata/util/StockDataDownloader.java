package com.helospark.financialdata.util;

import static com.helospark.financialdata.CommonConfig.BASE_FOLDER;
import static java.time.format.DateTimeFormatter.ISO_DATE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.google.common.util.concurrent.RateLimiter;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.CompanyListElement;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.domain.FxSupportedSymbolsResponse;
import com.helospark.financialdata.domain.Profile;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.CapeCalculator;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.DcfCalculator;
import com.helospark.financialdata.service.DividendCalculator;
import com.helospark.financialdata.service.FlagsProviderService;
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

public class StockDataDownloader {
    public static final String SYMBOL_CACHE_FILE = BASE_FOLDER + "/info/symbols/atGlance.json";
    public static final String SYMBOL_CACHE_HISTORY_FILE = BASE_FOLDER + "/info/symbols/";
    static final ObjectMapper objectMapper = new ObjectMapper();
    static final String API_KEY = System.getProperty("API_KEY");
    static final Integer NUM_YEARS = 100;
    static final Integer NUM_QUARTER = NUM_YEARS * 4;
    static final String FX_BASE_FOLDER = BASE_FOLDER + "/fxratefiles";
    static final String BASE_URL = "https://financialmodelingprep.com/api";
    static final int RATE_LIMIT_PER_MINUTE = 250;

    static RateLimiter rateLimiter = RateLimiter.create(RATE_LIMIT_PER_MINUTE / 60.0);

    public static void main(String[] args) {
        init();

        List<String> symbols = Arrays.asList(downloadSimpleUrlCached("/v3/financial-statement-symbol-lists", "info/financial-statement-symbol-lists.json", String[].class));

        List<String> sp500Symbols = downloadCompanyListCached("/v3/sp500_constituent", "info/sp500_constituent.json");
        List<String> nasdaqSymbols = downloadCompanyListCached("/v3/nasdaq_constituent", "info/nasdaq_constituent.json");
        List<String> dowjones_constituent = downloadCompanyListCached("/v3/dowjones_constituent", "info/dowjones_constituent.json");
        downloadFxRates();
        downloadUsefulInfo();

        for (var symbol : sp500Symbols) {
            downloadStockData(symbol);
        }
        for (var symbol : nasdaqSymbols) {
            downloadStockData(symbol);
        }
        for (var symbol : dowjones_constituent) {
            downloadStockData(symbol);
        }
        for (var symbol : symbols) {
            downloadStockData(symbol);
        }

        createExchangeCache();
        createSymbolCache();
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
        LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache = new LinkedHashMap<>();
        Set<Profile> usCompanies = new TreeSet<>((a, b) -> Double.compare(b.mktCap, a.mktCap));
        // sort by most often searched regions
        for (var symbol : DataLoader.provideSymbolsIn(Exchanges.getExchangesByRegion(ExchangeRegion.US))) {
            List<Profile> profiles = DataLoader.readFinancialFile(symbol, "profile.json", Profile.class);
            if (profiles.size() > 0 && !symbol.endsWith("-PL")) {
                usCompanies.add(profiles.get(0));
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
        }
        for (var symbol : DataLoader.provideSymbolsIn(Exchanges.getExchangesByType(MarketType.DEVELOPED_MARKET))) {
            if (!symbolCompanyNameCache.containsKey(symbol)) {
                Optional<AtGlanceData> information = symbolToSearchData(symbol, 0);
                if (information.isPresent()) {
                    symbolCompanyNameCache.put(symbol, information.get());
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
        }
        for (var symbol : DataLoader.provideAllSymbols()) {
            if (!symbolCompanyNameCache.containsKey(symbol)) {
                Optional<AtGlanceData> information = symbolToSearchData(symbol, 0);
                if (information.isPresent()) {
                    symbolCompanyNameCache.put(symbol, information.get());
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

        Map<Integer, Map<String, AtGlanceData>> yearData = new ConcurrentHashMap<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int thread = 0; thread < numberOfThreads; ++thread) {
            futures.add(CompletableFuture.runAsync(() -> {
                while (true) {
                    var entry = queue.poll();
                    if (entry == null) {
                        break;
                    }
                    int queueSize = allSymbolsSet.size() - queue.size();
                    if (queueSize % 1000 == 0) {
                        System.out.println("Progress: " + (((double) queueSize / allSymbolsSet.size())) * 100.0);
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
        LocalDate integerDate = now.minusYears(offsetYeari);
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
        data.eps = financial.incomeStatementTtm.eps;
        data.pe = Optional.ofNullable(RatioCalculator.calculatePriceToEarningsRatio(financial)).orElse(Double.NaN).floatValue();
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

        data.stockCompensationPerMkt = StockBasedCompensationCalculator.stockBasedCompensationPerMarketCap(financial).floatValue();

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
        downloadUrlIfNeeded("info/s&p500_price.json", "/v3/historical-price-full/%5EGSPC", Map.of("serietype", "line"));
        // https://financialmodelingprep.com/api/v3/historical/sp500_constituent?apikey=API_KEY
        downloadUrlIfNeeded("info/s&p500_historical_constituent.json", "/v3/historical/sp500_constituent", Map.of());
        // https://financialmodelingprep.com/api/v3/historical/dowjones_constituent?apikey=API_KEY
        downloadUrlIfNeeded("info/dowjones_constituent_historical_constituent.json", "/v3/historical/dowjones_constituent", Map.of());

        for (var element : List.of("GDP", "realGDP", "nominalPotentialGDP", "realGDPPerCapita", "federalFunds", "CPI",
                "inflationRate", "inflation", "retailSales", "consumerSentiment", "durableGoods",
                "unemploymentRate", "totalNonfarmPayroll", "initialClaims", "industrialProductionTotalIndex",
                "newPrivatelyOwnedHousingUnitsStartedTotalUnits", "totalVehicleSales", "retailMoneyFunds",
                "smoothedUSRecessionProbabilities", "3MonthOr90DayRatesAndYieldsCertificatesOfDeposit",
                "commercialBankInterestRateOnCreditCardPlansAllAccounts", "30YearFixedRateMortgageAverage",
                "15YearFixedRateMortgageAverage")) {
            downloadUrlIfNeeded("info/" + element + ".json", "/v4/economic", Map.of("name", element, "from", "1920-01-01", "to", LocalDate.now().format(ISO_DATE)));
        }

        //https://financialmodelingprep.com/api/v3/form-thirteen-date/0001035674?apikey=API_KEY
        //https://financialmodelingprep.com/api/v3/form-thirteen/0001067983?date=2022-12-10&apikey=API_KEY
        downloadUrlIfNeeded("info/portfolios/warren_buffett.json", "/v3/form-thirteen/0001067983", Map.of("date", "2022-09-30"));
        downloadUrlIfNeeded("info/portfolios/seth_klarman.json", "/v3/form-thirteen/0001061768", Map.of("date", "2022-09-30"));
        downloadUrlIfNeeded("info/portfolios/li_lu.json", "/v3/form-thirteen/0001709323", Map.of("date", "2022-09-30"));
        downloadUrlIfNeeded("info/portfolios/li_lu.json", "/v3/form-thirteen/0001709323", Map.of("date", "2022-09-30"));

    }

    private static void downloadFxRates() {
        try {
            File symbolsFile = new File(FX_BASE_FOLDER + "/symbols.json");
            if (!symbolsFile.exists()) {
                String symbolsUri2 = "https://api.exchangerate.host/symbols";
                System.out.println(symbolsUri2);
                symbolsFile.getParentFile().mkdirs();
                String data = IOUtils.toString(URI.create(symbolsUri2), StandardCharsets.UTF_8);

                try (FileOutputStream fos = new FileOutputStream(symbolsFile)) {
                    fos.write(data.getBytes());
                }
            }
            FxSupportedSymbolsResponse symbols = objectMapper.readValue(symbolsFile, FxSupportedSymbolsResponse.class);

            Set<String> currencies = symbols.symbols.keySet();

            for (var currency : currencies) {
                LocalDate localDate = LocalDate.of(2000, 1, 1);
                while (localDate.getYear() <= LocalDate.now().getYear()) {
                    File currencyFile = new File(FX_BASE_FOLDER + "/" + currency + "_" + localDate.getYear() + ".json");
                    if (!currencyFile.exists()) {
                        rateLimiter.acquire();
                        String uri = "https://api.exchangerate.host/timeseries?start_date=" + localDate.toString() + "&end_date=" + localDate.plusYears(1).toString() + "&base=" + currency;
                        System.out.println(uri);
                        String data = IOUtils.toString(URI.create(uri), StandardCharsets.UTF_8);

                        try (FileOutputStream fos = new FileOutputStream(currencyFile)) {
                            fos.write(data.getBytes());
                        }
                    }
                    localDate = localDate.plusYears(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void downloadStockData(String symbol) {
        symbol = symbol.replace("^", "%5E");
        Map<String, String> queryMap = new HashMap<>(Map.of("limit", asString(NUM_QUARTER)));

        queryMap.put("period", "quarter");

        //https://financialmodelingprep.com/api/v3/income-statement/AAPL?limit=120&apikey=API_KEY
        downloadUrlIfNeeded("fundamentals/" + symbol + "/income-statement.json", "/v3/income-statement/" + symbol, queryMap);
        //https://financialmodelingprep.com/api/v3/balance-sheet-statement/AAPL?period=quarter&limit=400&apikey=API_KEY
        downloadUrlIfNeeded("fundamentals/" + symbol + "/balance-sheet.json", "/v3/balance-sheet-statement/" + symbol, queryMap);
        // https://financialmodelingprep.com/api/v3/cash-flow-statement/AAPL?limit=120&apikey=API_KEY
        downloadUrlIfNeeded("fundamentals/" + symbol + "/cash-flow.json", "/v3/cash-flow-statement/" + symbol, queryMap);
        // https://financialmodelingprep.com/api/v3/ratios/AAPL?limit=40&apikey=API_KEY
        //        downloadUrlIfNeeded("fundamentals/" + symbol + "/ratios.json", "/v3/ratios/" + symbol, queryMap);
        // https://financialmodelingprep.com/api/v3/enterprise-values/AAPL?limit=40&apikey=API_KEY
        // downloadUrlIfNeeded("fundamentals/" + symbol + "/enterprise-values.json", "/v3/enterprise-values/" + symbol, queryMap);
        // https://financialmodelingprep.com/api/v3/key-metrics/AAPL?limit=40&apikey=API_KEY
        //downloadUrlIfNeeded("fundamentals/" + symbol + "/key-metrics.json", "/v3/key-metrics/" + symbol, queryMap);
        // https://financialmodelingprep.com/api/v3/historical-price-full/AAPL?serietype=line&apikey=API_KEY
        downloadUrlIfNeeded("fundamentals/" + symbol + "/historical-price.json", "/v3/historical-price-full/" + symbol, Map.of("serietype", "line"));
        // https://financialmodelingprep.com/api/v3/profile/AAPL?apikey=API_KEY
        downloadUrlIfNeeded("fundamentals/" + symbol + "/profile.json", "/v3/profile/" + symbol, Map.of());
    }

    private static void init() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JSR310Module());
    }

    private static <T> T downloadSimpleUrlCached(String urlPath, String folder, Class<T> clazz) {
        return downloadCachedUrlInternal(folder, urlPath, clazz, Map.of());
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
                String data = IOUtils.toString(URI.create(fullUri), StandardCharsets.UTF_8);

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

    private static void putToMultiMap(Map<String, List<String>> exchangeToSymbol, String key, String value) {
        List<String> values = exchangeToSymbol.get(key);
        if (values == null) {
            values = new ArrayList<>();
            exchangeToSymbol.put(key, values);
        }
        values.add(value);
    }

}
