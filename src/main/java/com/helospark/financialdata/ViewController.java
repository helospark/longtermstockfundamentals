package com.helospark.financialdata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.management.inspire.InspirationProvider;
import com.helospark.financialdata.management.screener.ScreenerController;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.ViewedStocksService;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.user.repository.FreeStockRepository;
import com.helospark.financialdata.management.watchlist.repository.LatestPriceProvider;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.MarginCalculator;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.service.exchanges.Exchanges;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/")
public class ViewController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewController.class);
    @Autowired
    private SymbolAtGlanceProvider symbolIndexProvider;
    @Autowired
    private LoginController loginController;
    @Autowired
    private ViewedStocksService viewedStocksService;
    @Autowired
    private FreeStockRepository freeStockRepository;
    @Autowired
    private InspirationProvider inspirationProvider;
    @Autowired
    private ScreenerController screenerController;
    @Autowired
    private LatestPriceProvider latestPriceProvider;

    @GetMapping("/stock/{stock}")
    public String stock(@PathVariable("stock") String stock, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (!symbolIndexProvider.doesCompanyExists(stock)) {
            return "redirect:/?error=Stock not found";
        } else {
            fillModelWithCommonStockData(stock, model, request);
            return "stock";
        }
    }

    @GetMapping("/stock")
    public String stockSearch(Model model, @RequestParam("error") String error) {
        model.addAttribute("error", error);
        return "stock_search";
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        return "faq";
    }

    @GetMapping("/inspire")
    public String inspire(Model model, @RequestParam(defaultValue = "AAPL", name = "stock", required = false) String stock, HttpServletRequest request) {
        fillModelWithCommonStockData(stock, model, request);

        Optional<DecodedJWT> jwtOptional = loginController.getJwt(request);

        model.addAttribute("inspirations", inspirationProvider.getAvailablePortfolios());
        model.addAttribute("allowed", jwtOptional.isPresent());

        return "inspire";
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpServletRequest request) {
        Optional<DecodedJWT> jwtOptional = loginController.getJwt(request);

        if (!jwtOptional.isPresent()) {
            return "redirect:/";
        }
        var jwt = jwtOptional.get();
        AccountType accountType = loginController.getAccountType(jwt);
        int viewLimit = viewedStocksService.getAllowedViewCount(accountType);
        model.addAttribute("viewLimitText", viewLimit == Integer.MAX_VALUE ? "∞" : Integer.toString(viewLimit));
        model.addAttribute("currentlyViewText", viewedStocksService.getViewCount(jwt.getSubject()));

        return "profile";
    }

    @GetMapping("/site-map")
    public String siteMap(Model model) {
        List<Pair> exchanges = new ArrayList<>();
        for (var exchange : Exchanges.values()) {
            exchanges.add(new Pair(exchange.name(), exchange.getName()));
        }
        model.addAttribute("exchanges", exchanges);
        return "sitemap";
    }

    @GetMapping("/screener")
    public String screener(Model model, @RequestParam(defaultValue = "AAPL", name = "stock", required = false) String stock, HttpServletRequest request) {
        Optional<DecodedJWT> jwtOptional = loginController.getJwt(request);

        if (!jwtOptional.isPresent()) {
            model.addAttribute("accountType", "NOT_LOGGED_IN");
            model.addAttribute("allowed", false);
        }
        model.addAttribute("stock", stock);

        model.addAttribute("screenerFields", screenerController.getScreenerDescriptions());
        model.addAttribute("operators", screenerController.getScreenerOperators());
        model.addAttribute("exchanges", Exchanges.getIdToNameMap());
        model.addAttribute("backtestDates", screenerController.getBacktestDates());
        return "screener";
    }

    @GetMapping("/site-map/exchange/{exchange}")
    public String siteMap(Model model, @PathVariable("exchange") String exchangeString) {
        List<Pair> stocks = new ArrayList<>();
        var exchange = Exchanges.fromString(exchangeString);

        Set<String> symbols = DataLoader.provideSymbolsIn(Set.of(exchange));

        for (var symbol : symbols) {
            String name = symbolIndexProvider.getCompanyName(symbol).orElse("");
            stocks.add(new Pair(symbol, name));
        }

        model.addAttribute("stocks", stocks);
        return "sitemap";
    }

    static class Pair {
        public String symbol;
        public String name;

        public Pair(String symbol, String name) {
            this.symbol = symbol;
            this.name = name;
        }

    }

    @GetMapping("/calculator/{stock}")
    public String calculator(@PathVariable("stock") String stock, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes,
            @RequestParam(required = false, name = "startMargin") Double startMarginParam,
            @RequestParam(required = false, name = "endMargin") Double endMarginParam,
            @RequestParam(required = false, name = "startGrowth") Double startGrowthParam,
            @RequestParam(required = false, name = "endGrowth") Double endGrowthParam,
            @RequestParam(required = false, name = "startShareChange") Double startShareChangeParam,
            @RequestParam(required = false, name = "endShareChange") Double endShareChangeParam,
            @RequestParam(required = false, name = "discount") Double discountParam,
            @RequestParam(required = false, name = "endMultiple") Double endMultipleParam) {
        if (!symbolIndexProvider.doesCompanyExists(stock)) {
            redirectAttributes.addAttribute("generalInfo", "stock_not_found");
            return "redirect:/";
        } else {
            fillModelWithCommonStockData(stock, model, request);

            if (Boolean.TRUE.equals(model.getAttribute("allowed"))) {
                CompanyFinancials company = DataLoader.readFinancials(stock);
                if (company.financials.size() > 0) {
                    Double startGrowth = startGrowthParam;
                    if (startGrowth == null) {
                        startGrowth = GrowthCalculator.getMedianRevenueGrowth(company.financials, 8, 0.0).orElse(10.0);
                    }
                    Double startMargin = startMarginParam;
                    if (startMargin == null) {
                        startMargin = MarginCalculator.getAvgNetMargin(company.financials, 0) * 100.0;
                    }

                    Double startShareCountGrowth = startShareChangeParam;
                    if (startShareCountGrowth == null) {
                        startShareCountGrowth = GrowthCalculator.getShareCountGrowthInInterval(company.financials, 5, 0).orElse(0.0);
                    }
                    double endGrowth = nonNullOf(endGrowthParam, startGrowth * 0.5);

                    Double endShareCountGrowth = endShareChangeParam;
                    if (endShareCountGrowth == null) {
                        endShareCountGrowth = startShareCountGrowth;
                    }

                    Double endMultiple = endMultipleParam;
                    if (endMultiple == null) {
                        endMultiple = 12.0;
                        if (endGrowth > 12) {
                            endMultiple = endGrowth;
                        }
                        if (endMultiple > 24) {
                            endMultiple = 24.0;
                        }
                    }
                    Double endMargin = nonNullOf(endMarginParam, startMargin);
                    Double discount = nonNullOf(discountParam, 10.0);

                    model.addAttribute("revenue", (double) company.financials.get(0).incomeStatementTtm.revenue / 1_000_000);
                    model.addAttribute("shareCount", company.financials.get(0).incomeStatementTtm.weightedAverageShsOut / 1000);

                    model.addAttribute("startGrowth", String.format("%.2f", startGrowth));
                    model.addAttribute("endGrowth", String.format("%.2f", endGrowth));
                    model.addAttribute("startMargin", String.format("%.2f", startMargin));
                    model.addAttribute("endMargin", String.format("%.2f", endMargin));
                    model.addAttribute("shareChange", String.format("%.2f", startShareCountGrowth));
                    model.addAttribute("endShareChange", String.format("%.2f", endShareCountGrowth));
                    model.addAttribute("endMultiple", String.format("%.0f", endMultiple));
                    model.addAttribute("discount", String.format("%.0f", discount));
                    var tradingCurrency = Optional.ofNullable(company.profile.currency).orElse("");
                    var reportedCurrency = Optional.ofNullable(company.profile.reportedCurrency).orElse("");
                    if (tradingCurrency.equals(reportedCurrency)) {
                        model.addAttribute("reportingCurrencyToTradingCurrencyRate", 1.0);
                    } else {
                        LocalDate now = LocalDate.now();
                        Optional<Double> exchangeRate = DataLoader.convertFx(1.0, reportedCurrency, tradingCurrency, now, true);
                        if (!exchangeRate.isPresent()) {
                            LOGGER.error("Cannot convert exchange rates {}->{} at date {}", reportedCurrency, tradingCurrency, now);
                        }
                        model.addAttribute("reportingCurrencyToTradingCurrencyRate", exchangeRate.orElse(1.0));
                    }
                }

                double latestPriceInTradingCurrency = latestPriceProvider.provideLatestPrice(stock);
                Optional<Double> priceInReportCurrency = DataLoader.convertFx(latestPriceInTradingCurrency, company.profile.currency, company.profile.reportedCurrency, LocalDate.now(), false);
                model.addAttribute("latestPrice", priceInReportCurrency.orElse(company.latestPrice));
                model.addAttribute("latestPriceTradingCurrency", latestPriceInTradingCurrency);
                model.addAttribute("tradingCurrencySymbol", getCurrencySymbol(company.profile.currency));
            }

            return "calculator";
        }
    }

    public static String getCurrencySymbol(String currencyName) {
        try {
            return Currency.getInstance(currencyName).getSymbol();
        } catch (Exception e) {
            return "";
        }
    }

    private double nonNullOf(Double a, double d) {
        if (a != null) {
            return a;
        }
        return d;
    }

    public void fillModelWithCommonStockData(String stock, Model model, HttpServletRequest request) {
        Optional<DecodedJWT> jwtOptional = loginController.getJwt(request);

        if (jwtOptional.isPresent()) {
            DecodedJWT jwt = jwtOptional.get();
            AccountType accountType = loginController.getAccountType(jwt);
            boolean allowed = viewedStocksService.getAndUpdateAllowViewStocks(jwt.getSubject(), accountType, stock);

            model.addAttribute("accountType", accountType.toString());
            int viewLimit = viewedStocksService.getAllowedViewCount(accountType);
            model.addAttribute("viewLimit", viewLimit);
            if (!allowed) {
                model.addAttribute("allowed", false);
            } else {
                model.addAttribute("allowed", true);
            }
            model.addAttribute("viewLimitText", viewLimit == Integer.MAX_VALUE ? "∞" : Integer.toString(viewLimit));
            model.addAttribute("currentlyViewText", viewedStocksService.getViewCount(jwt.getSubject()));
            model.addAttribute("haveLimitedStocksCount", accountType.equals(AccountType.STANDARD) || accountType.equals(AccountType.FREE));
        } else {
            List<String> freeSockList = freeStockRepository.getFreeSockList();
            model.addAttribute("accountType", "NOT_LOGGED_IN");
            if (!freeSockList.contains(stock)) {
                model.addAttribute("allowed", false);
            } else {
                model.addAttribute("allowed", true);
            }
        }

        Optional<String> companyNameOptional = symbolIndexProvider.getCompanyName(stock);
        String companyName = stock;
        if (companyNameOptional.isPresent()) {
            companyName = companyNameOptional.get();
        }
        model.addAttribute("stock", stock);
        if (Boolean.TRUE.equals(model.getAttribute("allowed"))) {
            model.addAttribute("stockToLoad", stock);
        } else {
            model.addAttribute("stockToLoad", "INTC");
        }
        model.addAttribute("companyName", companyName);

    }

    @GetMapping("/sp500")
    public String sp500(Model model) {
        return "sp500";
    }

}
