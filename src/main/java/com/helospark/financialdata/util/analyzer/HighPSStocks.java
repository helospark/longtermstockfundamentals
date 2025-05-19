package com.helospark.financialdata.util.analyzer;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class HighPSStocks implements StockScreeners {

    public static void main(String[] args) {
        Set<String> symbols = new TreeSet<>(DataLoader.provideSymbolsFromNasdaqNyse());
        //                .stream()
        //                .flatMap(a -> symbolAtGlanceProvider.getAtGlanceData(a).stream())
        //                .filter(a -> a.price10Gr > 15.0)
        //                .map(a -> a.symbol)
        //                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        new HighPSStocks().analyze(symbols);
    }

    @Override
    public void analyze(Set<String> symbols) {
        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();

        symbols.parallelStream().forEach(symbol -> {

            for (int k = 2000; k < 2025; ++k) {
                var latestGlance = symbolAtGlanceProvider.loadAtGlanceDataAtYear(k, 1).map(a -> a.get(symbol));
                if (!latestGlance.isPresent()) {
                    continue;
                }
                for (int i = 1995; i < k - 3; ++i) {
                    for (int j = 1; j <= 12; j += 3) {
                        Optional<AtGlanceData> data = symbolAtGlanceProvider.loadAtGlanceDataAtYear(i, j).map(a -> a.get(symbol));
                        if (!data.isPresent()) {
                            continue;
                        }
                        AtGlanceData glance = data.get();
                        if (glance.pts > 90 && Float.isFinite(glance.pts) && glance.marketCapUsd > 100) {

                            Optional<Double> cagr = GrowthCalculator.calculateAnnualGrowth(glance.latestStockPriceTradingCur, glance.actualDate, latestGlance.get().latestStockPriceTradingCur,
                                    latestGlance.get().actualDate);
                            if (cagr.isPresent() && cagr.get() > 15) {
                                System.out.println(i + "-" + j + " to " + k + "-1\t\t" + symbol + " " + glance.pts + " " + cagr.get());
                            }
                        }
                    }
                }
            }

        });
    }

}
