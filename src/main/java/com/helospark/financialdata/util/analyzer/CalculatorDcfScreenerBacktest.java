package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;

import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.DcfCalculator;

public class CalculatorDcfScreenerBacktest {

    public void analyze(Set<String> symbols) {
        System.out.printf("Symbol\tGrowth\tPE\tEPS_SD\tREV_SD\tFCF_SD\tGrowth%% (from->to)\n");
        for (int index = 0; index <= 25 * 4; ++index) {
            for (var symbol : symbols) {
                CompanyFinancials company = readFinancials(symbol);

                Optional<Double> value = DcfCalculator.doDcfAnalysisRevenueWithDefaultParameters(company, 0.0);

                if (value.isPresent()) {
                    System.out.println(symbol + "\t" + value.get());
                }
            }
        }
    }

}
