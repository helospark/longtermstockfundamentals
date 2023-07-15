package com.helospark.financialdata.management.inspire;

import static com.helospark.financialdata.CommonConfig.BASE_FOLDER;
import static com.helospark.financialdata.management.config.SymbolLinkBuilder.createSymbolLink;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.InspirationController;
import com.helospark.financialdata.domain.PortfolioElement;
import com.helospark.financialdata.management.config.SymbolLinkBuilder;
import com.helospark.financialdata.management.screener.annotation.AtGlanceFormat;
import com.helospark.financialdata.management.screener.annotation.ScreenerElement;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.analyzer.HighRoicScreener;
import com.helospark.financialdata.util.glance.AtGlanceData;

@Component
public class InspirationProvider {
    private static final String VALUE_LABEL = "Value (m$)";
    @Autowired
    private SymbolAtGlanceProvider symbolIndexProvider;
    Map<String, String> availablePortfolios = new LinkedHashMap<>();

    Cache<String, List<PortfolioElement>> fileToPortfolioElements = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(10)
            .build();
    Cache<String, List<String>> fileToAlgorithmFileElements = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(10)
            .build();

    public InspirationProvider() {
        availablePortfolios.put(InspirationController.PORTFOLIO_BASE_PATH + "warren_buffett", "Warren Buffett");
        availablePortfolios.put(InspirationController.PORTFOLIO_BASE_PATH + "li_lu", "Li Lu");
        availablePortfolios.put(InspirationController.ALGORITHM_BASE_PATH + "Backtest", "Backtest");
    }

    private Inspiration loadPortfolio(String filename, String description) {
        Inspiration result = new Inspiration();
        result.description = description;

        var listOfInvestments = fileToPortfolioElements.get(filename, name -> {
            File file = new File(BASE_FOLDER + name);
            return DataLoader.readListOfClassFromFile(file, PortfolioElement.class);
        });

        result.columns = List.of("symbol", "name", VALUE_LABEL, "Trailing PEG", "ROIC", "AltmanZ", "Revenue Growth", "EPS Growth", "10yr return (%/yr)", "20yr return (%/yr)");

        List<Map<String, String>> portfolioElements = new ArrayList<>();
        for (var element : listOfInvestments) {
            Optional<String> companyName = symbolIndexProvider.getCompanyName(element.tickercusip);
            Optional<AtGlanceData> optionalAtGlance = symbolIndexProvider.getAtGlanceData(element.tickercusip);
            if (symbolIndexProvider.doesCompanyExists(element.tickercusip) && optionalAtGlance.isPresent()) {
                var atGlance = optionalAtGlance.get();
                double priceUsd = atGlance.latestStockPriceUsd;
                Map<String, String> portfolioElement = new HashMap<>();
                portfolioElement.put("symbol", createSymbolLink(element.tickercusip));
                portfolioElement.put("name", companyName.orElse(""));
                portfolioElement.put(VALUE_LABEL, formatString((element.shares * priceUsd / 1_000_000.0)));
                portfolioElement.put("Trailing PEG", formatString(atGlance.trailingPeg));
                portfolioElement.put("ROIC", formatString(atGlance.roic));
                portfolioElement.put("AltmanZ", formatString(atGlance.altman));
                portfolioElement.put("Revenue Growth", formatString(atGlance.revenueGrowth));
                portfolioElement.put("EPS Growth", formatString(atGlance.epsGrowth));
                portfolioElement.put("10yr return (%/yr)", formatString(atGlance.price10Gr));
                portfolioElement.put("20yr return (%/yr)", formatString(atGlance.price20Gr));
                portfolioElements.add(portfolioElement);
            }
        }

        // TODO: don't serialize just to parse again
        portfolioElements.sort((a, b) -> Double.compare(Double.parseDouble(b.get(VALUE_LABEL)), Double.parseDouble(a.get(VALUE_LABEL))));

        result.portfolio = portfolioElements;

        return result;
    }

    public String formatString(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return "-";
        } else {
            return String.format("%.2f", value);
        }
    }

    public String formatString(Double value, int precision) {
        if (value == null || !Double.isFinite(value)) {
            return "-";
        } else {
            return String.format("%." + precision + "f", value);
        }
    }

    public String formatString(Float value) {
        if (value == null || !Float.isFinite(value)) {
            return "-";
        } else {
            return String.format("%.2f", value);
        }
    }

    private Inspiration loadAlgorithm(String fullPath, String description, AccountType type) {
        Inspiration result = new Inspiration();
        result.description = description;

        List<String> lines = fileToAlgorithmFileElements.get(fullPath, path -> {
            File file = new File(fullPath);
            try (var fis = new FileInputStream(file)) {
                return Arrays.asList(new String(fis.readAllBytes()).split("\n"));
            } catch (Exception e) {
                throw new RuntimeException("Unable to load inspiration", e);
            }
        });

        result.columns = Arrays.asList(lines.get(0).split(";"));

        if (type.equals(AccountType.ADVANCED) || type.equals(AccountType.ADMIN)) {
            result.portfolio = new ArrayList<>();
            for (int i = 1; i < lines.size(); ++i) {
                var values = lines.get(i).split(";");
                Map<String, String> elements = new HashMap<>();
                for (int j = 0; j < values.length; ++j) {
                    String columnName = result.columns.get(j);
                    String currentValue = values[j];
                    if (columnName.equalsIgnoreCase("Symbol")) {
                        currentValue = createSymbolLink(currentValue);
                    }
                    elements.put(columnName, currentValue);
                }
                result.portfolio.add(elements);
            }
        } else {
            result.authorizationError = "This algorithm is only available for users with Advanced account";
        }

        return result;
    }

    public Inspiration getAlgorithm(String algorithmName, AccountType type) {
        if (algorithmName.equals("high_roic")) {
            return loadAlgorithm(HighRoicScreener.RESULT_FILE_NAME,
                    "This algorithm filters for stocks with high ROIC (>32%), low trailing PEG (<1.3) "
                            + "and high altmanZ score (>5.2).<br/> <a href=\"/backtests/high_roic.xlsx\">Backtest</a> between 1994 to 2020 (~26yrs) shows that "
                            + "buying and holding stocks returned by this algorithm has beaten the S&P500 91% of the time and "
                            + "on average had 45% higher annualized return than S&P (12.4% vs S&P 8.5%).",
                    type);
        } else if (algorithmName.equals("Backtest")) {
            return loadBacktestResultAlgorithm(
                    "This list of stocks below are the result of many backtests combined.<br/>"
                            + "The way backtest works is by running tests with various fundamental attributes in the past (eg. ROIC > 30 AND TPEG < 1.5 AND ...)"
                            + " and finds which fundamentals done the best over the long term. Using that fundamental list it runs the screener and collect stocks matching the conditions now and put them into a list.<br/>"
                            + " Backtest is ran for many different year-range and beat percents and the tables of each backtests are merged is collected in the table below.<br/>"
                            + "The Backtest count shows the number of time the stock appeared in a S&P500 beating backtest, for example 300 means that 300 different fundamental condition lists (that done well in the past)"
                            + " applies to that stock right now.",
                    type);
        } else {
            throw new InspirationClientException("Portfolio doesn't exist");
        }
    }

    private Inspiration loadBacktestResultAlgorithm(String description, AccountType type) {
        String allStockString = StaticStockList.CUMULATIVE_BACKTEST_RESULT;
        String[] stocks = allStockString.split(", ");

        List<String> columns = List.of("pe", "trailingPeg:tpeg", "altman", "roic", "roe", "dtoe", "grMargin:Gross margin", "opCMargin:OP margin", "shareCountGrowth:Share count growth",
                "price10Gr:10 year growth", "fvCalculatorMoS:Margin of safety");
        LinkedHashMap<String, AtGlanceData> data = symbolIndexProvider.getSymbolCompanyNameCache();

        Inspiration result = new Inspiration();
        result.description = description;
        result.columns = new ArrayList<>();
        result.portfolio = new ArrayList<>();

        result.columns.add("Stock");
        result.columns.add("Backtest count");
        for (var column : columns) {
            if (column.contains(":")) {
                result.columns.add(column.split(":")[1]);
            } else {
                result.columns.add(column);
            }
        }
        result.columns.add("Market cap (bn$)");
        result.columns.add("Full name");

        if (!(type.equals(AccountType.ADVANCED) || type.equals(AccountType.ADMIN))) {
            result.authorizationError = "This algorithm is only available for users with Advanced account";
            return result;
        }
        for (var stock : stocks) {
            LinkedHashMap<String, String> stockMap = new LinkedHashMap<>();
            result.portfolio.add(stockMap);
            String ticker = stock.substring(0, stock.indexOf("("));
            String backtestCount = stock.substring(stock.indexOf("(")).replace("(", "").replace(")", "");
            AtGlanceData atGlance = data.get(ticker);
            if (atGlance != null) {
                stockMap.put("Stock", SymbolLinkBuilder.createSymbolLink(ticker));
                stockMap.put("Backtest count", backtestCount);

                for (var column : columns) {
                    String[] parts = column.split(":");
                    String columnReadableName = "";
                    String columnName = "";
                    if (parts.length == 1) {
                        columnReadableName = parts[0];
                        columnName = parts[0];
                    } else {
                        columnReadableName = parts[1];
                        columnName = parts[0];
                    }
                    String field = getFieldAsString(atGlance, columnName);
                    if (columnName.equals("pe") && field.length() > 5) {
                        field = "-";
                    }
                    if (columnName.startsWith("ro") && field.length() > 8) {
                        field = "-";
                    }
                    stockMap.put(columnReadableName, field);
                }
                stockMap.put("Market cap (bn$)", formatString(atGlance.marketCapUsd / 1000.0, 3));
                stockMap.put("Full name", atGlance.companyName);
            }
        }
        return result;
    }

    public String getFieldAsString(AtGlanceData atGlance, String column) {
        try {
            Field field = AtGlanceData.class.getField(column);
            ScreenerElement screenerElementAnnotation = field.getAnnotation(ScreenerElement.class);
            AtGlanceFormat format = AtGlanceFormat.SIMPLE_NUMBER;
            if (screenerElementAnnotation != null) {
                format = screenerElementAnnotation.format();
            }
            Object value = field.get(atGlance);

            Double result = Double.NaN;

            if (value.getClass().equals(Double.class)) {
                result = (Double) value;
            } else if (value.getClass().equals(Float.class)) {
                result = ((Float) value).doubleValue();
            } else if (value.getClass().equals(Byte.class)) {
                result = ((Byte) value).doubleValue();
            } else if (value.getClass().equals(Integer.class)) {
                result = ((Integer) value).doubleValue();
            } else if (value.getClass().equals(Short.class)) {
                result = ((Short) value).doubleValue();
            }
            return format.format(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "-";
        }
    }

    public Map<String, String> getAvailablePortfolios() {
        return availablePortfolios;
    }

    public Inspiration getPortfolio(String portfolioName) {
        if (portfolioName.equals("li_lu")) {
            return loadPortfolio("/info/portfolios/li_lu.json",
                    "Li Lu's value investment firm, Himalaya Capital Investors, has reportedly produced a 30% compound annual return since inception in 1998, putting this little known investor in the same leagues as the greats, Warren Buffett, Charlie Munger, and Peter Cundill.");
        } else if (portfolioName.equals("warren_buffett")) {
            return loadPortfolio("/info/portfolios/warren_buffett.json",
                    "Warren Buffet's Berkshire shares generated a compound annual return of 20.1% against 10.5% for the S&P 500 (from 1965 through 2021).");
        } else {
            throw new InspirationClientException("Portfolio doesn't exist");
        }
    }

}
