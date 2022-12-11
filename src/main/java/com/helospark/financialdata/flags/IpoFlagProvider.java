package com.helospark.financialdata.flags;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;

@Component
public class IpoFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags) {
        List<FinancialsTtm> financials = company.financials;
        if (financials.size() > 0) {
            var date = financials.get(financials.size() - 1).getDate();
            long yearsDiff = ChronoUnit.YEARS.between(date, LocalDate.now());
            if (yearsDiff < 5) {
                flags.add(new FlagInformation(FlagType.YELLOW, "Company went public less than 5 years ago, many check are not available"));
            }
        }
    }

}
