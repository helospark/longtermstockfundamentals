package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.exchanges.Exchanges;

public class NetNetFinder implements StockScreeners {

    public static void main(String[] args) {
        Set<String> symbols = new TreeSet<>(DataLoader.provideSymbolsIn(Exchanges.HKSE));
        //                .stream()
        //                .flatMap(a -> symbolAtGlanceProvider.getAtGlanceData(a).stream())
        //                .filter(a -> a.price10Gr > 15.0)
        //                .map(a -> a.symbol)
        //                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        //        new NetNetFinder().analyze(symbols);
        new NetNetFinder().isNetNet("H02.SI", DataLoader.readFinancials("H02.SI"));
    }

    @Override
    public void analyze(Set<String> symbols) {
        symbols.parallelStream().forEach(symbol -> {
            CompanyFinancials financials = DataLoader.readFinancials(symbol);

            isNetNet(symbol, financials);

        });
    }

    public void isNetNet(String symbol, CompanyFinancials financials) {
        if (financials.financials.size() > 10) {
            FinancialsTtm asd = financials.financials.get(0);

            double diff = asd.balanceSheet.totalCurrentAssets - asd.balanceSheet.totalLiabilities;

            double diffUsd = DataLoader.convertFx(diff, financials.profile.reportedCurrency, "USD", LocalDate.now(), false).orElse(diff);
            double marketCapUsd = financials.latestPriceUsd * asd.incomeStatement.weightedAverageShsOut;

            if (diff > 0 && marketCapUsd < diffUsd) {
                System.out.printf("%s\t%2f\t%d\n", symbol, marketCapUsd / diffUsd, (int) (marketCapUsd / 1_000_000));
            }
        }
    }

}
