package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.Helpers;

public class BetterThanSAndPScreener {

    public static void main(String[] args) {
        Set<String> symbols = DataLoader.provideSp500Symbols();

        new BetterThanSAndPScreener().analyze(symbols);
    }

    public void analyze(Set<String> symbols) {
        System.out.println("symbol\t(2yrgrowth, PE, peExcRnd, peExclMarketing) growthSince");

        int count = 0;
        int total = 0;

        for (var symbol : symbols) {
            CompanyFinancials company = DataLoader.readFinancials(symbol);
            var financials = company.financials;

            int indexOld = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.of(2015, 1, 1));
            int indexNew = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.of(2019, 1, 1));

            if (indexOld == -1 || indexNew == -1 || financials.isEmpty()) {
                continue;
            }

            FinancialsTtm priceOld = financials.get(indexOld);
            FinancialsTtm priceNew = financials.get(indexNew);

            double growthTillToday = GrowthCalculator.calculateGrowth(priceNew.price, priceOld.price, Helpers.daysBetween(priceOld.date, priceNew.date) / 365.0);

            if (growthTillToday > 8.5) {
                count++;
                System.out.printf("%s\t%.2f%% \n", symbol, growthTillToday);
            } else {
                // System.out.printf("----  %s\t%.2f%% \n", symbol, growthTillToday);

            }
            total++;
        }
        System.out.printf("Better: %d (%.2f%%)\n", count, (double) count / total * 100.0);
    }

}
