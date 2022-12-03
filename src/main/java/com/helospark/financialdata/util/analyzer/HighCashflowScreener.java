package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.AltmanZCalculator.calculateAltmanZScore;
import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthCalculator.getFcfGrowthInInterval;

import java.util.Set;

import com.helospark.financialdata.domain.CashFlow;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;

public class HighCashflowScreener {

    public void analyze(Set<String> symbols) {
        System.out.println("symbol\tPO%\tCashFlowGrowthPerShare%");
        for (var symbol : symbols) {
            CompanyFinancials company = readFinancials(symbol);
            var financials = company.financials;

            if (financials.isEmpty()) {
                continue;
            }

            boolean continouslyProfitable = isProfitableEveryYearSince(financials, 9, 0);
            FinancialsTtm firstElement = financials.get(0);
            double altmanZ = calculateAltmanZScore(firstElement, company.latestPrice);

            double tenYearGrowth = getFcfGrowthInInterval(financials, 9, 0).orElse(0.0);
            double eightYearGrowth = getFcfGrowthInInterval(financials, 8, 0).orElse(0.0);
            double fiveYearGrowth = getFcfGrowthInInterval(financials, 5, 0).orElse(0.0);
            double threeYearGrowth = getFcfGrowthInInterval(financials, 3, 0).orElse(0.0);
            double preCovidGrowth = getFcfGrowthInInterval(financials, 9, 3).orElse(0.0);

            if (continouslyProfitable && altmanZ > 2.0 &&
                    tenYearGrowth > 15.0 && preCovidGrowth > 15.0) {
                CashFlow cashFlow = firstElement.cashFlowTtm;

                long fcf = cashFlow.freeCashFlow;
                double fcfPerShare = (double) fcf / firstElement.incomeStatementTtm.weightedAverageShsOut;
                double priceToFcf = fcfPerShare > 0 ? fcfPerShare / company.latestPrice : 0;

                if (priceToFcf > 0.1) {
                    System.out.printf("%s\t%.2f\t(%.2f, %.2f %.2f, %.2f, %.2f)\n", symbol, priceToFcf, tenYearGrowth, eightYearGrowth, fiveYearGrowth, threeYearGrowth, preCovidGrowth);
                }
            }

        }
    }

}
