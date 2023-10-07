package com.helospark.financialdata;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.Profile;
import com.helospark.financialdata.domain.SimpleDataElement;
import com.helospark.financialdata.domain.SimpleDateDataElement;
import com.helospark.financialdata.flags.FlagProvider;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.CapeCalculator;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.DcfCalculator;
import com.helospark.financialdata.service.DividendCalculator;
import com.helospark.financialdata.service.EnterpriseValueCalculator;
import com.helospark.financialdata.service.EverythingMoneyCalculator;
import com.helospark.financialdata.service.FedRateProvider;
import com.helospark.financialdata.service.GrahamNumberCalculator;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.PietroskyScoreCalculator;
import com.helospark.financialdata.service.RatioCalculator;
import com.helospark.financialdata.service.ReturnWithDividendCalculator;
import com.helospark.financialdata.service.RevenueProjector;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.StockBasedCompensationCalculator;
import com.helospark.financialdata.service.TrailingPegCalculator;

@RestController
@RequestMapping("/{stock}/financials")
public class FinancialsController {
    @Autowired
    List<FlagProvider> flagProviers;

    @GetMapping("/profile")
    public Profile getProfile(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        return company.profile;
    }

    @GetMapping("/eps")
    public List<SimpleDataElement> getEps(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> (double) financialsTtm.incomeStatementTtm.netIncome / financialsTtm.incomeStatementTtm.weightedAverageShsOut);
    }

    @GetMapping("/revenue")
    public List<SimpleDataElement> getRevenue(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.incomeStatementTtm.revenue);
    }

    @GetMapping("/fcf")
    public List<SimpleDataElement> getFcf(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.cashFlowTtm.freeCashFlow);
    }

    @GetMapping("/net_income")
    public List<SimpleDataElement> getNetIncome(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.incomeStatementTtm.netIncome);
    }

    @GetMapping("/pfcf")
    public List<SimpleDataElement> getPFcf(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> (double) financialsTtm.cashFlowTtm.freeCashFlow / financialsTtm.incomeStatementTtm.weightedAverageShsOut);
    }

    @GetMapping("/operating_margin")
    public List<SimpleDataElement> getOperativeMargin(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent((double) financialsTtm.incomeStatementTtm.operatingIncome / financialsTtm.incomeStatementTtm.revenue));
    }

    @GetMapping("/gross_margin")
    public List<SimpleDataElement> getGrossMargin(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent(RatioCalculator.calculateGrossProfitMargin(financialsTtm)));
    }

    @GetMapping("/net_margin")
    public List<SimpleDataElement> getNetMargin(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> {
            Double result = toPercent((double) financialsTtm.incomeStatementTtm.netIncome / financialsTtm.incomeStatementTtm.revenue);
            if (result == null || result > 1000) {
                return null;
            }
            return result;
        });
    }

    @GetMapping("/fcf_margin")
    public List<SimpleDataElement> getFcfMargin(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent((double) financialsTtm.cashFlowTtm.freeCashFlow / financialsTtm.incomeStatementTtm.revenue));
    }

    @GetMapping("/market_cap_usd")
    public List<SimpleDataElement> getMarketCapUsd(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = getIncomeData(company, false, financialsTtm -> financialsTtm.incomeStatementTtm.weightedAverageShsOut * financialsTtm.priceUsd);
        if (company.financials.size() > 0 && company.latestPriceDate.compareTo(company.financials.get(0).getDate()) > 0) {
            double mk = company.financials.get(0).incomeStatementTtm.weightedAverageShsOut * company.latestPriceUsd;
            result.add(0, new SimpleDataElement(company.latestPriceDate.toString(), mk));
        }
        return result;
    }

    @GetMapping("/pe_ratio")
    public List<SimpleDataElement> getPeMargin(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getPriceIncomeData(stock, quarterly, (price, financialsTtm) -> RatioCalculator.calculatePriceToEarningsRatio(price, financialsTtm));
    }

    @GetMapping("/pfcf_ratio")
    public List<SimpleDataElement> getPFcfRatio(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getPriceIncomeData(stock, quarterly, (price, financialsTtm) -> price / ((double) financialsTtm.cashFlowTtm.freeCashFlow / financialsTtm.incomeStatementTtm.weightedAverageShsOut));
    }

    @GetMapping("/price_to_gross_profit")
    public List<SimpleDataElement> getPriceToGrossProfit(@PathVariable("stock") String stock) {
        return getPriceIncomeData(stock, false, (price, financialsTtm) -> (price * financialsTtm.incomeStatementTtm.weightedAverageShsOut) / financialsTtm.incomeStatementTtm.grossProfit);
    }

    @GetMapping("/price_to_sales")
    public List<SimpleDataElement> getPriceToSales(@PathVariable("stock") String stock) {
        return getPriceIncomeData(stock, false, (price, financialsTtm) -> (price * financialsTtm.incomeStatementTtm.weightedAverageShsOut) / (financialsTtm.incomeStatementTtm.revenue));
    }

    @GetMapping("/expected_return")
    public List<SimpleDataElement> getExpectedReturn(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);

            double pe = financialsTtm.price / financialsTtm.incomeStatementTtm.eps;
            double pastGrowthRate = TrailingPegCalculator.getPastEpsGrowthRate(company, i);
            double dividendYield = DividendCalculator.getDividendYield(company, i) * 100.0;
            double stockBasedCompensationPerMkt = StockBasedCompensationCalculator.stockBasedCompensationPerMarketCap(financialsTtm);
            double yearsAgo = i / 4.0;
            double pastShareCountGrowth = GrowthCalculator.getShareCountGrowthInInterval(company.financials, yearsAgo + 5, yearsAgo).orElse(0.0);

            if (pastShareCountGrowth > 0) {
                pastShareCountGrowth = 0.0;
            } else {
                pastShareCountGrowth *= -1;
            }

            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), 100.0 / pe + pastGrowthRate + dividendYield - stockBasedCompensationPerMkt - pastShareCountGrowth));
        }
        return result;
    }

    @GetMapping("/past_pe_to_growth_ratio")
    public List<SimpleDataElement> getTrailingPeg(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);
            Optional<Double> value = TrailingPegCalculator.calculateTrailingPeg(company, i / 4.0);

            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), value.orElse(null)));
        }
        return result;
    }

    @GetMapping("/past_pe_to_rev_growth_ratio")
    public List<SimpleDataElement> getTrailingPegWithRevGrowth(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);
            Optional<Double> value = TrailingPegCalculator.calculateTrailingPegWithRevGrowth(company, i / 4.0);

            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), value.orElse(null)));
        }
        return result;
    }

    @GetMapping("/past_cape_to_growth_ratio")
    public List<SimpleDataElement> getTrailingCapeg(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);
            Optional<Double> value = TrailingPegCalculator.calculateTrailingCAPeg(company, i / 4.0);

            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), value.orElse(null)));
        }
        return result;
    }

    @GetMapping("/fcf_yield")
    public List<SimpleDataElement> getFreeCashFlowYield(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly,
                financialsTtm -> toPercent(((double) financialsTtm.cashFlowTtm.freeCashFlow / financialsTtm.incomeStatementTtm.weightedAverageShsOut) / financialsTtm.price));
    }

    @GetMapping("/eps_yield")
    public List<SimpleDataElement> getEpsYield(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getPriceIncomeData(stock, quarterly, (price, financialsTtm) -> toPercent(financialsTtm.incomeStatementTtm.eps / price));
    }

    @GetMapping("/p2b_ratio")
    public List<SimpleDataElement> getP2BValue(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> RatioCalculator.calculatePriceToBookRatio(financialsTtm));
    }

    @GetMapping("/p2tb_ratio")
    public List<SimpleDataElement> getP2TangibleBookValue(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> RatioCalculator.calculatePriceToTangibleBookRatio(financialsTtm));
    }

    @GetMapping("/intangible_assets_percent")
    public List<SimpleDataElement> getIntangibleAssetsPercent(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent((double) financialsTtm.balanceSheet.goodwillAndIntangibleAssets / financialsTtm.balanceSheet.totalAssets));
    }

    @GetMapping("/goodwill_percent")
    public List<SimpleDataElement> getGoodwillPercent(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent((double) financialsTtm.balanceSheet.goodwill / financialsTtm.balanceSheet.totalAssets));
    }

    @GetMapping("/fed_rate")
    public List<SimpleDataElement> getFedRate(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> FedRateProvider.getFedFundsRate(financialsTtm.getDate()));
    }

    @GetMapping("/quick_ratio")
    public List<SimpleDataElement> getQuickRatio(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> RatioCalculator.calculateQuickRatio(financialsTtm).orElse(null));
    }

    @GetMapping("/short_term_coverage_ratio")
    public List<SimpleDataElement> getShortTermCoverageRatio(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock,
                quarterly,
                financialsTtm -> toPercent(financialsTtm.balanceSheet.shortTermDebt > 0.0 ? (double) financialsTtm.cashFlowTtm.operatingCashFlow / financialsTtm.balanceSheet.shortTermDebt : null));
    }

    @GetMapping("/short_term_assets_to_total_debt")
    public List<SimpleDataElement> getShortTermAssetsToTotalDebt(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock,
                quarterly, financialsTtm -> financialsTtm.balanceSheet.longTermDebt > 0 ? (double) financialsTtm.balanceSheet.totalCurrentAssets / financialsTtm.balanceSheet.totalDebt : null);
    }

    @GetMapping("/return_on_assets")
    public List<SimpleDataElement> getReturnOnAssets(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent(RoicCalculator.calculateROA(financialsTtm)));
    }

    @GetMapping("/return_on_equity")
    public List<SimpleDataElement> getReturnOnEquity(@PathVariable("stock") String stock) {
        return getIncomeData(stock, false, financialsTtm -> toPercent(RoicCalculator.calculateROE(financialsTtm)));
    }

    @GetMapping("/return_on_tangible_assets")
    public List<SimpleDataElement> getReturnOnTangibleAssets(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent(RoicCalculator.calculateROTA(financialsTtm)));
    }

    @GetMapping("/cash_flow_to_debt")
    public List<SimpleDataElement> getCashFlowToDebt(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock,
                quarterly,
                financialsTtm -> toPercent(financialsTtm.balanceSheet.shortTermDebt > 0.0 ? (double) financialsTtm.cashFlowTtm.operatingCashFlow / financialsTtm.balanceSheet.totalDebt : 0));
    }

    @GetMapping("/share_count")
    public List<SimpleDataElement> getShareCount(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.incomeStatementTtm.weightedAverageShsOut);
    }

    @GetMapping("/share_buyback_per_net_income")
    public List<SimpleDataElement> getShareBuybackPerNetIncome(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly,
                financialsTtm -> {
                    if (financialsTtm.incomeStatementTtm.netIncome < 0) {
                        return null;
                    }
                    return toPercent(((double) -financialsTtm.cashFlowTtm.commonStockRepurchased - financialsTtm.cashFlowTtm.commonStockIssued) / financialsTtm.incomeStatementTtm.netIncome);
                });
    }

    @GetMapping("/share_buyback_per_net_fcf")
    public List<SimpleDataElement> getShareBuybackPerFcf(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly,
                financialsTtm -> {
                    if (financialsTtm.cashFlowTtm.freeCashFlow < 0) {
                        return null;
                    }
                    return toPercent(((double) -financialsTtm.cashFlowTtm.commonStockRepurchased - financialsTtm.cashFlowTtm.commonStockIssued) / financialsTtm.cashFlowTtm.freeCashFlow);
                });
    }

    @GetMapping("/interest_expense")
    public List<SimpleDataElement> getInterestExpense(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.incomeStatementTtm.interestExpense);
    }

    @GetMapping("/interest_rate")
    public List<SimpleDataElement> getInterestRate(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> {
            if (financialsTtm.balanceSheet.totalDebt > 0 && financialsTtm.incomeStatementTtm.interestExpense > 0) {
                return toPercent((double) financialsTtm.incomeStatementTtm.interestExpense / financialsTtm.balanceSheet.totalDebt);
            } else {
                return null;
            }
        });
    }

    @GetMapping("/interest_coverage")
    public List<SimpleDataElement> getInterestCoverage(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> {
            return RatioCalculator.calculateInterestCoverageRatio(financialsTtm);
        });
    }

    @GetMapping("/eps_dcf")
    public List<SimpleDataElement> getEpsDcf(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);

            double growth = calculateAnyLongTermEpsGrowthAtYear(company, i);

            double dcf = DcfCalculator.doStockDcfAnalysis(getMeanEps(company.financials, i), growth);

            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), dcf));
        }
        return result;
    }

    @GetMapping("/fcf_dcf")
    public List<SimpleDataElement> getFcfDcf(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);

            double growth = calculateAnyLongTermFcfGrowthAtYear(company, i);

            double dcf = DcfCalculator.doStockDcfAnalysis(getMeanFcf(company.financials, i), growth);

            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), dcf));
        }
        return result;
    }

    @GetMapping("/dividend_dcf")
    public List<SimpleDataElement> getDividendDcf(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);

            double dividend = calculateDividendPaidPerShare(financialsTtm);

            double growth = calculateAnyLongTermDividendGrowthAtYear(company, i);

            double dcf = DcfCalculator.doCashFlowDcfAnalysisWithGrowth(dividend, growth * 0.9, growth * 0.75);

            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), dcf));
        }
        return result;
    }

    @GetMapping("/revenue_projection")
    public List<SimpleDataElement> getRevenueProjection(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            int year = i / 4;
            FinancialsTtm financialsTtm = company.financials.get(i);

            double growth = getAnyRevenueGrowth(company, year);

            double dcf = RevenueProjector.projectRevenue(financialsTtm, growth * 0.7, growth * 0.4);

            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), dcf));
        }
        return result;
    }

    @GetMapping("/composite_fair_value")
    public List<SimpleDataElement> getCompositeFairvalue(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);

            Optional<Double> dcf = DcfCalculator.doFullDcfAnalysisWithGrowth(company.financials, (i / 4.0));

            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), dcf.orElse(null)));
        }
        return result;
    }

    @GetMapping("/default_calculator_result")
    public List<SimpleDataElement> getDefaultCalculatorResult(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);

            Optional<Double> dcf = DcfCalculator.doDcfAnalysisRevenueWithDefaultParameters(company, (i / 4.0)).map(a -> a < 0 ? 0.0 : a);

            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), dcf.orElse(null)));
        }
        return result;
    }

    @GetMapping("/insider_trading_bought")
    public List<SimpleDataElement> getInsiderTradingBoughtResult(@PathVariable("stock") String stock) {
        return getIncomeData(stock, false, financialsTtm -> (double) financialsTtm.auxilaryInfo.insiderBoughtShares);
    }

    @GetMapping("/insider_trading_sold")
    public List<SimpleDataElement> getInsiderTradingSoldResult(@PathVariable("stock") String stock) {
        return getIncomeData(stock, false, financialsTtm -> (double) financialsTtm.auxilaryInfo.insiderSoldShares);
    }

    @GetMapping("/senate_trading_bought")
    public List<SimpleDataElement> getSenateTradingBoughtResult(@PathVariable("stock") String stock) {
        return getIncomeData(stock, false, financialsTtm -> (double) financialsTtm.auxilaryInfo.senateBoughtDollar);
    }

    @GetMapping("/senate_trading_sold")
    public List<SimpleDataElement> getSenateTradingSoldResult(@PathVariable("stock") String stock) {
        return getIncomeData(stock, false, financialsTtm -> (double) financialsTtm.auxilaryInfo.senateSoldDollar);
    }

    @GetMapping("/earnings_surprise")
    public List<SimpleDataElement> getEarningsSurprise(@PathVariable("stock") String stock) {
        return getIncomeData(stock, false, financialsTtm -> (double) financialsTtm.auxilaryInfo.earnSurprisePercent);
    }

    private double getAnyRevenueGrowth(CompanyFinancials company, int year) {
        List<Double> growths = new ArrayList<>();
        for (int i = 6; i > 0; --i) {
            Optional<Double> result = GrowthCalculator.getRevenueGrowthInInterval(company.financials, i + year, year);
            if (result.isPresent() && !result.get().isNaN()) {
                growths.add(result.get());
            }
        }
        for (int i = 4; i > 0; --i) {
            Optional<Double> result = GrowthCalculator.getRevenueGrowthInInterval(company.financials, i + year + 2, year + 2);
            if (result.isPresent() && !result.get().isNaN()) {
                growths.add(result.get());
            }
        }
        Collections.sort(growths);
        return growths.size() > 0 ? growths.get(growths.size() / 2) : 0.0;
    }

    private double getMeanFcf(List<FinancialsTtm> financials, int index) {
        List<Double> fcfs = new ArrayList<>();
        for (int i = index; i < index + 4 && i < financials.size(); ++i) {
            fcfs.add(GrowthCalculator.getFcfPerShare(financials.get(i)));
        }
        Collections.sort(fcfs);
        return fcfs.size() > 0 ? fcfs.get(fcfs.size() / 2) : 0.0;
    }

    private double getMeanEps(List<FinancialsTtm> financials, int index) {
        List<Double> epses = new ArrayList<>();
        for (int i = index; i < index + 4 && i < financials.size(); ++i) {
            epses.add(financials.get(i).incomeStatementTtm.eps);
        }
        Collections.sort(epses);
        return epses.size() > 0 ? epses.get(epses.size() / 2) : 0.0;
    }

    private double calculateAnyLongTermEpsGrowthAtYear(CompanyFinancials company, int i) {
        int year = i / 4;
        List<Double> growthRateList = new ArrayList<>();
        for (int offset = 10; offset > 3; --offset) {
            Optional<Double> growthInYear = GrowthCalculator.getEpsGrowthInInterval(company.financials, offset + year, year);
            if (growthInYear.isPresent() && !growthInYear.get().isNaN()) {
                growthRateList.add(growthInYear.get());
            }
        }
        for (int offset = 7; offset >= 3; --offset) {
            Optional<Double> growthInYear = GrowthCalculator.getEpsGrowthInInterval(company.financials, offset + year + 3, 3);
            if (growthInYear.isPresent() && !growthInYear.get().isNaN()) {
                growthRateList.add(growthInYear.get());
            }
        }
        Collections.sort(growthRateList);
        return growthRateList.size() > 1 ? growthRateList.get(growthRateList.size() / 2) : 0.0;
    }

    private double calculateAnyLongTermFcfGrowthAtYear(CompanyFinancials company, int i) {
        int year = i / 4;
        List<Double> growthRateList = new ArrayList<>();
        for (int offset = 10; offset > 3; --offset) {
            Optional<Double> growthInYear = GrowthCalculator.getFcfGrowthInInterval(company.financials, offset + year, year);
            if (growthInYear.isPresent() && !growthInYear.get().isNaN()) {
                growthRateList.add(growthInYear.get());
            }
        }
        for (int offset = 7; offset >= 3; --offset) {
            Optional<Double> growthInYear = GrowthCalculator.getFcfGrowthInInterval(company.financials, offset + year + 3, 3);
            if (growthInYear.isPresent() && !growthInYear.get().isNaN()) {
                growthRateList.add(growthInYear.get());
            }
        }
        Collections.sort(growthRateList);
        return growthRateList.size() > 1 ? growthRateList.get(growthRateList.size() / 2) : 0.0;
    }

    private double calculateAnyLongTermDividendGrowthAtYear(CompanyFinancials company, int i) {
        int year = i / 4;
        List<Double> growthRateList = new ArrayList<>();
        for (int offset = 10; offset > 3; --offset) {
            Optional<Double> growthInYear = GrowthCalculator.getDividendGrowthInInterval(company.financials, offset + year, year);
            if (growthInYear.isPresent() && !growthInYear.get().isNaN()) {
                growthRateList.add(growthInYear.get());
            }
        }
        Collections.sort(growthRateList);
        return growthRateList.size() > 1 ? growthRateList.get(growthRateList.size() / 2) : 0.0;
    }

    @GetMapping("/cash")
    public List<SimpleDataElement> getCash(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.balanceSheet.cashAndCashEquivalents);
    }

    @GetMapping("/current_assets")
    public List<SimpleDataElement> getTotalCurrentAssets(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.balanceSheet.totalCurrentAssets);
    }

    @GetMapping("/total_assets")
    public List<SimpleDataElement> getTotalAssets(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.balanceSheet.totalAssets);
    }

    @GetMapping("/total_liabilities")
    public List<SimpleDataElement> getTotalLiabilities(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.balanceSheet.totalLiabilities);
    }

    @GetMapping("/current_liabilities")
    public List<SimpleDataElement> getTotalCurrentLiabilities(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.balanceSheet.totalCurrentLiabilities);
    }

    @GetMapping("/long_term_debt")
    public List<SimpleDataElement> getLongTermDebt(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.balanceSheet.longTermDebt);
    }

    @GetMapping("/debt_to_equity")
    public List<SimpleDataElement> getDebtToEquity(@PathVariable("stock") String stock) {
        return getIncomeData(stock, false, financialsTtm -> RatioCalculator.calculateDebtToEquityRatio(financialsTtm));
    }

    @GetMapping("/non_current_assets")
    public List<SimpleDataElement> getLongTermLiabilities(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.balanceSheet.otherNonCurrentAssets);
    }

    @GetMapping("/acquisitions_per_market_cap")
    public List<SimpleDataElement> getAckquisitionsPerMarketCap(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent(-1.0 * financialsTtm.cashFlowTtm.acquisitionsNet / calculateMarketCap(financialsTtm)));
    }

    private double calculateMarketCap(FinancialsTtm financialsTtm) {
        return financialsTtm.price * financialsTtm.incomeStatementTtm.weightedAverageShsOut;
    }

    @GetMapping("/price")
    public List<SimpleDataElement> getPrice(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = getIncomeData(company, quarterly, financialsTtm -> financialsTtm.price);
        if (company.financials.size() > 0 && company.latestPriceDate.compareTo(company.financials.get(0).getDate()) > 0) {
            result.add(0, new SimpleDataElement(company.latestPriceDate.toString(), company.latestPrice));
        }
        return result;
    }

    @GetMapping("/return_with_reinvested_dividend")
    public List<SimpleDataElement> getPriceWithReinvestedDividends(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);

        return ReturnWithDividendCalculator.getPriceWithDividendsReinvested(company)
                .stream()
                .map(a -> new SimpleDataElement(a.getDate().toString(), a.value))
                .collect(Collectors.toList());
    }

    @GetMapping("/stock_compensation")
    public List<SimpleDataElement> getStockBasedCompensation(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> financialsTtm.cashFlowTtm.stockBasedCompensation);
    }

    @GetMapping("/stock_compensation_per_net_income")
    public List<SimpleDataElement> getStockBasedCompensationPerNetIncome(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> (double) financialsTtm.cashFlowTtm.stockBasedCompensation / financialsTtm.incomeStatementTtm.netIncome * 100.0);
    }

    @GetMapping("/stock_compensation_per_market_cap")
    public List<SimpleDataElement> getStockBasedCompensationPerMarketCap(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly,
                financialsTtm -> financialsTtm.cashFlowTtm.stockBasedCompensation / (financialsTtm.price * financialsTtm.incomeStatementTtm.weightedAverageShsOut) * 100.0);
    }

    @GetMapping("/roic")
    public List<SimpleDataElement> getRoic(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent(RoicCalculator.calculateRoic(financialsTtm)));
    }

    @GetMapping("/fcf_roic")
    public List<SimpleDataElement> getFcfRoic(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent(RoicCalculator.calculateFcfRoic(financialsTtm)));
    }

    @GetMapping("/capex_to_revenue")
    public List<SimpleDataElement> getCapexToRevenue(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent((double) financialsTtm.cashFlowTtm.capitalExpenditure / financialsTtm.incomeStatementTtm.revenue) * -1.0);
    }

    @GetMapping("/cash_per_share")
    public List<SimpleDataElement> getCashPerShare(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly,
                financialsTtm -> (double) financialsTtm.balanceSheet.cashAndShortTermInvestments / financialsTtm.incomeStatementTtm.weightedAverageShsOut);
    }

    @GetMapping("/ev_over_ebitda")
    public List<SimpleDataElement> getEvOverEbitda(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> EnterpriseValueCalculator.calculateEv(financialsTtm, financialsTtm.price) / financialsTtm.incomeStatementTtm.ebitda);
    }

    @GetMapping("/graham_number")
    public List<SimpleDataElement> getGrahamNumber(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> GrahamNumberCalculator.calculateGrahamNumber(financialsTtm).orElse(null));
    }

    @GetMapping("/stock_compensation_per_net_revenue")
    public List<SimpleDataElement> getStockBasedCompensationPerRevenue(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> (double) financialsTtm.cashFlowTtm.stockBasedCompensation / financialsTtm.incomeStatementTtm.revenue * 100.0);
    }

    @GetMapping("/dividend_yield")
    public List<SimpleDataElement> getDividendYield(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        var result = getIncomeData(company,
                quarterly, financialsTtm -> toPercent((double) -financialsTtm.cashFlowTtm.dividendsPaid / financialsTtm.incomeStatementTtm.weightedAverageShsOut / financialsTtm.price));
        if (company.financials.size() > 0) {
            Double yield = toPercent((double) -company.financials.get(0).cashFlowTtm.dividendsPaid / company.financials.get(0).incomeStatementTtm.weightedAverageShsOut / company.latestPrice);
            if (yield == null) {
                yield = 0.0;
            }
            result.add(0, new SimpleDataElement(company.latestPriceDate.toString(), yield));
        }
        return result;
    }

    @GetMapping("/dividend_payout_ratio")
    public List<SimpleDataElement> getPayoutRatio(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent(RatioCalculator.calculatePayoutRatio(financialsTtm)));
    }

    @GetMapping("/total_payout_ratio")
    public List<SimpleDataElement> getTotalPayoutRatio(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent(RatioCalculator.calculateTotalPayoutRatio(financialsTtm)));
    }

    @GetMapping("/dividend_payout_ratio_with_fcf")
    public List<SimpleDataElement> getPayoutRatioFcf(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> toPercent(RatioCalculator.calculateFcfPayoutRatio(financialsTtm)));
    }

    @GetMapping("/dividend_paid")
    public List<SimpleDataElement> getDividendPaid(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        return getIncomeData(stock, quarterly, financialsTtm -> calculateDividendPaidPerShare(financialsTtm));
    }

    public double calculateDividendPaidPerShare(FinancialsTtm financialsTtm) {
        return (double) -financialsTtm.cashFlow.dividendsPaid / financialsTtm.incomeStatement.weightedAverageShsOut;
    }

    @GetMapping("/dividend_yield_per_current_price")
    public List<SimpleDataElement> getDividendPerCurrentPrice(@PathVariable("stock") String stock) {
        return getIncomeDataCompany(stock,
                (financialsTtm, company) -> toPercent((double) -company.financials.get(0).cashFlowTtm.dividendsPaid / financialsTtm.incomeStatementTtm.weightedAverageShsOut / financialsTtm.price));
    }

    @GetMapping("/altmanz")
    public List<SimpleDataElement> getAltmanZ(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = getIncomeData(company, quarterly, financialsTtm -> AltmanZCalculator.calculateAltmanZScore(financialsTtm, financialsTtm.price));
        if (company.financials.size() > 0 && company.latestPriceDate.compareTo(company.financials.get(0).getDate()) > 0) {
            result.add(0, new SimpleDataElement(company.latestPriceDate.toString(), AltmanZCalculator.calculateAltmanZScore(company.financials.get(0), company.latestPrice)));
        }
        return result;
    }

    @GetMapping("/sloan")
    public List<SimpleDataElement> getSloan(@PathVariable("stock") String stock, @RequestParam(name = "quarterly", required = false) boolean quarterly) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        return getIncomeData(company, quarterly, financialsTtm -> RatioCalculator.calculateSloanPercent(financialsTtm));
    }

    @GetMapping("/eps_growth_rate")
    public List<SimpleDataElement> getGrowthRate(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 1; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = GrowthCalculator.getEpsGrowthInInterval(company.financials, yearsAgo, 0);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/dividend_growth_rate")
    public List<SimpleDataElement> getDividendRate(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 1; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = GrowthCalculator.getDividendGrowthInInterval(company.financials, yearsAgo, 0);
            if (growth.isPresent() && !growth.get().isInfinite() && !growth.get().isNaN()) {
                result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
            } else {
                result.add(new SimpleDataElement(element.getDate().toString(), null));
            }
        }

        return result;
    }

    @GetMapping("/eps_growth_rate_7yr_moving_avg")
    public List<SimpleDataElement> get7yrGrowthRateMovingAvg(@PathVariable("stock") String stock, @RequestParam(name = "year", defaultValue = "7") int year) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = GrowthCalculator.getEpsGrowthInInterval(company.financials, yearsAgo + year, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/revenue_growth_rate_xyr_moving_avg")
    public List<SimpleDataElement> getXyrGrowthRateMovingAvg(@PathVariable("stock") String stock, @RequestParam(name = "year", defaultValue = "7") int year) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = GrowthCalculator.getRevenueGrowthInInterval(company.financials, yearsAgo + year, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    private double calculateYearsAgo(LocalDate date) {
        return Math.abs(ChronoUnit.DAYS.between(date, LocalDate.now()) / 365.0);
    }

    private double calculateYearsDiff(LocalDate date, LocalDate laterDate) {
        return Math.abs(ChronoUnit.DAYS.between(date, laterDate) / 365.0);
    }

    @GetMapping("revenue_growth_rate")
    public List<SimpleDataElement> getRevenueGrowthRate(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 1; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = GrowthCalculator.getRevenueGrowthInInterval(company.financials, yearsAgo, 0);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/pietrosky_score")
    public List<SimpleDataElement> getPietroskyScore(@PathVariable("stock") String stock) {
        return getIncomeDataCompany(stock,
                (financialsTtm, company) -> {
                    Optional<Integer> growth = PietroskyScoreCalculator.calculatePietroskyScore(company, financialsTtm);
                    return (double) growth.orElse(0);
                });
    }

    @GetMapping("/fcf_growth_rate")
    public List<SimpleDataElement> getFcfGrowthRate(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 1; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = GrowthCalculator.getFcfGrowthInInterval(company.financials, yearsAgo, 0);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/fcf_growth_rate_7yr_moving_avg")
    public List<SimpleDataElement> get7yrFcfGrowthRateMovingAvg(@PathVariable("stock") String stock, @RequestParam(name = "year", defaultValue = "7") int year) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = GrowthCalculator.getFcfGrowthInInterval(company.financials, yearsAgo + year, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/price_growth_rate")
    public List<SimpleDataElement> getPriceGrowth(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        double latestPrice = company.latestPrice;
        for (int i = 4; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            double growth = GrowthCalculator.calculateGrowth(latestPrice, element.price, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth));
        }

        return result;
    }

    @GetMapping("/price_with_dividends_growth_rate")
    public List<SimpleDataElement> getPriceGrowthWithDividends(@PathVariable("stock") String stock) {
        List<SimpleDateDataElement> company = ReturnWithDividendCalculator.getPriceWithDividendsReinvested(DataLoader.readFinancials(stock));
        if (company.size() <= 0) {
            return List.of();
        }
        double now = company.get(0).value;
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 4; i < company.size(); ++i) {
            SimpleDateDataElement element = company.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            double growth = GrowthCalculator.calculateGrowth(now, element.value, yearsAgo);
            result.add(new SimpleDataElement(element.date.toString(), growth));
        }

        return result;
    }

    @GetMapping("/price_growth_rate_xyr_moving_avg")
    public List<SimpleDataElement> getXyrPriceGrowthRateMovingAvg(@PathVariable("stock") String stock, @RequestParam(name = "year", defaultValue = "7") int yearInterval) {
        List<SimpleDateDataElement> company = ReturnWithDividendCalculator.getPriceWithDividendsReinvested(DataLoader.readFinancials(stock));
        if (company.size() <= 2) {
            return List.of();
        }
        company.remove(0);

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.size(); ++i) {
            SimpleDateDataElement element = company.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            int newIndex = findIndexWithOrBeforeDate(company, CommonConfig.NOW.minusMonths((int) (yearsAgo * 12.0)));
            int oldIndex = findIndexWithOrBeforeDate(company, CommonConfig.NOW.minusMonths((int) (yearsAgo * 12.0 + yearInterval * 12.0)));

            if (oldIndex == -1 || newIndex == -1) {
                break;
            } else {
                double oldPrice = company.get(oldIndex).value;
                double newPrice = company.get(newIndex).value;

                double yearsDiff = calculateYearsDiff(company.get(oldIndex).date, company.get(newIndex).date);

                Double growth = GrowthCalculator.calculateGrowth(newPrice, oldPrice, yearsDiff);

                if (!Double.isFinite(growth)) {
                    growth = null;
                }

                result.add(new SimpleDataElement(element.getDate().toString(), growth));
            }
        }

        if (result.size() == 0) {
            LocalDate now = LocalDate.now();
            result.add(new SimpleDataElement(now.minusMonths(1).toString(), null));
            result.add(new SimpleDataElement(now.toString(), null));
        }

        return result;
    }

    @GetMapping("/share_count_growth_rate")
    public List<SimpleDataElement> getShareGrowthRate(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 1; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = GrowthCalculator.getShareCountGrowthInInterval(company.financials, yearsAgo, 0);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/flags")
    public List<FlagInformation> getFlags(@PathVariable("stock") String stock) {
        List<FlagInformation> result = new ArrayList<>();
        CompanyFinancials company = DataLoader.readFinancials(stock);

        for (var element : flagProviers) {
            element.addFlags(company, result, 0.0);
        }

        Collections.sort(result, (a, b) -> a.type.compareTo(b.type));

        return result;
    }

    @GetMapping("/cape_ratio")
    public List<SimpleDataElement> getCapeRatio(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);
            Double growth = CapeCalculator.calculateCapeRatioQ(company.financials, 6, i);
            result.add(new SimpleDataElement(financialsTtm.date.toString(), growth));
        }

        return result;
    }

    // EM
    @GetMapping("/5_year_pe")
    public List<SimpleDataElement> get5YearPe(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = EverythingMoneyCalculator.calculateFiveYearPe(company, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/5_year_pfcf")
    public List<SimpleDataElement> get5YearPfcf(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = EverythingMoneyCalculator.calculateFiveYearPe(company, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/ltl_per_5yr_fcf")
    public List<SimpleDataElement> getLtlPer5YrFcf(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = EverythingMoneyCalculator.calculateLtlPer5YrFcf(company, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/5_year_roic")
    public List<SimpleDataElement> get5YearRoic(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = EverythingMoneyCalculator.calculateFiveYearRoic(company, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/5_year_rev_growth")
    public List<SimpleDataElement> get5YearRevGrowth(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = EverythingMoneyCalculator.calculateFiveYearRevenueGrowth(company, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/5_year_netincome_growth")
    public List<SimpleDataElement> get5YearNetIncomeGrowth(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = EverythingMoneyCalculator.calculate5YearNetIncomeGrowth(company, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/5_year_fcf_growth")
    public List<SimpleDataElement> get5YearFcfGrowth(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = EverythingMoneyCalculator.calculate5YearFcfGrowth(company, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/5_year_share_growth")
    public List<SimpleDataElement> get5YearShareGrowth(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm element = company.financials.get(i);
            double yearsAgo = calculateYearsAgo(element.getDate());
            Optional<Double> growth = EverythingMoneyCalculator.calculate5YearShareGrowth(company, yearsAgo);
            result.add(new SimpleDataElement(element.getDate().toString(), growth.orElse(0.0)));
        }

        return result;
    }

    private List<SimpleDataElement> getIncomeData(String stock, boolean quarterly, Function<FinancialsTtm, ? extends Number> dataSupplier) {
        CompanyFinancials company = DataLoader.readFinancials(stock);

        return getIncomeData(company, quarterly, dataSupplier);
    }

    private List<SimpleDataElement> getPriceIncomeData(String stock, boolean quarterly, BiFunction<Double, FinancialsTtm, ? extends Number> dataSupplier) {
        CompanyFinancials company = DataLoader.readFinancials(stock);

        return getPriceIncomeData(company, quarterly, dataSupplier);
    }

    private List<SimpleDataElement> getIncomeData(CompanyFinancials company, boolean quarterly, Function<FinancialsTtm, ? extends Number> dataSupplier) {
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);
            if (quarterly) {
                financialsTtm = new FinancialsTtm(financialsTtm, false);
            }
            Double value = Optional.ofNullable(dataSupplier.apply(financialsTtm)).map(a -> a.doubleValue()).orElse(null);
            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), value));
        }
        return result;
    }

    private List<SimpleDataElement> getPriceIncomeData(CompanyFinancials company, boolean quarterly, BiFunction<Double, FinancialsTtm, ? extends Number> dataSupplier) {
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);
            if (quarterly) {
                financialsTtm = new FinancialsTtm(financialsTtm, false);
            }
            Double value = Optional.ofNullable(dataSupplier.apply(financialsTtm.price, financialsTtm)).map(a -> a.doubleValue()).orElse(null);
            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), value));
        }
        if (company.financials.size() > 0 && company.latestPriceDate.compareTo(company.financials.get(0).getDate()) > 0) {
            Double value = Optional.ofNullable(dataSupplier.apply(company.latestPrice, company.financials.get(0))).map(a -> a.doubleValue()).orElse(null);
            result.add(0, new SimpleDataElement(company.latestPriceDate.toString(), value));
        }
        return result;
    }

    private List<SimpleDataElement> getIncomeDataCompany(String stock, BiFunction<FinancialsTtm, CompanyFinancials, ? extends Number> dataSupplier) {
        CompanyFinancials company = DataLoader.readFinancials(stock);

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);
            Double value = Optional.ofNullable(dataSupplier.apply(financialsTtm, company)).map(a -> a.doubleValue()).orElse(null);
            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), value));
        }
        return result;
    }

    private Double toPercent(Double grossProfitMargin) {
        if (grossProfitMargin != null && Double.isFinite(grossProfitMargin)) {
            return grossProfitMargin * 100.0;
        } else {
            return null;
        }
    }
}
