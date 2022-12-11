package com.helospark.financialdata.flags;

import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;

@Component
public class ProfitabilityFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags) {
        List<FinancialsTtm> financials = company.financials;
        if (financials.size() > 0) {
            if (company.financials.get(0).incomeStatementTtm.netIncome < 0) {
                flags.add(new FlagInformation(FlagType.RED, "Company is currently not profitable"));
            }
            if (company.financials.get(0).cashFlowTtm.freeCashFlow < 0) {
                flags.add(new FlagInformation(FlagType.RED, "Company has no free cash flow"));
            }

            Optional<Integer> numberOfYearsProfitable = calculateNumberOfYearsProfitable(company);
            if (numberOfYearsProfitable.isPresent()) {
                if (numberOfYearsProfitable.get() > 15) {
                    flags.add(new FlagInformation(FlagType.STAR, format("Company has been continously profitable for %d yrs", numberOfYearsProfitable.get())));
                } else if (numberOfYearsProfitable.get() > 5) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Company has been continously profitable for %d yrs", numberOfYearsProfitable.get())));
                } else if (numberOfYearsProfitable.get() < 3) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Company has just been profitable for %d yrs", numberOfYearsProfitable.get())));
                }
            }
        }

    }

    private Optional<Integer> calculateNumberOfYearsProfitable(CompanyFinancials company) {
        for (int i = 20; i > 0; --i) {
            if (isProfitableEveryYearSince(company.financials, i, 0)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

}
