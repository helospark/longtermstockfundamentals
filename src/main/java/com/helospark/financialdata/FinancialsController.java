package com.helospark.financialdata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.SimpleDataElement;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.CapeCalculator;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.DcfCalculator;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.RevenueProjector;
import com.helospark.financialdata.service.TrailingPegCalculator;

@RestController
@RequestMapping("/{stock}/financials")
public class FinancialsController {
    @Autowired
    CapeCalculator capeCalculator;

    @GetMapping("/eps")
    public List<SimpleDataElement> getEps(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.incomeStatementTtm.eps);
    }

    @GetMapping("/revenue")
    public List<SimpleDataElement> getRevenue(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.incomeStatementTtm.revenue);
    }

    @GetMapping("/fcf")
    public List<SimpleDataElement> getFcf(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.cashFlowTtm.freeCashFlow);
    }

    @GetMapping("/pfcf")
    public List<SimpleDataElement> getPFcf(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> (double) financialsTtm.cashFlowTtm.freeCashFlow / financialsTtm.incomeStatementTtm.weightedAverageShsOut);
    }

    @GetMapping("/operating_margin")
    public List<SimpleDataElement> getOperativeMargin(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> toPercent(financialsTtm.remoteRatio.operatingProfitMargin));
    }

    @GetMapping("/gross_margin")
    public List<SimpleDataElement> getGrossMargin(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> toPercent(financialsTtm.remoteRatio.grossProfitMargin));
    }

    @GetMapping("/net_margin")
    public List<SimpleDataElement> getNetMargin(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> toPercent(financialsTtm.remoteRatio.netProfitMargin));
    }

    @GetMapping("/pe_ratio")
    public List<SimpleDataElement> getPeMargin(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.price / financialsTtm.incomeStatementTtm.eps);
    }

    @GetMapping("/past_pe_to_growth_ratio")
    public List<SimpleDataElement> getTrailingPegMargin(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);
            Optional<Double> value = TrailingPegCalculator.calculateTrailingPeg(company, i);

            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), value.orElse(null)));
        }
        return result;
    }

    @GetMapping("/pfcf_ratio")
    public List<SimpleDataElement> getFcfMargin(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.price / ((double) financialsTtm.cashFlowTtm.freeCashFlow / financialsTtm.incomeStatementTtm.weightedAverageShsOut));
    }

    @GetMapping("/p2g_ratio")
    public List<SimpleDataElement> getP2gMargin(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.remoteRatio.priceToBookRatio);
    }

    @GetMapping("/tax_rate")
    public List<SimpleDataElement> getTaxRate(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.remoteRatio.effectiveTaxRate);
    }

    @GetMapping("/quick_ratio")
    public List<SimpleDataElement> getQuickRatio(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.remoteRatio.quickRatio);
    }

    @GetMapping("/short_term_coverage_ratio")
    public List<SimpleDataElement> getShortTermCoverageRatio(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.remoteRatio.shortTermCoverageRatios);
    }

    @GetMapping("/short_term_assets_to_total_debt")
    public List<SimpleDataElement> getShortTermAssetsToTotalDebt(@PathVariable("stock") String stock) {
        return getIncomeData(stock,
                financialsTtm -> financialsTtm.balanceSheet.longTermDebt > 0 ? (double) financialsTtm.balanceSheet.totalCurrentAssets / financialsTtm.balanceSheet.totalDebt : null);
    }

    @GetMapping("/return_on_assets")
    public List<SimpleDataElement> getReturnOnAssets(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.remoteRatio.returnOnAssets);
    }

    @GetMapping("/return_on_capital")
    public List<SimpleDataElement> getReturnOnCapital(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.remoteRatio.returnOnCapitalEmployed);
    }

    @GetMapping("/return_on_equity")
    public List<SimpleDataElement> getReturnOnEquity(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.remoteRatio.returnOnEquity);
    }

    @GetMapping("/cash_flow_to_debt")
    public List<SimpleDataElement> getCashFlowToDebt(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.remoteRatio.cashFlowToDebtRatio);
    }

    @GetMapping("/share_count")
    public List<SimpleDataElement> getShareCount(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.incomeStatementTtm.weightedAverageShsOut);
    }

    @GetMapping("/interest_expense")
    public List<SimpleDataElement> getInterestExpense(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.incomeStatementTtm.interestExpense);
    }

    @GetMapping("/interest_rate")
    public List<SimpleDataElement> getInterestRate(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> {
            if (financialsTtm.balanceSheet.totalDebt > 0 && financialsTtm.incomeStatementTtm.interestExpense > 0) {
                return toPercent((double) financialsTtm.incomeStatementTtm.interestExpense / financialsTtm.balanceSheet.totalDebt);
            } else {
                return null;
            }
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

            double dividend = (double) -financialsTtm.cashFlowTtm.dividendsPaid / financialsTtm.incomeStatementTtm.weightedAverageShsOut;

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
            Optional<Double> growthInYear = GrowthCalculator.getGrowthInInterval(company.financials, offset + year, year);
            if (growthInYear.isPresent() && !growthInYear.get().isNaN()) {
                growthRateList.add(growthInYear.get());
            }
        }
        for (int offset = 7; offset >= 3; --offset) {
            Optional<Double> growthInYear = GrowthCalculator.getGrowthInInterval(company.financials, offset + year + 3, 3);
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
    public List<SimpleDataElement> getCash(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.balanceSheet.cashAndCashEquivalents);
    }

    @GetMapping("/current_assets")
    public List<SimpleDataElement> getTotalCurrentAssets(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.balanceSheet.totalCurrentAssets);
    }

    @GetMapping("/current_liabilities")
    public List<SimpleDataElement> getTotalCurrentLiabilities(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.balanceSheet.totalCurrentLiabilities);
    }

    @GetMapping("/long_term_debt")
    public List<SimpleDataElement> getLongTermDebt(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.balanceSheet.longTermDebt);
    }

    @GetMapping("/non_current_assets")
    public List<SimpleDataElement> getLongTermLiabilities(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.balanceSheet.otherNonCurrentAssets);
    }

    @GetMapping("/price")
    public List<SimpleDataElement> getPrice(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.price);
    }

    @GetMapping("/stock_compensation")
    public List<SimpleDataElement> getStockBasedCompensation(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.cashFlowTtm.stockBasedCompensation);
    }

    @GetMapping("/stock_compensation_per_net_income")
    public List<SimpleDataElement> getStockBasedCompensationPerNetIncome(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> (double) financialsTtm.cashFlowTtm.stockBasedCompensation / financialsTtm.incomeStatementTtm.netIncome * 100.0);
    }

    @GetMapping("/roic")
    public List<SimpleDataElement> getRoic(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> toPercent(financialsTtm.keyMetrics.roic));
    }

    @GetMapping("/capex_to_revenue")
    public List<SimpleDataElement> getCapexToRevenue(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> toPercent(financialsTtm.keyMetrics.capexToRevenue) * -1.0);
    }

    @GetMapping("/cash_per_share")
    public List<SimpleDataElement> getCashPerShare(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.keyMetrics.cashPerShare);
    }

    @GetMapping("/ev_over_ebitda")
    public List<SimpleDataElement> getEvOverEbitda(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.keyMetrics.enterpriseValueOverEBITDA);
    }

    @GetMapping("/graham_number")
    public List<SimpleDataElement> getGrahamNumber(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> financialsTtm.keyMetrics.grahamNumber);
    }

    @GetMapping("/stock_compensation_per_net_revenue")
    public List<SimpleDataElement> getStockBasedCompensationPerRevenue(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> (double) financialsTtm.cashFlowTtm.stockBasedCompensation / financialsTtm.incomeStatementTtm.revenue * 100.0);
    }

    @GetMapping("/dividend_yield")
    public List<SimpleDataElement> getDividendYield(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> toPercent((double) -financialsTtm.cashFlowTtm.dividendsPaid / financialsTtm.incomeStatementTtm.weightedAverageShsOut / financialsTtm.price));
    }

    @GetMapping("/dividend_payout_ratio")
    public List<SimpleDataElement> getPayoutRatio(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> toPercent(financialsTtm.remoteRatio.dividendPayoutRatio));
    }

    @GetMapping("/dividend_payout_ratio_with_fcf")
    public List<SimpleDataElement> getPayoutRatioFcf(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> toPercent((double) -financialsTtm.cashFlowTtm.dividendsPaid / financialsTtm.cashFlowTtm.freeCashFlow));
    }

    @GetMapping("/dividend_paid")
    public List<SimpleDataElement> getDividendPaid(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> (double) -financialsTtm.cashFlowTtm.dividendsPaid / financialsTtm.incomeStatementTtm.weightedAverageShsOut);
    }

    @GetMapping("/dividend_yield_per_current_price")
    public List<SimpleDataElement> getDividendPerCurrentPrice(@PathVariable("stock") String stock) {
        return getIncomeDataCompany(stock,
                (financialsTtm, company) -> toPercent((double) -company.financials.get(0).cashFlowTtm.dividendsPaid / financialsTtm.incomeStatementTtm.weightedAverageShsOut / financialsTtm.price));
    }

    @GetMapping("/altmanz")
    public List<SimpleDataElement> getAltmanZ(@PathVariable("stock") String stock) {
        return getIncomeData(stock, financialsTtm -> AltmanZCalculator.calculateAltmanZScore(financialsTtm, financialsTtm.price));
    }

    @GetMapping("/eps_growth_rate")
    public List<SimpleDataElement> getGrowthRate(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 1; i < 30 * 4; ++i) {
            Optional<Double> growth = GrowthCalculator.getGrowthInInterval(company.financials, i / 4.0, 0);
            result.add(new SimpleDataElement(LocalDate.now().minusMonths((long) (i * 4.0)).toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/dividend_growth_rate")
    public List<SimpleDataElement> getDividendRate(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 1; i < 30 * 4; ++i) {
            Optional<Double> growth = GrowthCalculator.getDividendGrowthInInterval(company.financials, i / 4.0, 0);
            if (growth.isPresent() && !growth.get().isInfinite() && !growth.get().isNaN()) {
                result.add(new SimpleDataElement(LocalDate.now().minusMonths((long) (i * 4.0)).toString(), growth.orElse(0.0)));
            } else {
                result.add(new SimpleDataElement(LocalDate.now().minusMonths((long) (i * 4.0)).toString(), null));
            }
        }

        return result;
    }

    @GetMapping("/eps_growth_rate_7yr_moving_avg")
    public List<SimpleDataElement> get7yrGrowthRateMovingAvg(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < 23 * 4; ++i) {
            double yearsAgo = i / 4.0;
            Optional<Double> growth = GrowthCalculator.getGrowthInInterval(company.financials, yearsAgo + 7, yearsAgo);
            result.add(new SimpleDataElement(LocalDate.now().minusMonths((long) (yearsAgo * 12.0)).toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("revenue_growth_rate")
    public List<SimpleDataElement> getRevenueGrowthRate(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 1; i < 30 * 4; ++i) {
            double yearsAgo = i / 4.0;
            Optional<Double> growth = GrowthCalculator.getRevenueGrowthInInterval(company.financials, yearsAgo, 0);
            result.add(new SimpleDataElement(LocalDate.now().minusMonths((long) (yearsAgo * 12.0)).toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/fcf_growth_rate")
    public List<SimpleDataElement> getFcfGrowthRate(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 1; i < 30 * 4; ++i) {
            double yearsAgo = i / 4.0;
            Optional<Double> growth = GrowthCalculator.getFcfGrowthInInterval(company.financials, yearsAgo, 0);
            result.add(new SimpleDataElement(LocalDate.now().minusMonths((long) (yearsAgo)).toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/fcf_growth_rate_7yr_moving_avg")
    public List<SimpleDataElement> get7yrFcfGrowthRateMovingAvg(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < 23 * 4; ++i) {
            double yearsAgo = i / 4.0;
            Optional<Double> growth = GrowthCalculator.getFcfGrowthInInterval(company.financials, yearsAgo + 7, yearsAgo);
            result.add(new SimpleDataElement(LocalDate.now().minusMonths((long) (yearsAgo * 12.0)).toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/price_growth_rate")
    public List<SimpleDataElement> getPriceGrowth(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 1; i < 30 * 4; ++i) {
            double yearsAgo = i / 4.0;
            Optional<Double> growth = GrowthCalculator.getPriceGrowthInInterval(company.financials, yearsAgo, 0);
            result.add(new SimpleDataElement(LocalDate.now().minusMonths((long) (yearsAgo * 12.0)).toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/share_count_growth_rate")
    public List<SimpleDataElement> getShareGrowthRate(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 1; i < 30 * 4; ++i) {
            double yearsAgo = i / 4.0;
            Optional<Double> growth = GrowthCalculator.getShareCountGrowthInInterval(company.financials, yearsAgo, 0);
            result.add(new SimpleDataElement(LocalDate.now().minusMonths((int) (yearsAgo * 12.0)).toString(), growth.orElse(0.0)));
        }

        return result;
    }

    @GetMapping("/cape_ratio")
    public List<SimpleDataElement> getCapeRatio(@PathVariable("stock") String stock) {
        CompanyFinancials company = DataLoader.readFinancials(stock);
        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);
            Double growth = capeCalculator.calculateCapeRatioQ(company.financials, i, 6);
            result.add(new SimpleDataElement(financialsTtm.date.toString(), growth));
        }

        return result;
    }

    private List<SimpleDataElement> getIncomeData(String stock, Function<FinancialsTtm, ? extends Number> dataSupplier) {
        CompanyFinancials company = DataLoader.readFinancials(stock);

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < company.financials.size(); ++i) {
            FinancialsTtm financialsTtm = company.financials.get(i);
            Double value = Optional.ofNullable(dataSupplier.apply(financialsTtm)).map(a -> a.doubleValue()).orElse(null);
            result.add(new SimpleDataElement(financialsTtm.getDate().toString(), value));
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
        if (grossProfitMargin != null) {
            return grossProfitMargin * 100.0;
        } else {
            return null;
        }
    }
}
