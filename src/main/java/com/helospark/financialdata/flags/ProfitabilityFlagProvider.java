package com.helospark.financialdata.flags;

import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
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

            Optional<Integer> numberOfYearsProfitable = calculateNumberOfYearsProfitable(company, offset);
            if (numberOfYearsProfitable.isPresent()) {
                int profitableYears = numberOfYearsProfitable.get();
                if (profitableYears > 15) {
                    flags.add(new FlagInformation(FlagType.STAR, format("Company has been continously profitable for %d yrs", profitableYears)));
                } else if (profitableYears > 5) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Company has been continously profitable for %d yrs", profitableYears)));
                } else if (profitableYears < 3) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Company has just been profitable for %d yrs", profitableYears)));
                }

                int yearsToCheckFcf = profitableYears > 8 ? 8 : profitableYears;
                boolean hasNegativeFcf = hasNegativeFreeCashFlow(company, index, yearsToCheckFcf);
                if (hasNegativeFcf) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Though company has been profitable %d yrs, it had negative FCF", yearsToCheckFcf)));
                }
            }
        }

    }

    private boolean hasNegativeFreeCashFlow(CompanyFinancials company, int index, int years) {
        for (int i = index; i < company.financials.size() && i < years * 4; ++i) {
            if (company.financials.get(i).cashFlowTtm.freeCashFlow < 0) {
                return true;
            }
        }
        return false;
    }

    private Optional<Integer> calculateNumberOfYearsProfitable(CompanyFinancials company, double offset) {
        for (int i = 20; i > 0; --i) {
            if (isProfitableEveryYearSince(company.financials, offset + i, offset)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

}
