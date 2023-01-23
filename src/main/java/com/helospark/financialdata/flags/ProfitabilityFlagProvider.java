package com.helospark.financialdata.flags;

import static java.lang.String.format;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.ProfitabilityCalculator;

@Component
public class ProfitabilityFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags, double offset) {
        List<FinancialsTtm> financials = company.financials;
        int index = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.now().minusMonths((long) (12.0 * offset)));
        if (index != -1) {
            if (company.financials.get(index).incomeStatementTtm.netIncome < 0) {
                flags.add(new FlagInformation(FlagType.RED, "Company is currently not profitable"));
            }
            if (company.financials.get(index).cashFlowTtm.freeCashFlow < 0) {
                flags.add(new FlagInformation(FlagType.RED, "Company has no free cash flow"));
            }

            Optional<Integer> numberOfYearsProfitable = ProfitabilityCalculator.calculateNumberOfYearsProfitable(company, offset);
            if (numberOfYearsProfitable.isPresent()) {
                int profitableYears = numberOfYearsProfitable.get();
                if (profitableYears > 15) {
                    flags.add(new FlagInformation(FlagType.STAR, format("Company has been continously profitable for %d yrs", profitableYears)));
                } else if (profitableYears > 5) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Company has been continously profitable for %d yrs", profitableYears)));
                } else if (profitableYears < 3 && profitableYears > 0) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Company has just been profitable for %d yrs", profitableYears)));
                }

                int yearsToCheckFcf = profitableYears > 8 ? 8 : profitableYears;
                boolean hasNegativeFcf = ProfitabilityCalculator.hasNegativeFreeCashFlow(company, index, yearsToCheckFcf);
                if (hasNegativeFcf) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Though company has been profitable %d yrs, it had negative FCF", yearsToCheckFcf)));
                }
            }
        }

    }

}
