package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.domain.SimpleDateDataElement;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.DrawDownService;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class MaxDrawdownFinderForStocksUsuallyNearAth implements StockScreeners {

    public static void main(String[] args) {
        Set<String> symbols = new TreeSet<>(DataLoader.provideSymbolsFromNasdaqNyse());
        //                .stream()
        //                .flatMap(a -> symbolAtGlanceProvider.getAtGlanceData(a).stream())
        //                .filter(a -> a.price10Gr > 15.0)
        //                .map(a -> a.symbol)
        //                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        new MaxDrawdownFinderForStocksUsuallyNearAth().analyze(symbols);
    }

    @Override
    public void analyze(Set<String> symbols) {
        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();

        symbols.stream().forEach(symbol -> {
            List<HistoricalPriceElement> prices = DataLoader.readHistoricalPriceNoCache(symbol);

            Optional<AtGlanceData> data = symbolAtGlanceProvider.getAtGlanceData(symbol);
            if (!data.isPresent()) {
                return;
            }

            AtGlanceData atGlanceData = data.get();
            if (prices.size() < 200 || atGlanceData.marketCapUsd < 1000) {
                return;
            }

            List<SimpleDateDataElement> drawDown = DrawDownService.getDrawdownChart(prices);

            if (drawDown.size() < 20) {
                return;
            }

            double growth = atGlanceData.revenueGrowth;
            double percentageNearTop = DrawDownService.getPercentageNearTop(prices, LocalDate.now(), 20.0);
            Double currentDrawdown = drawDown.get(0).value;

            //            System.out.println(symbol + "\t\t" + percentageNearTop + "\t\t" + currentDrawdown);

            if (currentDrawdown < 60 && percentageNearTop > 40 && growth > 5) {
                System.out.printf("%s\t: %.1f%%, %.1f%%, %.1f%%\n", symbol, 100.0 - currentDrawdown, percentageNearTop, growth);
            }
        });
    }

}
