package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.Helpers;

public class CalculatePerformanceForSp500Ytd {

    public static void main(String[] args) {
        Set<String> symbols = new TreeSet<>(DataLoader.provideSp500Symbols());
        int negativeCount = 0;
        int positiveCount = 0;
        int beatCount = 0;
        List<Map.Entry<String, Double>> full = new ArrayList<>();
        System.out.printf("    Symbol\tDotComPrice\t\tCurrentPrice\tMaxPrice(2002->2023)\n");
        for (var symbol : symbols) {
            List<HistoricalPriceElement> company = DataLoader.readHistoricalPrice(symbol, 500);

            if (company.size() < 0) {
                continue;
            }

            Double priceNow = company.get(0).close;
            Optional<Double> price2025 = findPriceOn(company, LocalDate.of(2021, 1, 1));

            if (price2025.isPresent()) {
                double change = (priceNow / price2025.get() - 1.0) * 100.0;

                full.add(Map.entry(symbol, change));
                if (change < 0) {
                    negativeCount += 1;
                }
                if (change > 0) {
                    positiveCount += 1;
                }
                if (change > 17) {
                    beatCount += 1;
                }
                //System.out.printf("    %-4s\t\t%3.2f\n", symbol, change);
            }

        }

        Collections.sort(full, (a, b) -> b.getValue().compareTo(a.getValue()));

        for (var entry : full) {
            System.out.printf("%s\t\t%.2f\n", entry.getKey(), entry.getValue());
        }

        System.out.println("Negative: " + negativeCount);
        System.out.println("Positive: " + positiveCount);
        System.out.println("Beat: " + beatCount);
    }

    private static Optional<Double> findPriceOn(List<HistoricalPriceElement> financials, LocalDate start) {
        int index1 = Helpers.findIndexWithOrBeforeDate(financials, start);

        if (index1 == -1) {
            return Optional.empty();
        }

        return Optional.of(financials.get(index1).close);
    }

}
