package com.helospark.financialdata.management.watchlist;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.management.user.LoginController;
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
    private static final String PE = "PE";
    private static final String DEBT_TO_EQUITY = "D2E";
    private static final String SHARE_CHANGE = "Share change";
    private static final String ROIC = "ROIC";
    private static final String FIVE_YR_ROIC = "5 yr ROIC";
    private static final String ROE = "ROE";
    private static final String SYMBOL_COL = "Symbol";
    private static final String NAME_COL = "Name";
    private static final String DIFFERENCE_COL = "Margin of safety";
    private static final String OWNED_SHARES = "Owned ($)";
    private static final String ALTMAN = "AltmanZ";
    private static final String PIETROSKY = "Piotrosky";
    private static final String ICR = "ICR";
    private static final String LTL5FCF = "LTL5FCF";
    private static final String GROSS_MARGIN = "Gross margin";
    private static final String OPERATING_MARGIN = "Op margin";
    private static final String REVENUE_GROWTH = "Rev growth";
    private static final String EPS_GROWTH = "EPS growth";
    private static final String FCF_YIELD = "FCF yield";

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

        Portfolio result = new Portfolio();
        result.columns = List.of(SYMBOL_COL, NAME_COL, DIFFERENCE_COL, OWNED_SHARES, PE, ROIC, FIVE_YR_ROIC, ROE, SHARE_CHANGE, DEBT_TO_EQUITY, ICR, ALTMAN, PIETROSKY, LTL5FCF, GROSS_MARGIN,
                OPERATING_MARGIN,
                REVENUE_GROWTH, EPS_GROWTH, FCF_YIELD);

        result.returnsColumns = List.of(SYMBOL_COL, NAME_COL, OWNED_SHARES, "1 year", "2 year", "3 year", "5 year", "10 year", "15 year", "20 year");

        result.portfolio = new ArrayList<>();
        result.returnsPortfolio = new ArrayList<>();
        Map<String, Double> industryToInvestment = new HashMap<>();
        Map<String, Double> sectorToInvestment = new HashMap<>();
        Map<String, Double> capToInvestment = new HashMap<>();
        Map<String, Double> countryToInvestment = new HashMap<>();
        Map<String, Double> profitableToInvestment = new HashMap<>();

        Map<String, CompletableFuture<Double>> prices = new HashMap<>();
        for (int i = 0; i < watchlistElements.size(); ++i) {
            String symbol = watchlistElements.get(i).symbol;
            prices.put(symbol, latestPriceProvider.provideLatestPriceAsync(symbol));
        }

        LocalDate now = LocalDate.now();
        for (int i = 0; i < watchlistElements.size(); ++i) {
            WatchlistElement currentElement = watchlistElements.get(i);
            String ticker = currentElement.symbol;
            Optional<AtGlanceData> optionalAtGlance = symbolIndexProvider.getAtGlanceData(ticker);
            if (symbolIndexProvider.doesCompanyExists(ticker) && optionalAtGlance.isPresent() && (onlyOwned == false || currentElement.ownedShares > 0)) {
                var atGlance = optionalAtGlance.get();
                double latestPriceInTradingCurrency = watchlistService.getPrice(prices, ticker);
                double ownedValue = atGlance.latestStockPriceUsd * currentElement.ownedShares;
                Map<String, String> portfolioElement = new HashMap<>();
                portfolioElement.put(SYMBOL_COL, ticker);
                portfolioElement.put(NAME_COL, Optional.ofNullable(atGlance.companyName).orElse(""));
                portfolioElement.put(DIFFERENCE_COL, formatStringAsPercent(calculateTargetPercent(latestPriceInTradingCurrency, currentElement.targetPrice)));
                portfolioElement.put(OWNED_SHARES, watchlistService.formatString(ownedValue));
                portfolioElement.put(PE, watchlistService.formatString(atGlance.latestStockPrice / atGlance.eps));
                portfolioElement.put(ROIC, formatStringWithThresholdsPercentAsc(atGlance.roic, 5, 10, 20, 30, 40));
                portfolioElement.put(FIVE_YR_ROIC, formatStringWithThresholdsPercentAsc(atGlance.fiveYrRoic, 4, 7, 10, 14, 20));
                portfolioElement.put(ROE, formatStringWithThresholdsPercentAsc(atGlance.roe, 5, 10, 20, 30, 40));
                portfolioElement.put(SHARE_CHANGE, formatStringWithThresholdsPercentDesc(atGlance.shareCountGrowth, 3.0, 2.0, 0.5, 0.0, -3.0));
                portfolioElement.put(DEBT_TO_EQUITY, formatStringWithThresholdsDescNonNegative(atGlance.dtoe, 1.5, 1.0, 0.8, 0.5, 0.1));
                portfolioElement.put(ALTMAN, formatStringWithThresholdsAsc(atGlance.altman, 0.5, 1.5, 2.0, 3.0, 4.0));
                portfolioElement.put(PIETROSKY, formatStringWithThresholdsAsc(atGlance.pietrosky, 3, 4, 5, 6, 7));
                portfolioElement.put(ICR, formatStringWithThresholdsAsc(atGlance.icr, 5, 10, 20, 30, 40));
                portfolioElement.put(LTL5FCF, formatStringWithThresholdsDescNonNegative(atGlance.ltl5Fcf, 20, 10, 7, 4, 2));
                portfolioElement.put(GROSS_MARGIN, formatStringWithThresholdsPercentAsc(atGlance.grMargin, 0, 20, 30, 40, 50));
                portfolioElement.put(OPERATING_MARGIN, formatStringWithThresholdsPercentAsc(atGlance.opMargin, 0, 10, 15, 20, 25));
                portfolioElement.put(REVENUE_GROWTH, formatStringWithThresholdsPercentAsc(atGlance.revenueGrowth, 0, 3, 7, 10, 20));
                portfolioElement.put(EPS_GROWTH, formatStringWithThresholdsPercentAsc(atGlance.epsGrowth, 0, 6, 10, 15, 20));
                portfolioElement.put(FCF_YIELD, formatStringWithThresholdsPercentAsc(atGlance.getFreeCashFlowYield(), 0, 3, 5, 8, 12));

                if (currentElement.calculatorParameters != null) {
                    portfolioElement.put("CALCULATOR_URI", watchlistService.buildCalculatorUri(currentElement.calculatorParameters, ticker));
                }

                result.portfolio.add(portfolioElement);

                CompanyFinancials data = DataLoader.readFinancials(currentElement.symbol);

                Map<String, String> returnsElement = new HashMap<>();

                returnsElement.put(SYMBOL_COL, ticker);
                returnsElement.put(NAME_COL, Optional.ofNullable(atGlance.companyName).orElse(""));
                returnsElement.put(OWNED_SHARES, watchlistService.formatString(ownedValue));
                returnsElement.put("1 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 1 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("2 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 2 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("3 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 3 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("5 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 5 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("10 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 10 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("15 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 15 * 12), -5, 0, 8, 11, 20));
                returnsElement.put("20 year", formatStringWithThresholdsPercentAsc(calculateReturnMonthAgo(data, now, 20 * 12), -5, 0, 8, 11, 20));

                // pie charts
                if (currentElement.ownedShares > 0) {
                    createPieChart(industryToInvestment, data, ownedValue, data.profile.industry);
                    createPieChart(sectorToInvestment, data, ownedValue, data.profile.sector);
                    createPieChart(countryToInvestment, data, ownedValue, data.profile.country);
                    createPieChart(capToInvestment, data, ownedValue, convertToCap(atGlance.marketCapUsd));
                    createPieChart(profitableToInvestment, data, ownedValue, atGlance.eps > 0 ? "profitable" : "non-profitable");
                }

                result.returnsPortfolio.add(returnsElement);
            }
        }

        result.industry = convertToPieChart(industryToInvestment);
        result.sector = convertToPieChart(sectorToInvestment);
        result.cap = convertToPieChart(capToInvestment);
        result.country = convertToPieChart(countryToInvestment);
        result.profitability = convertToPieChart(profitableToInvestment);

        return result;
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

    public String formatStringWithThresholdsAsc(double value, double awfulThreshold, double badThreshold, double neutralThreshold, double goodThreshold, double greatThreshold) {
        if (!Double.isFinite(value)) {
            return "-";
        } else {
            String valueString = String.format("%.2f", value);
            return colorFormatAsc(value, awfulThreshold, badThreshold, neutralThreshold, goodThreshold, greatThreshold, valueString);
        }
    }

    public String formatStringWithThresholdsPercentAsc(double value, double awfulThreshold, double badThreshold, double neutralThreshold, double goodThreshold, double greatThreshold) {
        if (!Double.isFinite(value)) {
            return "-";
        } else {
            String valueString = String.format("%.2f %%", value);
            return colorFormatAsc(value, awfulThreshold, badThreshold, neutralThreshold, goodThreshold, greatThreshold, valueString);
        }
    }

    public String formatStringWithThresholdsPercentDesc(double value, double awfulThreshold, double badThreshold, double neutralThreshold, double goodThreshold, double greatThreshold) {
        if (!Double.isFinite(value)) {
            return "-";
        } else {
            String valueString = String.format("%.2f %%", value);
            return colorFormatDesc(value, awfulThreshold, badThreshold, neutralThreshold, goodThreshold, greatThreshold, valueString);
        }
    }

    public String formatStringWithThresholdsDesc(double value, double awfulThreshold, double badThreshold, double neutralThreshold, double goodThreshold, double greatThreshold) {
        if (!Double.isFinite(value)) {
            return "-";
        } else {
            String valueString = String.format("%.2f", value);
            return colorFormatDesc(value, awfulThreshold, badThreshold, neutralThreshold, goodThreshold, greatThreshold, valueString);
        }
    }

    public String formatStringWithThresholdsDescNonNegative(double value, double awfulThreshold, double badThreshold, double neutralThreshold, double goodThreshold, double greatThreshold) {
        if (!Double.isFinite(value)) {
            return "-";
        } else {
            String valueString = String.format("%.2f", value);
            if (value < 0) {
                return String.format("<div style=\"color:red;  font-weight: 900;\">%s</div>", valueString);
            } else {
                return colorFormatDesc(value, awfulThreshold, badThreshold, neutralThreshold, goodThreshold, greatThreshold, valueString);
            }
        }
    }

    public String colorFormatAsc(double value, double awfulThreshold, double badThreshold, double neutralThreshold, double goodThreshold, double greatThreshold, String valueString) {
        if (value < awfulThreshold) {
            return String.format("<div style=\"color:red;  font-weight: 900;\">%s</div>", valueString);
        } else if (value < badThreshold) {
            return String.format("<div style=\"color:orange; font-weight: 600;\">%s</div>", valueString);
        } else if (value < neutralThreshold) {
            return String.format("<div style=\"color:gray; font-weight: 600;\">%s</div>", valueString);
        } else if (value < goodThreshold) {
            return String.format("<div style=\"color:black; font-weight: 600;\">%s</div>", valueString);
        } else if (value < greatThreshold) {
            return String.format("<div style=\"color:lime; font-weight: 600;\">%s</div>", valueString);
        } else {
            return String.format("<div style=\"color:green; font-weight: 900;\">%s</div>", valueString);
        }
    }

    public String colorFormatDesc(double value, double awfulThreshold, double badThreshold, double neutralThreshold, double goodThreshold, double greatThreshold, String valueString) {
        if (value > awfulThreshold) {
            return String.format("<div style=\"color:red;  font-weight: 900;\">%s</div>", valueString);
        } else if (value > badThreshold) {
            return String.format("<div style=\"color:orange; font-weight: 600;\">%s</div>", valueString);
        } else if (value > neutralThreshold) {
            return String.format("<div style=\"color:gray; font-weight: 600;\">%s</div>", valueString);
        } else if (value > goodThreshold) {
            return String.format("<div style=\"color:black; font-weight: 600;\">%s</div>", valueString);
        } else if (value > greatThreshold) {
            return String.format("<div style=\"color:lime; font-weight: 600;\">%s</div>", valueString);
        } else {
            return String.format("<div style=\"color:green; font-weight: 600;\">%s</div>", valueString);
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
                return String.format("<div style=\"color:red;  font-weight: 600;\">%.2f %%</div>", value);
            } else {
                return String.format("<div style=\"color:green; font-weight: 600;\">%.2f %%</div>", value);
            }
        }
    }
}
