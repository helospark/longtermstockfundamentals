package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class MultiBaggerFinder implements StockScreeners {
    static Map<String, Integer> capToMultibagger = new ConcurrentHashMap<>();
    static Map<String, Integer> capToDecline = new ConcurrentHashMap<>();
    static Map<String, Integer> capToAll = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Set<String> symbols = new TreeSet<>(DataLoader.provideSymbolsFromNasdaqNyse());
        //                .stream()
        //                .flatMap(a -> symbolAtGlanceProvider.getAtGlanceData(a).stream())
        //                .filter(a -> a.price10Gr > 15.0)
        //                .map(a -> a.symbol)
        //                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        new MultiBaggerFinder().analyze(symbols);
    }

    @Override
    public void analyze(Set<String> symbols) {
        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();

        symbolAtGlanceProvider.loadAtGlanceDataAtYear(LocalDate.now().getYear() - 10, 1);

        symbols.parallelStream().forEach(symbol -> {
            if (symbol.equals("PTR")) {
                return;
            }
            Optional<AtGlanceData> data = symbolAtGlanceProvider.getAtGlanceData(symbol);
            Optional<AtGlanceData> oldData = symbolAtGlanceProvider.loadAtGlanceDataAtYear(LocalDate.now().getYear() - 10, 1).map(a -> a.get(symbol));
            if (!data.isPresent() || !oldData.isPresent()) {
                return;
            }

            if (data.get().price10Gr > 26) {
                capToMultibagger.compute(mapMarketCap(oldData.get().marketCapUsd), (a, b) -> b == null ? 1 : b + 1);
            }
            if (data.get().price10Gr < -8) {
                capToDecline.compute(mapMarketCap(oldData.get().marketCapUsd), (a, b) -> b == null ? 1 : b + 1);
                if (oldData.get().marketCapUsd > 20_000) {
                    System.out.println(symbol + " " + data.get().price10Gr);
                }
            }
            capToAll.compute(mapMarketCap(oldData.get().marketCapUsd), (a, b) -> b == null ? 1 : b + 1);
        });
        System.out.println(capToAll);
        System.out.println(capToMultibagger);
        for (var entry : capToMultibagger.entrySet()) {
            double percent = (double) entry.getValue() / capToAll.get(entry.getKey()) * 100.0;

            System.out.printf("%s\t%.2f%%\n", entry.getKey(), percent);
        }

        System.out.println("--------------");
        for (var entry : capToDecline.entrySet()) {
            double percent = (double) entry.getValue() / capToAll.get(entry.getKey()) * 100.0;

            System.out.printf("%s\t%.2f%%\n", entry.getKey(), percent);
        }
    }

    public String mapMarketCap(double marketCapUsd) {
        if (marketCapUsd < 250) {
            return "Micro-cap";
        } else if (marketCapUsd < 2_000) {
            return "Small-cap";
        } else if (marketCapUsd < 10_000) {
            return "Mid-cap";
        } else if (marketCapUsd < 200_000) {
            return "Large-cap";
        } else if (marketCapUsd >= 200_000) {
            return "Mega-cap";
        }
        return "Unknown";
    }

}
