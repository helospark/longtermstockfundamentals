package com.helospark.financialdata.management.watchlist;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.Striped;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.DateAware;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.SimpleDataElement;
import com.helospark.financialdata.management.user.GenericResponseAccountResult;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.watchlist.repository.MessageCompresser;
import com.helospark.financialdata.management.watchlist.repository.PortfolioPerformanceHistory;
import com.helospark.financialdata.management.watchlist.repository.PortfolioPerformanceHistoryElement;
import com.helospark.financialdata.management.watchlist.repository.PortfolioPerformanceHistoryRepository;
import com.helospark.financialdata.management.watchlist.repository.SimpleHolding;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.FinancialDataMerger;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.InvestmentScoreCalculator;
import com.helospark.financialdata.service.PietroskyScoreCalculator;
import com.helospark.financialdata.service.RatioCalculator;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/historical-performance")
public class HistorcalPerformanceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistorcalPerformanceController.class);

    @Autowired
    private PortfolioPerformanceHistoryRepository portfolioHistoricalRepository;
    @Autowired
    private LoginController loginController;
    @Autowired
    private MessageCompresser messageCompresser;
    @Autowired
    private SymbolAtGlanceProvider atGlanceProvider;
    @Autowired
    private FinancialDataMerger merger;

    private static Striped<Lock> duplicateLoadLocks = Striped.lock(1000);

    Cache<String, List<ExtendedHistoricalChartElement>> extendedDataCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(2000)
            .build();

    @GetMapping("/eps")
    public List<SimpleDataElement> getEps(HttpServletRequest request, HttpServletResponse response) {
        return getSimplePortfolioResult(request, element -> element.getEps());
    }

    @GetMapping("/fcf")
    public List<SimpleDataElement> getFcf(HttpServletRequest request, HttpServletResponse response) {
        return getSimplePortfolioResult(request, element -> element.getFcf());
    }

    @GetMapping("/total")
    public List<SimpleDataElement> getTotal(HttpServletRequest request, HttpServletResponse response) {
        return getSimplePortfolioResult(request, element -> element.getTotal());
    }

    @GetMapping("/equity")
    public List<SimpleDataElement> getEquity(HttpServletRequest request, HttpServletResponse response) {
        return getSimplePortfolioResult(request, element -> element.getTotalEquity());
    }

    @GetMapping("/number-of-holdings")
    public List<SimpleDataElement> getNumberOfHoldings(HttpServletRequest request, HttpServletResponse response) {
        return getSimplePortfolioResult(request, element -> (double) element.getHoldings().size());
    }

    @GetMapping("/pe-ratio")
    public List<SimpleDataElement> getPeRatioHistory(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.pe);
    }

    @GetMapping("/pfcf-ratio")
    public List<SimpleDataElement> getPfcfRatioHistory(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.pfcf);
    }

    @GetMapping("/altman")
    public List<SimpleDataElement> getAltmanHistory(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.altman);
    }

    @GetMapping("/d2e")
    public List<SimpleDataElement> getD2E(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.d2e);
    }

    @GetMapping("/share-change")
    public List<SimpleDataElement> getShareChange(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.shareChange);
    }

    @GetMapping("/revenue-growth")
    public List<SimpleDataElement> getRevenueGrowth(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.revenueGrowth);
    }

    @GetMapping("/eps-growth")
    public List<SimpleDataElement> getEpsGrowtn(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.epsGrowth);
    }

    @GetMapping("/fcf-growth")
    public List<SimpleDataElement> getFcfGrowth(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.fcfGrowth);
    }

    @GetMapping("/revenue")
    public List<SimpleDataElement> getRevenue(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.totalAttributableRevenue);
    }

    @GetMapping("/roic")
    public List<SimpleDataElement> getRoic(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.roic);
    }

    @GetMapping("/roe")
    public List<SimpleDataElement> getRoe(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.roe);
    }

    @GetMapping("/roa")
    public List<SimpleDataElement> getRoa(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.roa);
    }

    @GetMapping("/fcf-roic")
    public List<SimpleDataElement> getFcfRoic(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.roe);
    }

    @GetMapping("/gross-margin")
    public List<SimpleDataElement> getGrossMargin(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.grossMargin);
    }

    @GetMapping("/operating-margin")
    public List<SimpleDataElement> getOpMargin(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.opMargin);
    }

    @GetMapping("/net-margin")
    public List<SimpleDataElement> getNetMargin(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.netMargin);
    }

    @GetMapping("/investment-score")
    public List<SimpleDataElement> getInvestmentScore(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.investmentScore);
    }

    @GetMapping("/stock-based-compensation-per-fcf")
    public List<SimpleDataElement> getStockBasedCompensationPerFcf(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.stockBasedCompensationPerFcf);
    }

    @GetMapping("/stock-based-compensation-per-revenue")
    public List<SimpleDataElement> getStockBasedCompensationPerRevenue(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.stockBasedCompensationPerRevenue);
    }

    /* USAGE metrics */
    @GetMapping("/rnd_per_ocf")
    public List<SimpleDataElement> getRndPerOcf(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.rndPerOCF);
    }

    @GetMapping("/dividend_per_ocf")
    public List<SimpleDataElement> getDividendPerOcf(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.dividendPerOCF);
    }

    @GetMapping("/buyback_per_ocf")
    public List<SimpleDataElement> getBuybackPerOcf(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.buybackPerOCF);
    }

    @GetMapping("/debt_repayment_per_ocf")
    public List<SimpleDataElement> getDebtRepaymentPerOcf(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.debtRepaymentPerOCF);
    }

    @GetMapping("/capex_per_ocf")
    public List<SimpleDataElement> getCapexPerOcf(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.capexPerOCF);
    }

    @GetMapping("/mna_per_ocf")
    public List<SimpleDataElement> getMnaPerOcf(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.mnaPerOCF);
    }

    @GetMapping("/opcash")
    public List<SimpleDataElement> getOperatingCashFlow(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.operatingCashFlow);
    }

    @GetMapping("/dividends")
    public List<SimpleDataElement> getDividends(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> -a.dividends);
    }

    @GetMapping("/shareholder-yield")
    public List<SimpleDataElement> getShareholderYield(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.shareholderYield);
    }

    @GetMapping("/pietrosky")
    public List<SimpleDataElement> getPietrosky(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.pietrosky);
    }

    @GetMapping("/cash")
    public List<SimpleDataElement> getCash(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.cash);
    }

    @GetMapping("/total-assets")
    public List<SimpleDataElement> getTotalAssets(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.totalAssets);
    }

    @GetMapping("/total-liabilities")
    public List<SimpleDataElement> getTotalLiabilities(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.totalLiabilities);
    }

    @GetMapping("/total-debt")
    public List<SimpleDataElement> getTotalDebt(HttpServletRequest request, HttpServletResponse response) {
        return mapExtendedChartElement(request, a -> a.totalDebt);
    }

    public List<SimpleDataElement> mapExtendedChartElement(HttpServletRequest request, Function<ExtendedHistoricalChartElement, Double> mapper) {
        List<ExtendedHistoricalChartElement> extendedChart = internalCalculateExtendedChartDateCached(request);

        return extendedChart.stream().map(a -> new SimpleDataElement(a.getDate().toString(), mapper.apply(a))).collect(Collectors.toList());
    }

    public List<ExtendedHistoricalChartElement> internalCalculateExtendedChartDateCached(HttpServletRequest request) {
        Optional<DecodedJWT> user = loginController.getJwt(request);
        if (!user.isPresent()) {
            throw new WatchlistPermissionDeniedException("Not logged in");
        }
        String subject = user.get().getSubject();
        List<ExtendedHistoricalChartElement> result = extendedDataCache.getIfPresent(subject);

        if (result != null) {
            return result;
        }

        Lock lock = duplicateLoadLocks.get(subject);
        try {
            lock.tryLock(50, TimeUnit.SECONDS);

            result = extendedDataCache.getIfPresent(subject);

            if (result != null) {
                return result;
            }

            result = internalCalculateExtendedChartDate(request);
            extendedDataCache.put(subject, result);

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }

    }

    private List<ExtendedHistoricalChartElement> internalCalculateExtendedChartDate(HttpServletRequest request) {
        List<PortfolioPerformanceHistoryElement> historicalPerformance = getHistoricalPortfolioElements(request);
        List<ExtendedHistoricalChartElement> result = new ArrayList<>();

        List<FinancialsTtm> mergedEntities = merger.createdWeightedPortfolio(historicalPerformance);

        int historicalIndex = 0;
        for (var element : historicalPerformance) {
            LocalDate date = element.getDate();
            double offsetYear = calculateYearsAgo(date);

            ExtendedHistoricalChartElement currentExtendedElement = new ExtendedHistoricalChartElement(date);
            result.add(currentExtendedElement);

            List<SimpleHolding> holdings = element.getHoldings();

            double totalExCash = 0.0;
            double totalEarnings = 0.0;
            double totalFcf = 0.0;
            double totalShareChange = 0.0;
            double totalRevenueGrowth = 0.0;
            double totalRevenueOwned = 0.0;
            double totalAltman = 0.0;
            double investmentScoreSum = 0.0;
            double pietroskyTotal = 0.0;
            double cashUsd = 0.0;

            double totalEPS4YrsAgo = 0.0;
            double totalFCF4YrsAgo = 0.0;

            double totalEPSNow = 0.0;
            double totalFCFNow = 0.0;

            for (var holding : holdings) {
                String stock = holding.ticket;
                int owned = holding.count;
                var data = DataLoader.readFinancials(stock);
                int index = Helpers.findIndexWithOrBeforeDate(data.financials, date);
                int indexYrsAgo = Helpers.findIndexWithOrBeforeDate(data.financials, date.minusYears(4));

                for (int i = 3 * 12; i >= 2 * 12 && indexYrsAgo == -1; --i) {
                    indexYrsAgo = Helpers.findIndexWithOrBeforeDate(data.financials, date.minusMonths(i));
                }

                Optional<AtGlanceData> atGlanceOpt = atGlanceProvider.loadAtGlanceDataClosestToDate(date).map(a -> a.get(stock));

                if (index == -1 || atGlanceOpt.isEmpty()) {
                    continue;
                }
                AtGlanceData atGlance = atGlanceOpt.get();

                var financialTtm = data.financials.get(index);
                double ownedValue = financialTtm.priceUsd * owned;

                if (!stock.startsWith("CASH.")) {
                    double revenueGrowth = Double.isFinite(atGlance.revenueGrowth) ? atGlance.revenueGrowth : calculateGrowth(data, d -> (double) d.incomeStatementTtm.revenue, date);

                    totalExCash += ownedValue;
                    totalEarnings += DataLoader.convertFx(financialTtm.incomeStatementTtm.eps * owned, data.profile.reportedCurrency, "USD", date, false).orElse(0.0);
                    totalFcf += DataLoader.convertFx(((double) financialTtm.cashFlowTtm.freeCashFlow / financialTtm.incomeStatementTtm.weightedAverageShsOut) * owned, data.profile.reportedCurrency, "USD", date, false).orElse(0.0);
                    totalShareChange += orZero(atGlance.shareCountGrowth2yr * ownedValue);
                    totalAltman += orZero(atGlance.altman * ownedValue);
                    totalRevenueGrowth += orZero(revenueGrowth * ownedValue);
                    if (indexYrsAgo != -1) {
                        var yrsAgoFinancial = data.financials.get(indexYrsAgo);
                        double fcfAmountThen = (((double) yrsAgoFinancial.cashFlowTtm.freeCashFlow / yrsAgoFinancial.incomeStatementTtm.weightedAverageShsOut) * ownedValue);
                        double fcfAmountNow = (((double) financialTtm.cashFlowTtm.freeCashFlow / financialTtm.incomeStatementTtm.weightedAverageShsOut) * ownedValue);
                        double epsAmountThen = yrsAgoFinancial.incomeStatementTtm.eps * ownedValue;
                        double epsAmountNow = financialTtm.incomeStatementTtm.eps * ownedValue;

                        totalEPS4YrsAgo += DataLoader.convertFx(epsAmountThen, data.profile.reportedCurrency, "USD", date, false).orElse(0.0);
                        totalFCF4YrsAgo += DataLoader.convertFx(fcfAmountThen, data.profile.reportedCurrency, "USD", date, false).orElse(0.0);
                        totalEPSNow += DataLoader.convertFx(epsAmountNow, data.profile.reportedCurrency, "USD", date, false).orElse(0.0);
                        totalFCFNow += DataLoader.convertFx(fcfAmountNow, data.profile.reportedCurrency, "USD", date, false).orElse(0.0);
                    }
                    totalRevenueOwned += DataLoader.convertFx((financialTtm.incomeStatementTtm.revenue / financialTtm.incomeStatementTtm.weightedAverageShsOut) * owned, data.profile.reportedCurrency, "USD", date, false).orElse(0.0);
                    investmentScoreSum += InvestmentScoreCalculator.calculate(data, offsetYear).orElse(0.0) * ownedValue;
                    pietroskyTotal += PietroskyScoreCalculator.calculatePietroskyScore(data, financialTtm).orElse(0) * ownedValue;
                    cashUsd += DataLoader.convertFx(((double) financialTtm.balanceSheet.cashAndCashEquivalents / financialTtm.incomeStatementTtm.weightedAverageShsOut) * owned, data.profile.reportedCurrency, "USD", date, false).orElse(0.0);
                } else {
                    cashUsd += DataLoader.convertFx(financialTtm.incomeStatementTtm.eps * owned, stock.replaceAll("CASH.", ""), "USD", date, false).orElse(0.0);
                }
            }

            var entity = mergedEntities.get(historicalIndex);
            currentExtendedElement.pe = totalExCash / totalEarnings;
            currentExtendedElement.pfcf = totalExCash / totalFcf;

            currentExtendedElement.shareChange = totalShareChange / totalExCash;
            currentExtendedElement.revenueGrowth = totalRevenueGrowth / totalExCash;
            currentExtendedElement.epsGrowth = GrowthCalculator.calculateGrowth(totalEPSNow, totalEPS4YrsAgo, 4);
            currentExtendedElement.fcfGrowth = GrowthCalculator.calculateGrowth(totalFCFNow, totalFCF4YrsAgo, 4);

            currentExtendedElement.totalAttributableRevenue = totalRevenueOwned;
            currentExtendedElement.cash = cashUsd;
            currentExtendedElement.operatingCashFlow = entity.cashFlowTtm.operatingCashFlow;

            currentExtendedElement.altman = totalAltman / totalExCash;
            currentExtendedElement.d2e = RatioCalculator.calculateDebtToEquityRatio(entity);
            currentExtendedElement.pietrosky = pietroskyTotal / totalExCash;
            currentExtendedElement.investmentScore = investmentScoreSum / totalExCash;
            currentExtendedElement.totalAssets = entity.balanceSheet.totalAssets;
            currentExtendedElement.totalLiabilities = entity.balanceSheet.totalLiabilities;
            currentExtendedElement.totalDebt = entity.balanceSheet.totalDebt;

            currentExtendedElement.roic = RoicCalculator.calculateRoic(entity) * 100.0;
            currentExtendedElement.roe = RoicCalculator.calculateROE(entity) * 100.0;
            currentExtendedElement.roa = RoicCalculator.calculateROA(entity) * 100.0;
            currentExtendedElement.fcfRoic = RoicCalculator.calculateFcfRoic(entity) * 100.0;

            currentExtendedElement.opMargin = (double) entity.incomeStatementTtm.operatingIncome / entity.incomeStatementTtm.revenue * 100.0;
            currentExtendedElement.grossMargin = (double) entity.incomeStatementTtm.grossProfit / entity.incomeStatementTtm.revenue * 100.0;
            currentExtendedElement.netMargin = (double) entity.incomeStatementTtm.netIncome / entity.incomeStatementTtm.revenue * 100.0;

            // usage
            currentExtendedElement.capexPerOCF = getCapexPerOCF(entity);
            currentExtendedElement.rndPerOCF = getRndPerOCF(entity);
            currentExtendedElement.mnaPerOCF = getMnAPerOCF(entity);
            currentExtendedElement.dividendPerOCF = getDividendPerOCF(entity);
            currentExtendedElement.buybackPerOCF = getBuybackPerOCF(entity);
            currentExtendedElement.debtRepaymentPerOCF = getDebtRepaymentPerOCF(entity);
            // -- end of usage

            currentExtendedElement.dividends = entity.cashFlowTtm.dividendsPaid;
            currentExtendedElement.shareholderYield = (entity.cashFlowTtm.dividendsPaid + entity.cashFlowTtm.commonStockRepurchased) * -1.0;
            currentExtendedElement.stockBasedCompensationPerFcf = (double) entity.cashFlowTtm.stockBasedCompensation / entity.cashFlowTtm.freeCashFlow * 100.0;
            currentExtendedElement.stockBasedCompensationPerRevenue = (double) entity.cashFlowTtm.stockBasedCompensation / entity.incomeStatementTtm.revenue * 100.0;

            ++historicalIndex;
        }

        return result;
    }

    private double calculateYearsAgo(LocalDate date) {
        return Math.abs(ChronoUnit.DAYS.between(date, LocalDate.now()) / 365.0);
    }

    private double orZero(double d) {
        return Double.isFinite(d) ? d : 0.0;
    }

    /* Usage metrics, same as in FinancialsController could be merged */
    public Double getCapexPerOCF(FinancialsTtm financialsTtm) {
        return toPercentNonNull((double) -financialsTtm.cashFlowTtm.capitalExpenditure / operatingCashFlowPlusRnd(financialsTtm));
    }

    public Double getRndPerOCF(FinancialsTtm financialsTtm) {
        return toPercentNonNull((double) financialsTtm.incomeStatementTtm.researchAndDevelopmentExpenses / operatingCashFlowPlusRnd(financialsTtm));
    }

    public Double getMnAPerOCF(FinancialsTtm financialsTtm) {
        return toPercentNonNull((double) -financialsTtm.cashFlowTtm.acquisitionsNet / operatingCashFlowPlusRnd(financialsTtm));
    }

    public Double getDividendPerOCF(FinancialsTtm financialsTtm) {
        return toPercentNonNull((double) -financialsTtm.cashFlowTtm.dividendsPaid / operatingCashFlowPlusRnd(financialsTtm));
    }

    public Double getBuybackPerOCF(FinancialsTtm financialsTtm) {
        return toPercentNonNull(((double) -financialsTtm.cashFlowTtm.commonStockRepurchased) / operatingCashFlowPlusRnd(financialsTtm));
    }

    public Double getDebtRepaymentPerOCF(FinancialsTtm financialsTtm) {
        return toPercentNonNull((double) -financialsTtm.cashFlowTtm.debtRepayment / operatingCashFlowPlusRnd(financialsTtm));
    }

    public long operatingCashFlowPlusRnd(FinancialsTtm financialsTtm) {
        return financialsTtm.cashFlowTtm.operatingCashFlow + financialsTtm.incomeStatementTtm.researchAndDevelopmentExpenses;
    }

    private Double toPercentNonNull(Double grossProfitMargin) {
        if (grossProfitMargin < 0.0) {
            return 0.0;
        }
        if (grossProfitMargin != null && Double.isFinite(grossProfitMargin)) {
            return grossProfitMargin * 100.0;
        } else {
            return Double.NaN;
        }
    }

    /* End of usage metrics */

    public List<SimpleDataElement> getSimplePortfolioResult(HttpServletRequest request, Function<PortfolioPerformanceHistoryElement, Double> function) {
        List<PortfolioPerformanceHistoryElement> historicalPerformance = getHistoricalPortfolioElements(request);

        List<SimpleDataElement> result = new ArrayList<>();
        for (var element : historicalPerformance) {
            result.add(new SimpleDataElement(element.getDate().toString(), function.apply(element)));
        }

        return result;
    }

    public List<PortfolioPerformanceHistoryElement> getHistoricalPortfolioElements(HttpServletRequest request) {
        Optional<DecodedJWT> user = loginController.getJwt(request);
        if (!user.isPresent()) {
            throw new WatchlistPermissionDeniedException("Not logged in");
        }
        List<PortfolioPerformanceHistoryElement> historicalPerformance = getHistoricalPerformance(user);
        return historicalPerformance;
    }

    public List<PortfolioPerformanceHistoryElement> getHistoricalPerformance(Optional<DecodedJWT> user) {
        Optional<PortfolioPerformanceHistory> historicalPerformance = portfolioHistoricalRepository.readHistoricalPortfolio(user.get().getSubject());

        if (historicalPerformance.isEmpty()) {
            return List.of();
        }

        ArrayList<PortfolioPerformanceHistoryElement> result = new ArrayList<>(messageCompresser.uncompressListOf(historicalPerformance.get().getHistory(), PortfolioPerformanceHistoryElement.class));

        Collections.sort(result, (a, b) -> a.getDate().compareTo(b.getDate()));

        return result;
    }

    private double calculateGrowth(CompanyFinancials data, Function<FinancialsTtm, Double> f, LocalDate date) {
        int nowIndex = Helpers.findIndexWithOrBeforeDate(data.financials, date);
        for (int i = 4; i >= 2; --i) {
            int thenIndex = Helpers.findIndexWithOrBeforeDate(data.financials, date.minusYears(i));
            if (thenIndex != -1) {
                double growth = GrowthCalculator.calculateGrowth(f.apply(data.financials.get(nowIndex)), f.apply(data.financials.get(thenIndex)), i);
                if (growth != Double.NaN) {
                    return (float) growth;
                }
            }
        }
        return Float.NaN;
    }

    @ExceptionHandler(WatchlistPermissionDeniedException.class)
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    public GenericResponseAccountResult handlePermissionDenied(WatchlistPermissionDeniedException exception) {
        LOGGER.warn("Unauthorized exception", exception);
        return new GenericResponseAccountResult(exception.getMessage());
    }

    static class ExtendedHistoricalChartElement implements DateAware {
        public double shareholderYield;
        public double fcfGrowth;
        public double epsGrowth;
        public double totalDebt;
        public double totalLiabilities;
        public double totalAssets;
        public double cash;
        public double pietrosky;
        public Double debtRepaymentPerOCF;
        public Double buybackPerOCF;
        public Double dividendPerOCF;
        public Double mnaPerOCF;
        public Double rndPerOCF;
        public Double capexPerOCF;

        public double stockBasedCompensationPerRevenue;

        public double stockBasedCompensationPerFcf;

        public double d2e;

        public double netMargin;

        public double grossMargin;

        public double fcfRoic;

        public double roa;

        public double investmentScore;

        public double opMargin;

        public double roe;

        public double roic;

        public double totalAttributableRevenue;
        public double operatingCashFlow;
        public double dividends;

        LocalDate date;

        public Double pe;
        public Double pfcf;
        public double altman;
        public double shareChange;
        public double revenueGrowth;

        public ExtendedHistoricalChartElement(LocalDate date) {
            this.date = date;
        }

        @Override
        public LocalDate getDate() {
            return date;
        }
    }

}
