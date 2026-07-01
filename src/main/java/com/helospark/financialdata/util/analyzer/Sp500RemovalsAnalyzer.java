package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.ReturnWithDividendCalculator;
import com.helospark.financialdata.util.spconstituents.Sp500ConstituentsProvider;
import com.helospark.financialdata.util.spconstituents.Sp500ConstituentsProvider.Sp500HistoricalConstituents;

public class Sp500RemovalsAnalyzer {

    public static void main(String[] args) {
        List<Map.Entry<LocalDate, Sp500HistoricalConstituents>> timeline = new ArrayList<>(Sp500ConstituentsProvider.getAllDetailedSp500ConstituentsNonCached().entrySet());

        System.out.println("Analyzing S&P 500 Removals and Subsequent Performance...\n");
        System.out.printf("%-12s | %-6s | %-12s | %-12s | %-10s%n",
                "Removal Date", "Ticker", "Start Date", "End Date", "CAGR %");
        System.out.println("------------------------------------------------------------------");

        for (int i = 0; i < timeline.size() - 1; i++) {
            LocalDate currentPeriodDate = timeline.get(i).getKey();
            List<String> currentConstituents = timeline.get(i).getValue().constituents;

            List<String> nextConstituents = timeline.get(i + 1).getValue().constituents;

            Set<String> nextPeriodSet = new HashSet<>(nextConstituents);

            for (String stock : currentConstituents) {
                if (!nextPeriodSet.contains(stock)) {
                    calculateAndPrintRemovalPerformance(stock, currentPeriodDate);
                }
            }
        }
    }

    private static void calculateAndPrintRemovalPerformance(String stock, LocalDate removalDate) {
        try {
            CompanyFinancials company = DataLoader.readFinancials(stock);
            if (company == null || company.financials == null || company.financials.isEmpty()) {
                return;
            }

            FinancialsTtm latestFinancialTtm = company.financials.get(0);

            int removalFinancialIndex = Helpers.findIndexWithOrBeforeDate(company.financials, removalDate);

            if (removalFinancialIndex < 0 || removalFinancialIndex >= company.financials.size()) {
                return;
            }

            FinancialsTtm removalFinancialTtm = company.financials.get(removalFinancialIndex);

            double cagr = ReturnWithDividendCalculator.getCagrBetween(
                    company,
                    removalFinancialTtm.date,
                    latestFinancialTtm.date).orElse(0.0);

            System.out.printf("%-12s | %-6s | %-12s | %-12s | %.2f%%%n",
                    removalDate, stock, removalFinancialTtm.date, latestFinancialTtm.date, cagr);

        } catch (Exception e) {
            // Suppress individual IO read failures (e.g., if a delisted file is missing from local disk cache)
            // so execution can process the rest of the timeline uninterrupted
        }
    }
}