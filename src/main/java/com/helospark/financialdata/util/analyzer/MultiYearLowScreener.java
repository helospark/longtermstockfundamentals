package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.RoicCalculator;

public class MultiYearLowScreener implements StockScreeners {

    public static void main(String[] args) {
        Set<String> symbols = DataLoader.provideSymbolsFromNasdaqNyse();

        new MultiYearLowScreener().analyze(symbols);
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
        System.out.println("symbol\tdropFromMax\tpriceFirstSeen\tgrowthSince");

        InvestmentReturn investmentReturn = new InvestmentReturn();

        symbols.parallelStream().forEach(symbol -> {
            CompanyFinancials company = DataLoader.readFinancials(symbol);
            var financials = company.financials;

            int index = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.now().minusDays((int) (years * 365)));

            if (index == -1 || financials.isEmpty() || financials.size() <= index || (financials.size() - index) < 10) {
                return;
            }

            FinancialsTtm latestFinancials = financials.get(index);
            double shareCount = latestFinancials.incomeStatementTtm.weightedAverageShsOut;
            double latestPrice = latestFinancials.price;

            double latestEps = latestFinancials.incomeStatementTtm.eps;
            double latestRevPerShare = latestFinancials.incomeStatementTtm.revenue / shareCount;
            double latestFcfPerShare = latestFinancials.cashFlowTtm.freeCashFlow / shareCount;

            boolean isEpsCloseToAllTimeHigh = true;
            boolean isRevCloseToAllTimeHigh = true;
            boolean isFcfCloseToAllTimeHigh = true;

            for (int i = index + 1; i < financials.size(); ++i) {
                FinancialsTtm thenFinancials = financials.get(i);
                double thenShareCount = latestFinancials.incomeStatementTtm.weightedAverageShsOut;

                double thenEps = thenFinancials.incomeStatementTtm.eps;
                double thenRevPerShare = thenFinancials.incomeStatementTtm.revenue / thenShareCount;
                double thenFcfPerShare = thenFinancials.cashFlowTtm.freeCashFlow / thenShareCount;

                if (latestEps * 1.1 < thenEps) {
                    isEpsCloseToAllTimeHigh = false;
                }
                if (latestFcfPerShare * 1.1 < thenFcfPerShare) {
                    isFcfCloseToAllTimeHigh = false;
                }
                if (latestRevPerShare * 1.1 < thenRevPerShare) {
                    isRevCloseToAllTimeHigh = false;
                }
            }

            double priceFirstSeenYearsAgo = -1.0;
            double maxPrice = 0.0;
            for (int i = financials.size() - 1; i >= index; --i) {
                FinancialsTtm thenFinancials = financials.get(i);

                if (thenFinancials.price >= latestPrice && priceFirstSeenYearsAgo < 0.0) {
                    priceFirstSeenYearsAgo = Math.abs(ChronoUnit.DAYS.between(thenFinancials.date, latestFinancials.date) / 365.0);
                }
                if (thenFinancials.price > maxPrice) {
                    maxPrice = thenFinancials.price;
                }
            }
            double priceDropFromMaxPercent = (100 - ((latestPrice / maxPrice) * 100.0)) * -1.0;

            double altmanZ = AltmanZCalculator.calculateAltmanZScore(latestFinancials, latestPrice);
            double roic = RoicCalculator.calculateRoic(latestFinancials);

            if (altmanZ > 2.0 && latestFcfPerShare > 0.0 && latestEps > 0.0 && roic > 0.2 &&
                    priceDropFromMaxPercent < -30 && priceFirstSeenYearsAgo > 2.0 && priceFirstSeenYearsAgo < 7.0
                    && (isEpsCloseToAllTimeHigh || isFcfCloseToAllTimeHigh) && isRevCloseToAllTimeHigh) {
                investmentReturn.investedTotal += 1000;
                investmentReturn.nowTotal += ((1000.0 / latestFinancials.price) * financials.get(0).price);
                double growthTillToday = ((financials.get(0).price / latestFinancials.price) - 1.0) * 100.0;
                System.out.printf("%s \t %.2f%% \t %.2f \t %.2f%%\n", symbol, priceDropFromMaxPercent, priceFirstSeenYearsAgo, growthTillToday);
            }
        });

        if (years > 0) {
            double investmentGrowth = GrowthCalculator.calculateGrowth(investmentReturn.nowTotal, investmentReturn.investedTotal, years);
            System.out.printf("-- %.2f%% CAGR; $%.2f -> $%.2f", investmentGrowth, investmentReturn.investedTotal, investmentReturn.nowTotal);
        }
    }

    static class InvestmentReturn {
        double investedTotal = 0.0;
        double nowTotal = 0.0;

    }

}
