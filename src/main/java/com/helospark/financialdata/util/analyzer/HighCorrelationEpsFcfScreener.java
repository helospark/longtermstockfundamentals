package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.AltmanZCalculator.calculateAltmanZScore;
import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthCalculator.getFcfGrowthInInterval;

import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.service.GrowthCorrelationCalculator;

public class HighCorrelationEpsFcfScreener {

    public void analyze(Set<String> symbols) {
        System.out.println("symbol\tGrowth%\t\tCorr\tCompany");
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
            double preCovidGrowth = getFcfGrowthInInterval(financials, 9, 3).orElse(0.0);
            Optional<Double> corr = GrowthCorrelationCalculator.calculateEpsFcfCorrelation(financials, 8.0, 0.0);

            if (continouslyProfitable && altmanZ > 2.0 &&
                    tenYearGrowth > 12.0 && preCovidGrowth > 12.0 && corr.isPresent() && corr.get() > 0.95) {
                System.out.printf("%s\t(%.2f %.2f)\t%.2f\t%s | %s\n", symbol, tenYearGrowth, preCovidGrowth, corr.get(), company.profile.companyName,
                        company.profile.industry);
            }

        }
    }

}
