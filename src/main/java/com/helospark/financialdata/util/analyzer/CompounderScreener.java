package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthCalculator.getEpsGrowthInInterval;
import static java.lang.Double.NaN;

import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.GrowthStandardDeviationCounter;
import com.helospark.financialdata.service.TrailingPegCalculator;

public class CompounderScreener {
    static double EPS_SD = 15.0;
    static double REV_SD = 7.0;
    static double FCF_SD = 40.0;
    static double GROWTH = 12.0;
    static double PEG_CUTOFF = 2.0;

    public void analyze(Set<String> symbols) {
        System.out.printf("Symbol\tGrowth\tPE\tEPS_SD\tREV_SD\tFCF_SD\tPEG\tCompany\n");
        for (var symbol : symbols) {
            CompanyFinancials company = readFinancials(symbol);
            var financials = company.financials;

            if (financials.isEmpty()) {
                continue;
            }

            Optional<Double> tenYearAvgGrowth = getEpsGrowthInInterval(financials, 8, 0);
            boolean continouslyProfitable = isProfitableEveryYearSince(financials, 8, 0);
            Optional<Double> epsDeviation = GrowthStandardDeviationCounter.calculateEpsGrowthDeviation(company.financials, 8, 0.0);
            Optional<Double> revenueDeviation = GrowthStandardDeviationCounter.calculateRevenueGrowthDeviation(company.financials, 8, 0.0);
            Optional<Double> fcfDeviation = GrowthStandardDeviationCounter.calculateFcfGrowthDeviation(company.financials, 8, 0.0);
            double altmanZ = AltmanZCalculator.calculateAltmanZScore(financials.get(0), company.latestPrice);

            Optional<Double> trailingPeg = TrailingPegCalculator.calculateTrailingPeg(company, 0);
            Optional<Double> trailingPeg2 = TrailingPegCalculator.calculateTrailingPeg(company, 0 + 1);
            Optional<Double> trailingPeg3 = TrailingPegCalculator.calculateTrailingPeg(company, 0 + 2);

            if (tenYearAvgGrowth.isPresent() && continouslyProfitable && epsDeviation.isPresent() && revenueDeviation.isPresent() && fcfDeviation.isPresent()) {
                //                Double peRatio = financials.get(0).remoteRatio.priceEarningsRatio;
                double currentPe = company.latestPrice / financials.get(0).incomeStatementTtm.eps;
                double growth = tenYearAvgGrowth.get();
                Double epsStandardDeviation = epsDeviation.get();
                if (growth >= GROWTH && epsStandardDeviation < EPS_SD && revenueDeviation.get() < REV_SD && fcfDeviation.get() < FCF_SD && altmanZ > 2.2 &&
                        trailingPeg.isPresent() && trailingPeg.get() < PEG_CUTOFF && trailingPeg2.orElse(0.0) < PEG_CUTOFF && trailingPeg3.orElse(0.0) < PEG_CUTOFF) {
                    System.out.printf("%s\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%s | %s\n", symbol, growth, currentPe, epsStandardDeviation, revenueDeviation.orElse(NaN), fcfDeviation.orElse(NaN),
                            trailingPeg.get(), company.profile.companyName, company.profile.industry);
                }
            }

        }
    }

}
