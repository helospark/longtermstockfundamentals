package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.Helpers;

public class CalculateDotComRecovery {

    public static void main(String[] args) {
        Set<String> symbols = new TreeSet<>(DataLoader.provideSymbolsFromNasdaqNyse());
        System.out.printf("    Symbol\tDotComPrice\t\tCurrentPrice\tMaxPrice(2002->2023)\n");
        for (var symbol : symbols) {
            List<HistoricalPriceElement> company = DataLoader.readHistoricalPrice(symbol, 500);

            Optional<Double> price2000 = findMaxPriceBetween(company, LocalDate.of(2000, 1, 1), LocalDate.of(2002, 1, 1));
            Optional<Double> price2023 = findMaxPriceBetween(company, LocalDate.of(2003, 1, 1), LocalDate.of(2023, 1, 1));

            if (price2000.isPresent() && price2023.isPresent()) {
                double currentPrice = company.get(0).close;
                if (currentPrice > price2000.get() * 0.8 && price2023.get() < price2000.get()) {
                    System.out.printf("    %-4s\t\t%3.2f\t\t%3.2f\t\t%3.2f\n", symbol, price2000.get(), currentPrice, price2023.get());
                }
            }

        }
    }

    private static Optional<Double> findMaxPriceBetween(List<HistoricalPriceElement> financials, LocalDate start, LocalDate end) {
        int index1 = Helpers.findIndexWithOrBeforeDate(financials, start);
        int index2 = Helpers.findIndexWithOrBeforeDate(financials, end);

        if (index1 == -1 || index2 == -1) {
            return Optional.empty();
        }

        int startIndex = Math.min(index1, index2);
        int endIndex = Math.max(index1, index2);

        double max = financials.get(startIndex).close;
        for (int i = startIndex; i < endIndex; ++i) {
            double current = financials.get(i).close;
            if (current > max) {
                max = current;
            }
        }

        return Optional.of(max);
    }

}
