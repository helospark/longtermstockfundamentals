package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class MaxDrawdownFinderWithGrowth implements StockScreeners {

    public static void main(String[] args) {
        Set<String> symbols = new TreeSet<>(DataLoader.provideSymbolsFromNasdaqNyse());
        //                .stream()
        //                .flatMap(a -> symbolAtGlanceProvider.getAtGlanceData(a).stream())
        //                .filter(a -> a.price10Gr > 15.0)
        //                .map(a -> a.symbol)
        //                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        new MaxDrawdownFinderWithGrowth().analyze(symbols);
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
            if (prices.size() < 200 || atGlanceData.marketCapUsd < 1000 || !Double.isFinite(atGlanceData.price5Gr) || !Double.isFinite(atGlanceData.price10Gr)) {
                return;
            }

            int lastIndex = prices.size() - 1;
            double min = prices.get(lastIndex).close, max = prices.get(lastIndex).close;
            LocalDate maxDate = prices.get(lastIndex).date;
            LocalDate minDate = prices.get(lastIndex).date;

            String result = "";
            for (int i = lastIndex; i >= 0; --i) {
                double price = prices.get(i).close;
                LocalDate date = prices.get(i).date;

                if (price > max || i == 0) {
                    double drawDown = ((1.0 - (min / max)) * 100.0);
                    long distanceInDays = ChronoUnit.DAYS.between(maxDate, minDate);
                    if (drawDown > 30.0 && distanceInDays > 2 * 365) {
                        Optional<AtGlanceData> historicalGlanceData = symbolAtGlanceProvider.loadAtGlanceDataClosestToDate(minDate).map(q -> q.get(symbol));
                        if (historicalGlanceData.isPresent() && historicalGlanceData.get().revenueGrowth > 5.0 && historicalGlanceData.get().epsGrowth > 7.0) {
                            result += (symbol + " drawdown " + ((int) Math.round(drawDown)) + "% in " + distanceInDays + " days between (" + maxDate + " - " + minDate + ") while revGr=" + historicalGlanceData.get().revenueGrowth + " EpsGr="
                                    + historicalGlanceData.get().epsGrowth);
                            if (i != 0) {
                                result += "; recovered on " + date;
                            }
                            result += "\n";
                        }
                    }

                    maxDate = date;
                    max = price;
                    min = price;
                }
                if (price < min) {
                    minDate = date;
                    min = price;
                }
            }
            if (!result.isEmpty()) {
                System.out.printf("%s\tCAGR: %.1f%%, %.1f%%, %.1f%%, %.1f%%\n", symbol, +atGlanceData.price5Gr, atGlanceData.price10Gr, atGlanceData.price15Gr, atGlanceData.price20Gr);
                System.out.println(result);
                System.out.println();
            }
        });
    }

}
