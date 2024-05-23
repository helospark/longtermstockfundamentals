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

public class EpsExcludingMarketingScreener implements StockScreeners {

    public static void main(String[] args) {
        Set<String> symbols = DataLoader.provideSymbolsFromNasdaqNyse();

        new EpsExcludingMarketingScreener().analyze(symbols);
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
        System.out.println("symbol\t(2yrgrowth, PE, peExcRnd, peExclMarketing) growthSince");

        InvestmentReturn investmentReturn = new InvestmentReturn();

        symbols.parallelStream().forEach(symbol -> {
            CompanyFinancials company = DataLoader.readFinancials(symbol);
            var financials = company.financials;

            int index = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.now().minusDays((int) (years * 365)));

            if (index == -1 || financials.isEmpty() || financials.size() <= index) {
                return;
            }

            var financialsTtm = financials.get(index);
            double epsExcludingRnd = ((double) financialsTtm.incomeStatementTtm.netIncome + financialsTtm.incomeStatementTtm.researchAndDevelopmentExpenses)
                    / financialsTtm.incomeStatementTtm.weightedAverageShsOut;
            Optional<Double> epsExclMarketing = getEpsExcludingMarketing(financialsTtm);
            double eps = financialsTtm.incomeStatementTtm.eps;

            double peExclRnd = financialsTtm.price / epsExcludingRnd;
            double regularPe = financialsTtm.price / eps;
            Optional<Double> peExcludingMarketing = epsExclMarketing.map(as -> financialsTtm.price / as);
            Optional<Double> twoYearAvgGrowth = GrowthCalculator.getRevenueGrowthInInterval(financials, years + 2.0, years);
            boolean isBarelyProfitable = (regularPe > 60 || regularPe < 0);
            double altman = AltmanZCalculator.calculateAltmanZScore(financialsTtm, financialsTtm.price);

            double priceNow = financials.get(0).price;
            double growthTillToday = GrowthCalculator.calculateGrowth(priceNow, financialsTtm.price, years);

            if (twoYearAvgGrowth.isPresent() && twoYearAvgGrowth.get() > 20 && altman > 3 && (
            /*(peExclRnd > 0 && peExclRnd < 30 && isBarelyProfitable) ||*/
            (peExcludingMarketing.isPresent() && peExcludingMarketing.get() > 0 && peExcludingMarketing.get() < 30 && isBarelyProfitable))) {
                double twoYearGrowth = twoYearAvgGrowth.get();
                investmentReturn.investedTotal += 1000;
                investmentReturn.nowTotal += ((1000.0 / financialsTtm.price) * priceNow);
                System.out.printf("%s\t(%.2f%%, %.2f, %.2f, %.2f)\t%.2f%%  \n", symbol, twoYearGrowth, regularPe, peExclRnd, peExcludingMarketing.orElse(Double.NaN), growthTillToday);
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
