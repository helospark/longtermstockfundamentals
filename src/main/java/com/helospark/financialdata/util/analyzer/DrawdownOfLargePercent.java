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

public class DrawdownOfLargePercent implements StockScreeners {
    private static final int DRAWDOWN_THRESOLHOLD = 70;

    private static int recoveryCompanyCount = 0;
    private static int drawDownCompanyCount = 0;
    private static long recoveryTime = 0;
    private static int allDrawDownCount = 0;

    public static void main(String[] args) {
        Set<String> symbols = new TreeSet<>(DataLoader.provideSymbolsFromNasdaqNyse());
        //                .stream()
        //                .flatMap(a -> symbolAtGlanceProvider.getAtGlanceData(a).stream())
        //                .filter(a -> a.price10Gr > 15.0)
        //                .map(a -> a.symbol)
        //                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        new DrawdownOfLargePercent().analyze(symbols);
    }

    @Override
    public void analyze(Set<String> symbols) {
        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();

        symbols.stream().forEach(symbol -> {
            List<HistoricalPriceElement> prices = DataLoader.readHistoricalPriceNoCache(symbol);

            Optional<AtGlanceData> data = symbolAtGlanceProvider.getAtGlanceData(symbol);
            if (!data.isPresent() || data.get().marketCapUsd < 1000) {
                return;
            }

            int lastIndex = prices.size() - 1;
            double min = prices.get(lastIndex).close, max = prices.get(lastIndex).close;
            LocalDate maxDate = prices.get(lastIndex).date;
            LocalDate minDate = prices.get(lastIndex).date;
            boolean hasThereBeenPrint = false;
            boolean hasRecoveredAtLeastOnce = false;
            boolean hasDrawDownAtLeastOnce = false;
            for (int i = lastIndex; i >= 0; --i) {
                double price = prices.get(i).close;
                LocalDate date = prices.get(i).date;

                if (price > max) {
                    double drawDown = ((1.0 - (min / max)) * 100.0);
                    if (drawDown > DRAWDOWN_THRESOLHOLD) {
                        System.out.println(
                                symbol + " drawdown " + ((int) Math.round(drawDown)) + "% in " + ChronoUnit.DAYS.between(maxDate, minDate) + " days between (" + maxDate + " - " + minDate
                                        + ") new ATH at: " + date);
                        hasThereBeenPrint = true;
                        hasDrawDownAtLeastOnce = true;
                        hasRecoveredAtLeastOnce = true;
                        recoveryTime += ChronoUnit.DAYS.between(minDate, date);
                        allDrawDownCount += 1;
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
            double endPrice = prices.get(0).close;
            double drawDown = ((1.0 - (min / max)) * 100.0);
            double newDrawDown = ((1.0 - (endPrice / max)) * 100.0);
            if (endPrice < max * (100 - DRAWDOWN_THRESOLHOLD) / 100.0) {
                System.out.println(
                        symbol + " drawdown " + ((int) Math.round(drawDown)) + "% since " + maxDate + " has not recovered, currently at " + Math.round(newDrawDown) + "%");
                hasThereBeenPrint = true;
                hasDrawDownAtLeastOnce = true;
            } else if (drawDown > DRAWDOWN_THRESOLHOLD && newDrawDown < 20) {

                System.out.println(
                        symbol + " drawdown " + ((int) Math.round(drawDown)) + "% in " + ChronoUnit.DAYS.between(maxDate, minDate) + " days between (" + maxDate + " - " + minDate
                                + ") recovered to only " + Math.round(newDrawDown) + "%");
                hasThereBeenPrint = true;
                hasDrawDownAtLeastOnce = true;
            }
            drawDownCompanyCount += (hasDrawDownAtLeastOnce ? 1 : 0);
            recoveryCompanyCount += (hasRecoveredAtLeastOnce ? 1 : 0);
            if (hasThereBeenPrint) {
                System.out.println();
            }

        });

        System.out.println("Drawdowns of " + DRAWDOWN_THRESOLHOLD + " recover with " + Math.round((double) recoveryCompanyCount / drawDownCompanyCount) * 100.0 + "% of times");
        System.out.println("Average recovery time: " + Math.round(((double) recoveryTime / allDrawDownCount)) + " days");
    }

}
