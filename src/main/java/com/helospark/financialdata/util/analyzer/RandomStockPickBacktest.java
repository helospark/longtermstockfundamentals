package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;

import java.util.Random;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;

public class RandomStockPickBacktest implements StockScreeners {
    Random random = new Random();

    @Override
    public void analyze(Set<String> symbols) {
        for (int yearsAgo = 5; yearsAgo <= 21; ++yearsAgo) {
            double growthSum = 0.0;
            int count = 0;
            System.out.println("symbol\tincrease (from -> to)");
            for (var symbol : symbols) {
                CompanyFinancials company = readFinancials(symbol);
                var financials = company.financials;

                int latestElement = yearsAgo * 4;
                if (financials.isEmpty() || financials.size() <= latestElement) {
                    continue;
                }
                if (random.nextInt(100) == 1) {
                    double latestPriceThen = financials.get(latestElement).price;
                    double sellPrice = company.latestPrice;
                    double growthRatio = sellPrice / latestPriceThen;
                    growthSum += (growthRatio * 1000.0);
                    ++count;

                    double growthTillSell = (growthRatio - 1.0) * 100.0;

                    System.out.printf("%s\t%.1f%%\t (%.1f -> %.1f)\n", symbol,
                            growthTillSell, latestPriceThen, sellPrice);
                }
            }
            double increase = (growthSum / (count * 1000) - 1.0);
            double annual = Math.pow(growthSum / (count * 1000), (1.0 / yearsAgo)) - 1.0;
            System.out.println("Have " + (growthSum) + " from " + (count * 1000) + " (" + (increase * 100.0) + "%, " + (annual * 100.0) + "%) invested " + yearsAgo);
            System.out.println();
        }
    }

}
