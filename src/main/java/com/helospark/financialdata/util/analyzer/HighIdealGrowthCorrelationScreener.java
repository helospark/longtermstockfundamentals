package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;

import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.IdealGrowthCorrelationCalculator;

public class HighIdealGrowthCorrelationScreener {
    public static final String RESULT_FILE_NAME = CommonConfig.BASE_FOLDER + "/info/screeners/high_ideal_growth.csv";

    public void analyze(Set<String> symbols) {
        StringBuilder csv = new StringBuilder();
        csv.append("Symbol;ROIC;Trailing PEG;AltmanZ;Name\n");
        System.out.printf("Symbol\tROIC\tPEG\tCompany\n");
        for (var symbol : symbols) {
            if (symbol.equals("FB")) {
                continue; // replaced by META
            }
            //            symbol = "POOL";
            CompanyFinancials company = readFinancials(symbol);
            var financials = company.financials;

            if (financials.isEmpty()) {
                continue;
            }

            int years = 12;
            Optional<Double> idealGrowth = IdealGrowthCorrelationCalculator.calculateRevenueCorrelation(company.financials, years, 0.0);
            Optional<Double> epsIdealGrowth = IdealGrowthCorrelationCalculator.calculateEpsCorrelation(company.financials, years, 0.0);
            Optional<Double> fcfIdealGrowth = IdealGrowthCorrelationCalculator.calculateFcfCorrelation(company.financials, years, 0.0);
            Optional<Double> growth = GrowthCalculator.getRevenueGrowthInInterval(company.financials, years, 0.0);

            if (symbol.equals("AZO") || symbol.equals("POOL")) {
                System.out.println(symbol + " " + idealGrowth + " " + epsIdealGrowth + " " + growth);
            }

            if (idealGrowth.isPresent() && idealGrowth.get() > 0.985 && growth.isPresent() && growth.get() > 10 && epsIdealGrowth.isPresent() && epsIdealGrowth.get() > 0.95) {
                System.out.printf("%s\t%.4f\t%.4f\t%.4f\t%s | %s\n", symbol, idealGrowth.get(), epsIdealGrowth.get(), fcfIdealGrowth.orElse(Double.NaN), company.profile.companyName,
                        company.profile.industry);
            }

        }

    }

}
