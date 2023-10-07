package com.helospark.financialdata.management.watchlist;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.watchlist.domain.PieChart;
import com.helospark.financialdata.management.watchlist.domain.Portfolio;
import com.helospark.financialdata.management.watchlist.repository.LatestPriceProvider;
import com.helospark.financialdata.management.watchlist.repository.WatchlistElement;
import com.helospark.financialdata.management.watchlist.repository.WatchlistService;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class PortfolioController {
    private static final String MARKET_CAP = "MarketCap";
    private static final String RANK = "Rank";
    private static final String SYMBOL_RAW = "SYMBOL_RAW";
    private static final String PE = "PE";
    private static final String DEBT_TO_EQUITY = "D2E";
    private static final String SHARE_CHANGE = "Δ Shares";
    private static final String ROIC = "ROIC";
    private static final String FIVE_YR_ROIC = "5 yr ROIC";
    private static final String ROE = "ROE";
    private static final String SYMBOL_COL = "Symbol";
    private static final String NAME_COL = "Name";
    private static final String DIFFERENCE_COL = "Margin of safety";
    private static final String OWNED_SHARES = "Owned ($)";
    private static final String ALTMAN = "AltmanZ";
    private static final String PIETROSKY = "Piotrosky";
    private static final String RED_FLAGS = "Red ⚑";
    private static final String ICR = "ICR";
    private static final String LTL5FCF = "LTL5FCF";
    private static final String GROSS_MARGIN = "Gross margin";
    private static final String OPERATING_MARGIN = "Op margin";
    private static final String REVENUE_GROWTH = "Rev growth";
    private static final String EPS_GROWTH = "EPS growth";
    private static final String FCF_YIELD = "FCF yield";

    static final double[] ROIC_RANGES = new double[] { 5, 10, 20, 30, 40 };
    static final double[] ALTMAN_RANGES = new double[] { 0.5, 1.5, 2.0, 3.0, 4.0 };
    static final double[] GROSS_MARGIN_RANGES = new double[] { 0, 20, 30, 40, 50 };
    static final double[] REVENUE_RANGES = new double[] { 0, 3, 7, 10, 20 };
    static final double[] GROWTH_RANGES = new double[] { 0, 6, 10, 15, 20 };
    static final double[] ICR_RANGES = new double[] { 5, 10, 20, 30, 40 };
    static final double[] SHARE_CHANGE_RANGES = new double[] { 3.0, 2.0, 0.5, 0.0, -3.0 };
    static final double[] PE_RANGES = new double[] { 0.0, 7.0, 15.0, 22.0, 30.0 };
    static final double[] PIOTROSKY_RANGES = new double[] { 3, 4, 5, 6, 7 };

    private static final String GOOD_COLOR = "green";
    private static final String OK_COLOR = "lime";
    private static final String SOSO_COLOR = "black";
    private static final String NEUTRAL_COLOR = "gray";
    private static final String BAD_COLOR = "orange";
    private static final String AWEFUL_COLOR = "red";

    @Autowired
    private WatchlistService watchlistService;
    @Autowired
    private LoginController loginController;
    @Autowired
    private SymbolAtGlanceProvider symbolIndexProvider;
    @Autowired
    private LatestPriceProvider latestPriceProvider;

    @GetMapping("/portfoliodata")
    public Portfolio getPortfolio(HttpServletRequest httpRequest, @RequestParam(name = "onlyOwned", defaultValue = "true") boolean onlyOwned) {
        Optional<DecodedJWT> jwt = loginController.getJwt(httpRequest);
        if (jwt.isEmpty()) {
            throw new WatchlistPermissionDeniedException("Not logged in");
        }
        List<WatchlistElement> watchlistElements = watchlistService.readWatchlistFromDb(jwt.get().getSubject());

        Portfolio result = createSummaryTable(onlyOwned, watchlistElements);

        return result;
    }

    @PostMapping("/stocks_summary")
    public Portfolio getSummaryFromStocks(HttpServletRequest httpRequest, @RequestBody SummaryRequest request) {
        Optional<DecodedJWT> jwt = loginController.getJwt(httpRequest);
        if (jwt.isEmpty()) {
            throw new WatchlistPermissionDeniedException("Not logged in");
        }
        if (loginController.getAccountType(jwt.get()) != AccountType.ADMIN) {
            throw new WatchlistPermissionDeniedException("This is only for admins");
        }
        List<WatchlistElement> watchlistElements = watchlistService.readWatchlistFromDb(jwt.get().getSubject());
        Map<String, Integer> stocks = listToSummary(request.data);
        List<WatchlistElement> summaryElements = new ArrayList<>();

        for (var stock : stocks.keySet()) {
            Optional<AtGlanceData> optionalAtGlance = symbolIndexProvider.getAtGlanceData(stock);
            Optional<WatchlistElement> ownedElement = watchlistElements.stream().filter(a -> a.symbol.equals(stock)).findFirst();
            if (optionalAtGlance.isPresent()) {
                AtGlanceData atGlance = optionalAtGlance.get();
                WatchlistElement element = new WatchlistElement();
                element.symbol = stock;
                element.targetPrice = ownedElement.map(a -> a.targetPrice).orElse(atGlance.latestStockPrice * (atGlance.fvCalculatorMoS / 100.0 + 1.0));
                element.ownedShares = (int) (100000.0 / atGlance.latestStockPrice);
                element.calculatorParameters = ownedElement.map(a -> a.calculatorParameters).orElse(null);
                summaryElements.add(element);
            }
        }

        Portfolio result = createSummaryTable(false, summaryElements);

        for (var line : result.portfolio) {
            String symbol = line.get(SYMBOL_RAW);
            AtGlanceData atGlance = symbolIndexProvider.getAtGlanceData(symbol).get();
            Integer listRank = stocks.get(symbol);
            line.put(RANK, listRank + "");
            line.put(MARKET_CAP, watchlistService.formatString(atGlance.marketCapUsd / 1000.0) + "");
        }
        for (var line : result.returnsPortfolio) {
            String symbol = line.get(SYMBOL_RAW);
            Integer listRank = stocks.get(symbol);
            line.put(RANK, listRank + "");
        }
        result.columns = new ArrayList<>(result.columns);
        result.columns.remove(OWNED_SHARES);
        result.columns.add(3, RANK);
        result.columns.add(4, MARKET_CAP);

        result.returnsColumns = new ArrayList<>(result.returnsColumns);
        result.returnsColumns.remove(OWNED_SHARES);
        result.returnsColumns.add(2, RANK);
        return result;
    }

    private Map<String, Integer> listToSummary(String data) {
        String[] stocks = data.split(",");
        Map<String, Integer> result = new LinkedHashMap<>();

        for (var stock : stocks) {
            String trimmedStock = stock.trim();

            int parIndex = trimmedStock.indexOf('(');
            if (parIndex != -1) {
                String key = trimmedStock.substring(0, parIndex).trim();
                Integer value = 1;
                try {
                    value = Integer.parseInt(trimmedStock.substring(parIndex).replace("(", "").replace(")", "").trim());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                result.put(key, value);
            } else {
                result.put(trimmedStock, 1);
            }
        }
        return result;
    }

    public Portfolio createSummaryTable(boolean onlyOwned, List<WatchlistElement> watchlistElements) {
        Portfolio result = new Portfolio();
        result.columns = List.of(SYMBOL_COL, NAME_COL, DIFFERENCE_COL, OWNED_SHARES, PE, ROIC, FIVE_YR_ROIC, ROE, SHARE_CHANGE, DEBT_TO_EQUITY, ICR, LTL5FCF, ALTMAN, PIETROSKY, RED_FLAGS,
                GROSS_MARGIN,
                OPERATING_MARGIN,
                REVENUE_GROWTH, EPS_GROWTH, FCF_YIELD);

        result.returnsColumns = List.of(SYMBOL_COL, NAME_COL, OWNED_SHARES, "1 year", "2 year", "3 year", "5 year", "8 year", "10 year", "12 year", "15 year", "20 year");

        result.portfolio = new ArrayList<>();
        result.returnsPortfolio = new ArrayList<>();
        Map<String, Double> industryToInvestment = new HashMap<>();
        Map<String, Double> sectorToInvestment = new HashMap<>();
        Map<String, Double> capToInvestment = new HashMap<>();
        Map<String, Double> countryToInvestment = new HashMap<>();
        Map<String, Double> profitableToInvestment = new HashMap<>();
        Map<String, Double> investmentsToInvestment = new HashMap<>();
        Map<String, Double> peToInvestment = new HashMap<>();

        Map<String, Double> roicToInvestment = new LinkedHashMap<>();
        Map<String, Double> shareChangeToInvestment = new LinkedHashMap<>();
        Map<String, Double> altmanToInvestment = new LinkedHashMap<>();
        Map<String, Double> growthToInvestment = new LinkedHashMap<>();
        Map<String, Double> icrToInvestment = new LinkedHashMap<>();
        Map<String, Double> grossMToInvestment = new LinkedHashMap<>();
        Map<String, Double> piotroskyToInvestment = new LinkedHashMap<>();

        Map<String, CompletableFuture<Double>> prices = new HashMap<>();
        for (int i = 0; i < watchlistElements.size(); ++i) {
            String symbol = watchlistElements.get(i).symbol;
            prices.put(symbol, latestPriceProvider.provideLatestPriceAsync(symbol));
        }

        initPieChart(roicToInvestment, ROIC_RANGES, true);
        initPieChart(altmanToInvestment, ALTMAN_RANGES, false);
        initPieChart(growthToInvestment, GROWTH_RANGES, true);
        initPieChart(icrToInvestment, ICR_RANGES, false);
        initPieChart(grossMToInvestment, GROSS_MARGIN_RANGES, true);
        initPieChart(peToInvestment, PE_RANGES, false);
        initPieChart(piotroskyToInvestment, PIOTROSKY_RANGES, false);
        initPieChartDesc(shareChangeToInvestment, (SHARE_CHANGE_RANGES), true);

        LocalDate now = LocalDate.now();
        for (int i = 0; i < watchlistElements.size(); ++i) {
            WatchlistElement currentElement = watchlistElements.get(i);
            String ticker = currentElement.symbol;
            Optional<AtGlanceData> optionalAtGlance = symbolIndexProvider.getAtGlanceData(ticker);
            if (symbolIndexProvider.doesCompanyExists(ticker) && optionalAtGlance.isPresent() && (onlyOwned == false || currentElement.ownedShares > 0)) {
                var atGlance = optionalAtGlance.get();
                CompanyFinancials data = DataLoader.readFinancials(currentElement.symbol);
                double latestPriceInTradingCurrency = watchlistService.getPrice(prices, ticker);
                double latestPriceInUsd = DataLoader.convertFx(latestPriceInTradingCurrency, data.profile.currency, "USD", now, false).orElse(atGlance.latestStockPriceUsd);
                double latestPriceInReportingCurrency = DataLoader.convertFx(latestPriceInTradingCurrency, data.profile.currency, data.profile.reportedCurrency, now, false)
                        .orElse(atGlance.latestStockPrice);
                double ownedValue = latestPriceInUsd * currentElement.ownedShares;
                Map<String, String> portfolioElement = new HashMap<>();
                portfolioElement.put(SYMBOL_COL, ticker);
                portfolioElement.put(NAME_COL, Optional.ofNullable(atGlance.companyName).orElse(""));
                portfolioElement.put(DIFFERENCE_COL, formatStringAsPercent(calculateTargetPercent(latestPriceInTradingCurrency, currentElement.targetPrice)));
                portfolioElement.put(OWNED_SHARES, watchlistService.formatString(ownedValue));
                portfolioElement.put(PE, watchlistService.formatString(latestPriceInReportingCurrency / atGlance.eps));
                portfolioElement.put(ROIC, formatStringWithThresholdsPercentAsc(atGlance.roic, ROIC_RANGES));
                portfolioElement.put(FIVE_YR_ROIC, formatStringWithThresholdsPercentAsc(atGlance.fiveYrRoic, 4, 7, 10, 14, 20));
                portfolioElement.put(ROE, formatStringWithThresholdsPercentAsc(atGlance.roe, 5, 10, 20, 30, 40));
                portfolioElement.put(SHARE_CHANGE, formatStringWithThresholdsPercentDesc(atGlance.shareCountGrowth, SHARE_CHANGE_RANGES));
                portfolioElement.put(DEBT_TO_EQUITY, formatStringWithThresholdsDescNonNegative(atGlance.dtoe, 1.5, 1.0, 0.8, 0.5, 0.1));
                portfolioElement.put(ALTMAN, formatStringWithThresholdsAsc(atGlance.altman, ALTMAN_RANGES));
                portfolioElement.put(PIETROSKY, formatStringWithThresholdsAsc(atGlance.pietrosky, PIOTROSKY_RANGES));
                portfolioElement.put(RED_FLAGS, formatStringWithThresholdsDesc(atGlance.redFlags, 2, 1, 1, 1, 0));
                portfolioElement.put(ICR, formatStringWithThresholdsAsc(atGlance.icr, ICR_RANGES));
                portfolioElement.put(LTL5FCF, formatStringWithThresholdsDescNonNegative(atGlance.ltl5Fcf, 20, 10, 7, 4, 2));
                portfolioElement.put(GROSS_MARGIN, formatStringWithThresholdsPercentAsc(atGlance.grMargin, GROSS_MARGIN_RANGES));
                portfolioElement.put(OPERATING_MARGIN, formatStringWithThresholdsPercentAsc(atGlance.opMargin, 0, 10, 15, 20, 25));
                portfolioElement.put(REVENUE_GROWTH, formatStringWithThresholdsPercentAsc(atGlance.revenueGrowth, REVENUE_RANGES));
                portfolioElement.put(EPS_GROWTH, formatStringWithThresholdsPercentAsc(atGlance.epsGrowth, GROWTH_RANGES));
                portfolioElement.put(FCF_YIELD, formatStringWithThresholdsPercentAsc(atGlance.getFreeCashFlowYield(), 0, 3, 5, 8, 12));
                portfolioElement.put(SYMBOL_RAW, ticker);

                if (currentElement.calculatorParameters != null) {
                    portfolioElement.put("CALCULATOR_URI", watchlistService.buildCalculatorUri(currentElement.calculatorParameters, ticker));
                }

                result.portfolio.add(portfolioElement);

                Map<String, String> returnsElement = new HashMap<>();

                returnsElement.put(SYMBOL_COL, ticker);
                returnsElement.put(NAME_COL, Optional.ofNullable(atGlance.companyName).orElse(""));
                returnsElement.put(OWNED_SHARES, watchlistService.formatString(ownedValue));
                returnsElement.put(SYMBOL_RAW, ticker);
                returnsElement.put("1 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 1 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("2 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 2 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("3 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 3 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("5 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 5 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("8 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 8 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("10 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 10 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("12 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 12 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("15 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 15 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("20 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 20 * 12), -5, 0, 8, 11, 20));

                // pie charts
                if (currentElement.ownedShares > 0) {
                    createPieChart(industryToInvestment, data, ownedValue, data.profile.industry);
                    createPieChart(sectorToInvestment, data, ownedValue, data.profile.sector);
                    createPieChart(countryToInvestment, data, ownedValue, data.profile.country);
                    createPieChart(capToInvestment, data, ownedValue, convertToCap(atGlance.marketCapUsd));
                    createPieChart(profitableToInvestment, data, ownedValue, atGlance.eps > 0 ? "profitable" : "non-profitable");
                    createPieChart(investmentsToInvestment, data, ownedValue, ticker);
                    createPieChart(peToInvestment, data, ownedValue, calculateRanges(atGlance.pe, false, PE_RANGES));

                    createPieChart(roicToInvestment, data, ownedValue, calculateRanges(atGlance.roic, true, ROIC_RANGES));
                    createPieChart(altmanToInvestment, data, ownedValue, calculateRanges(atGlance.altman, false, ALTMAN_RANGES));
                    createPieChart(growthToInvestment, data, ownedValue, calculateRanges(atGlance.revenueGrowth, true, GROWTH_RANGES));
                    createPieChart(icrToInvestment, data, ownedValue, calculateRanges(atGlance.icr, false, ICR_RANGES));
                    createPieChart(grossMToInvestment, data, ownedValue, calculateRanges(atGlance.grMargin, true, GROSS_MARGIN_RANGES));
                    createPieChart(piotroskyToInvestment, data, ownedValue, calculateRanges(atGlance.pietrosky, false, PIOTROSKY_RANGES));
                    createPieChart(shareChangeToInvestment, data, ownedValue, calculateRangesDesc(atGlance.shareCountGrowth, true, SHARE_CHANGE_RANGES));
                }

                result.returnsPortfolio.add(returnsElement);
            }
        }

        result.industry = convertToPieChart(industryToInvestment);
        result.sector = convertToPieChart(sectorToInvestment);
        result.cap = convertToPieChart(capToInvestment);
        result.country = convertToPieChart(countryToInvestment);
        result.profitability = convertToPieChart(profitableToInvestment);
        result.investments = convertToPieChart(investmentsToInvestment);
        result.peChart = convertToPieChart(peToInvestment);

        result.roicChart = convertToPieChartWithoutSorting(roicToInvestment);
        result.altmanChart = convertToPieChartWithoutSorting(altmanToInvestment);
        result.growthChart = convertToPieChartWithoutSorting(growthToInvestment);
        result.icrChart = convertToPieChartWithoutSorting(icrToInvestment);
        result.grossMarginChart = convertToPieChartWithoutSorting(grossMToInvestment);
        result.shareChangeChart = convertToPieChartWithoutReverse(shareChangeToInvestment);
        result.piotroskyChart = convertToPieChartWithoutSorting(piotroskyToInvestment);
        return result;
    }

    private void initPieChartDesc(Map<String, Double> roicToInvestment, double[] roicRanges, boolean percent) {
        roicToInvestment.put(calculateRangesDesc(roicRanges[roicRanges.length - 1] - 0.01, percent, roicRanges), 0.0);
        for (int i = roicRanges.length - 1; i >= 0; --i) {
            roicToInvestment.put(calculateRangesDesc(roicRanges[i] - 0.01, percent, roicRanges), 0.0);
        }
    }

    private String calculateRangesDesc(double value, boolean percent, double[] ranges) {
        double a = ranges[0];
        double b = ranges[1];
        double c = ranges[2];
        double d = ranges[3];
        double e = ranges[4];

        String postfix = percent ? "%" : "";
        if (value < e) {
            return "< " + e + postfix;
        } else if (value < d) {
            return "" + e + postfix + " - " + d + postfix + "";
        } else if (value < c) {
            return "" + d + postfix + " - " + c + postfix + "";
        } else if (value < b) {
            return "" + c + postfix + " - " + b + postfix + "";
        } else if (value < b) {
            return "" + b + postfix + " - " + a + postfix + "";
        } else {
            return ">" + a + postfix;
        }
    }

    private void initPieChart(Map<String, Double> roicToInvestment, double[] roicRanges, boolean percent) {
        roicToInvestment.put(calculateRanges(roicRanges[0] - 0.01, percent, roicRanges), 0.0);
        for (double value : roicRanges) {
            roicToInvestment.put(calculateRanges(value + 0.01, percent, roicRanges), 0.0);
        }
    }

    private String calculateRanges(double value, boolean percent, double... ranges) {
        double a = ranges[0];
        double b = ranges[1];
        double c = ranges[2];
        double d = ranges[3];
        double e = ranges[4];

        String postfix = percent ? "%" : "";
        if (value < a) {
            return "< " + a + postfix;
        } else if (value < b) {
            return "" + a + postfix + " - " + b + postfix + "";
        } else if (value < c) {
            return "" + b + postfix + " - " + c + postfix + "";
        } else if (value < d) {
            return "" + c + postfix + " - " + d + postfix + "";
        } else if (value < e) {
            return "" + d + postfix + " - " + e + postfix + "";
        } else {
            return ">" + e + postfix;
        }
    }

    private double calculateReturnMonthAgo(CompanyFinancials data, LocalDate now, int monthAgo) {
        return calculateReturnMonthAgo(data, now, monthAgo, true);
    }

    public double calculateReturnMonthAgo(CompanyFinancials data, LocalDate now, int monthAgo, boolean annualized) {
        int element = Helpers.findIndexWithOrBeforeDate(data.financials, now.minusMonths(monthAgo));
        if (element == -1) {
            return Double.NaN;
        }

        return GrowthCalculator.getPriceGrowthWithReinvestedDividendsGrowth(data, monthAgo / 12.0, 0).orElse(Double.NaN);
    }

    private String convertToCap(double marketCapUsd) {
        if (marketCapUsd < 50) {
            return "Nano-cap";
        } else if (marketCapUsd < 250) {
            return "Micro-cap";
        } else if (marketCapUsd < 2_000) {
            return "Small-cap";
        } else if (marketCapUsd < 10_000) {
            return "Mid-cap";
        } else if (marketCapUsd < 200_000) {
            return "Large-cap";
        } else if (marketCapUsd >= 200_000) {
            return "Mega-cap";
        }
        return "Unknown";
    }

    public void createPieChart(Map<String, Double> map, CompanyFinancials data, double ownedValue, String value) {
        if (value == null) {
            value = "Unknown";
        }

        Double currentValue = map.getOrDefault(value, 0.0);
        map.put(value, currentValue + ownedValue);
    }

    private PieChart convertToPieChart(Map<String, Double> map) {
        ArrayList<Entry<String, Double>> entryList = new ArrayList<>(map.entrySet());

        Collections.sort(entryList, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<String> keys = entryList.stream().map(a -> a.getKey()).collect(Collectors.toList());
        List<Double> values = entryList.stream().map(a -> a.getValue()).collect(Collectors.toList());

        return new PieChart(keys, values);
    }

    private PieChart convertToPieChartWithoutSorting(Map<String, Double> map) {
        ArrayList<Entry<String, Double>> entryList = new ArrayList<>(map.entrySet());

        Collections.reverse(entryList);

        List<String> keys = entryList.stream().map(a -> a.getKey()).collect(Collectors.toList());
        List<Double> values = entryList.stream().map(a -> a.getValue()).collect(Collectors.toList());

        return new PieChart(keys, values);
    }

    private PieChart convertToPieChartWithoutReverse(Map<String, Double> map) {
        ArrayList<Entry<String, Double>> entryList = new ArrayList<>(map.entrySet());

        List<String> keys = entryList.stream().map(a -> a.getKey()).collect(Collectors.toList());
        List<Double> values = entryList.stream().map(a -> a.getValue()).collect(Collectors.toList());

        return new PieChart(keys, values);
    }

    public String formatStringWithThresholdsAsc(double value, double... ranges) {
        if (!Double.isFinite(value)) {
            return "-";
        } else {
            String valueString = String.format("%.2f", value);
            return colorFormatAsc(value, valueString, ranges);
        }
    }

    public String formatStringWithThresholdsPercentAsc(double value, double... ranges) {
        if (!Double.isFinite(value)) {
            return "-";
        } else {
            String valueString = String.format("%.2f %%", value);
            return colorFormatAsc(value, valueString, ranges);
        }
    }

    public String formatStringWithThresholdsPercentDesc(double value, double... ranges) {
        if (!Double.isFinite(value)) {
            return "-";
        } else {
            String valueString = String.format("%.2f %%", value);
            return colorFormatDesc(value, valueString, ranges);
        }
    }

    public String formatStringWithThresholdsDesc(double value, double awfulThreshold, double badThreshold, double neutralThreshold, double goodThreshold, double greatThreshold) {
        if (!Double.isFinite(value)) {
            return "-";
        } else {
            String valueString = String.format("%.2f", value);
            return colorFormatDesc(value, valueString, awfulThreshold, badThreshold, neutralThreshold, goodThreshold, greatThreshold);
        }
    }

    public String formatStringWithThresholdsDescNonNegative(double value, double awfulThreshold, double badThreshold, double neutralThreshold, double goodThreshold, double greatThreshold) {
        if (!Double.isFinite(value)) {
            return "-";
        } else {
            String valueString = String.format("%.2f", value);
            if (value < 0) {
                return String.format("<div style=\"color:" + AWEFUL_COLOR + ";  font-weight: 900;\">%s</div>", valueString);
            } else {
                return colorFormatDesc(value, valueString, awfulThreshold, badThreshold, neutralThreshold, goodThreshold, greatThreshold);
            }
        }
    }

    public String colorFormatAsc(double value, String valueString, double... ranges) {
        double awfulThreshold = ranges[0];
        double badThreshold = ranges[1];
        double neutralThreshold = ranges[2];
        double goodThreshold = ranges[3];
        double greatThreshold = ranges[4];

        if (value < awfulThreshold) {
            return String.format("<div style=\"color:" + AWEFUL_COLOR + ";  font-weight: 900;\">%s</div>", valueString);
        } else if (value < badThreshold) {
            return String.format("<div style=\"color:" + BAD_COLOR + "; font-weight: 600;\">%s</div>", valueString);
        } else if (value < neutralThreshold) {
            return String.format("<div style=\"color:" + NEUTRAL_COLOR + "; font-weight: 600;\">%s</div>", valueString);
        } else if (value < goodThreshold) {
            return String.format("<div style=\"color:" + SOSO_COLOR + "; font-weight: 600;\">%s</div>", valueString);
        } else if (value < greatThreshold) {
            return String.format("<div style=\"color:" + OK_COLOR + "; font-weight: 600;\">%s</div>", valueString);
        } else {
            return String.format("<div style=\"color:" + GOOD_COLOR + "; font-weight: 900;\">%s</div>", valueString);
        }
    }

    public String colorFormatDesc(double value, String valueString, double... ranges) {
        double awfulThreshold = ranges[0];
        double badThreshold = ranges[1];
        double neutralThreshold = ranges[2];
        double goodThreshold = ranges[3];
        double greatThreshold = ranges[4];

        if (value > awfulThreshold) {
            return String.format("<div style=\"color:" + AWEFUL_COLOR + ";  font-weight: 900;\">%s</div>", valueString);
        } else if (value > badThreshold) {
            return String.format("<div style=\"color:" + BAD_COLOR + "; font-weight: 600;\">%s</div>", valueString);
        } else if (value > neutralThreshold) {
            return String.format("<div style=\"color:" + NEUTRAL_COLOR + "; font-weight: 600;\">%s</div>", valueString);
        } else if (value > goodThreshold) {
            return String.format("<div style=\"color:" + SOSO_COLOR + "; font-weight: 600;\">%s</div>", valueString);
        } else if (value > greatThreshold) {
            return String.format("<div style=\"color:" + OK_COLOR + "; font-weight: 600;\">%s</div>", valueString);
        } else {
            return String.format("<div style=\"color:" + GOOD_COLOR + "; font-weight: 600;\">%s</div>", valueString);
        }
    }

    private Double calculateTargetPercent(double latestStockPrice, Double targetPrice) {
        if (targetPrice == null) {
            return null;
        }
        return (targetPrice / latestStockPrice - 1.0) * 100.0;
    }

    private String formatStringAsPercent(Double value) {
        if (value == null) {
            return "-";
        } else {
            if (value < 0) {
                return String.format("<div style=\"color:" + AWEFUL_COLOR + ";  font-weight: 600;\">%.2f %%</div>", value);
            } else {
                return String.format("<div style=\"color:" + GOOD_COLOR + "; font-weight: 600;\">%.2f %%</div>", value);
            }
        }
    }

    static class SummaryRequest {
        public String data = "";
    }
}
