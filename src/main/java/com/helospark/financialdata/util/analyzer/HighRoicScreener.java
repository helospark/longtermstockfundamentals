package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;

import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.TrailingPegCalculator;

public class HighRoicScreener {

    public void analyze(Set<String> symbols) {
        System.out.printf("Symbol\tROIC\tPEG\tCompany\n");
        for (var symbol : symbols) {
            CompanyFinancials company = readFinancials(symbol);
            var financials = company.financials;

            if (financials.isEmpty()) {
                continue;
            }

            boolean continouslyProfitable = isProfitableEveryYearSince(financials, 8, 0);
            double altmanZ = AltmanZCalculator.calculateAltmanZScore(financials.get(0), company.latestPrice);

            if (altmanZ > 2.0 && continouslyProfitable) {
                Optional<Double> roic = RoicCalculator.getAverageRoic(company.financials, 0.0);
                Optional<Double> trailingPeg = TrailingPegCalculator.calculateTrailingPeg(company, 0);

                if (roic.isPresent() && roic.get() > 0.12) {
                    System.out.printf("%s\t%.2f\t%.2f\t%s | %s\n", symbol, roic.get(), trailingPeg.orElse(Double.NaN), company.profile.companyName, company.profile.industry);
                }
            }

        }
    }

}
