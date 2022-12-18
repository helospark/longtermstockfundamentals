package com.helospark.financialdata.flags;

import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthCalculator.getEpsGrowthInInterval;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.service.GrowthStandardDeviationCounter;

@Component
public class CompounderFlagProvider implements FlagProvider {
    static double EPS_SD = 15.0;
    static double REV_SD = 7.0;
    static double FCF_SD = 40.0;

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags, double offset) {
        List<FinancialsTtm> financials = company.financials;
        Optional<Double> tenYearAvgGrowth = getEpsGrowthInInterval(financials, 7 + offset, offset);
        boolean continouslyProfitable = isProfitableEveryYearSince(financials, 7 + offset, offset);
        Optional<Double> epsDeviation = GrowthStandardDeviationCounter.calculateEpsGrowthDeviation(company.financials, offset, 8);
        Optional<Double> revenueDeviation = GrowthStandardDeviationCounter.calculateRevenueGrowthDeviation(company.financials, offset, 8);
        Optional<Double> fcfDeviation = GrowthStandardDeviationCounter.calculateFcfGrowthDeviation(company.financials, offset, 8);

        if (tenYearAvgGrowth.isPresent() && tenYearAvgGrowth.get() > 0.0 && continouslyProfitable && epsDeviation.isPresent() && revenueDeviation.isPresent() && fcfDeviation.isPresent()) {
            Double epsStandardDeviation = epsDeviation.get();
            if (epsStandardDeviation < EPS_SD && revenueDeviation.get() < REV_SD && fcfDeviation.get() < FCF_SD && tenYearAvgGrowth.get() > 10.0) {
                flags.add(new FlagInformation(FlagType.STAR, "Excellent compounder"));
            } else if (epsStandardDeviation < EPS_SD && revenueDeviation.get() < REV_SD && fcfDeviation.get() < 90.0) {
                flags.add(new FlagInformation(FlagType.GREEN, "Good compounder"));
            }
        }

    }

}
