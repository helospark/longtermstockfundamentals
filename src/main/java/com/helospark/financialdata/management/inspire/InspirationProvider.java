package com.helospark.financialdata.management.inspire;

import static com.helospark.financialdata.CommonConfig.BASE_FOLDER;

import java.io.File;
import java.io.FileInputStream;
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
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.SymbolIndexProvider;
import com.helospark.financialdata.util.analyzer.HighRoicScreener;

@Component
public class InspirationProvider {
    @Autowired
    private SymbolIndexProvider symbolIndexProvider;
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
        availablePortfolios.put(InspirationController.ALGORITHM_BASE_PATH + "high_roic", "High ROIC");
    }

    private Inspiration loadPortfolio(String filename, String description) {
        Inspiration result = new Inspiration();
        result.description = description;

        var listOfInvestments = fileToPortfolioElements.get(filename, name -> {
            File file = new File(BASE_FOLDER + name);
            return DataLoader.readListOfClassFromFile(file, PortfolioElement.class);
        });

        result.columns = List.of("symbol", "name", "shares");

        List<Map<String, String>> portfolioElements = new ArrayList<>();
        for (var element : listOfInvestments) {
            Optional<String> company = symbolIndexProvider.getCompanyName(element.tickercusip);
            if (company.isPresent()) {
                Map<String, String> portfolioElement = new HashMap<>();
                portfolioElement.put("symbol", createSymbolLink(element.tickercusip));
                portfolioElement.put("name", company.get());
                portfolioElement.put("shares", String.valueOf(element.shares));
                portfolioElements.add(portfolioElement);
            }
        }
        result.portfolio = portfolioElements;

        return result;
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
            result.authorizationError = "This algorithm is only available for user with Advanced account";
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
        } else {
            throw new InspirationClientException("Portfolio doesn't exist");
        }
    }

    public String createSymbolLink(String ticker) {
        return "<a href=\"/stock/" + ticker + "\">" + ticker + "</a>";
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
