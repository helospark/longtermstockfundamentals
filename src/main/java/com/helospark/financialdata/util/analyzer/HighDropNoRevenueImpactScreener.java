package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.Helpers;

public class HighDropNoRevenueImpactScreener implements StockScreeners {

    public static void main(String[] args) {
        Set<String> symbols = DataLoader.provideSymbolsFromNasdaqNyse();

        new HighDropNoRevenueImpactScreener().analyze(symbols);
    }

    @Override
    public void analyze(Set<String> symbols) {
        for (int i = 5; i < 25; ++i) {
            System.out.println("\n--------- " + i + " ----------");
            findAtIndex(symbols, i);
        }

        System.out.println("\n--------- CURRENT ----------");
        findAtIndex(symbols, 0);
    }

    public void findAtIndex(Set<String> symbols, double years) {
        System.out.println("symbol\tdeltaRev\tdeltaPrice\trev_CAGR");

        InvestmentReturn investmentReturn = new InvestmentReturn();

        symbols.parallelStream().forEach(symbol -> {
            CompanyFinancials company = DataLoader.readFinancials(symbol);
            var financials = company.financials;

            int index = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.now().minusDays((int) (years * 365)));

            if (index == -1 || financials.isEmpty() || financials.size() <= index) {
                return;
            }

            var financialsTtm = financials.get(index);

            double maxPrice = 0.0;
            double revenueThen = 0.0;

            for (int i = index; i < financials.size(); ++i) {
                var financial = financials.get(i);
                if (financial.priceUsd > maxPrice) {
                    maxPrice = financial.priceUsd;
                    revenueThen = financial.incomeStatementTtm.revenue;
                }
            }

            double currentPrice = financialsTtm.priceUsd;
            double currentRevenue = financialsTtm.incomeStatementTtm.revenue;

            double deltaPrice = currentPrice / maxPrice;
            double deltaRev = currentRevenue / revenueThen;

            if (deltaPrice < 0.45 && deltaRev > 1.2 && Double.isFinite(deltaRev) && Double.isFinite(deltaRev)) {
                Optional<Double> twoYearAvgGrowth = GrowthCalculator.getRevenueGrowthInInterval(financials, years + 2.0, years);
                double altman = AltmanZCalculator.calculateAltmanZScore(financialsTtm, financialsTtm.price);

                double priceNow = financials.get(0).price;

                if (twoYearAvgGrowth.isPresent() && twoYearAvgGrowth.get() > 20 && altman > 2.5) {
                    double twoYearGrowth = twoYearAvgGrowth.get();
                    investmentReturn.investedTotal += 1000;
                    investmentReturn.nowTotal += ((1000.0 / financialsTtm.price) * priceNow);
                    System.out.printf("%s\t%.2f%% \t -%.2f%% \t %.2f%% \n", symbol, deltaRev * 100.0, (1.0 - deltaPrice) * 100.0, twoYearGrowth);
                }
            }
        });

        double investmentGrowth = GrowthCalculator.calculateGrowth(investmentReturn.nowTotal, investmentReturn.investedTotal, years);
        System.out.printf("-- %.2f%% CAGR; $%.2f -> $%.2f", investmentGrowth, investmentReturn.investedTotal, investmentReturn.nowTotal);
    }

    Optional<Double> getEpsExcludingMarketing(FinancialsTtm financialsTtm) {
        if (financialsTtm.incomeStatementTtm.sellingAndMarketingExpenses < financialsTtm.incomeStatementTtm.costAndExpenses) {
            return Optional
                    .of(((double) financialsTtm.incomeStatementTtm.netIncome + financialsTtm.incomeStatementTtm.sellingAndMarketingExpenses) / financialsTtm.incomeStatementTtm.weightedAverageShsOut);
        } else {
            return Optional.empty();
        }
    }

    static class InvestmentReturn {
        double investedTotal = 0.0;
        double nowTotal = 0.0;

    }

}
