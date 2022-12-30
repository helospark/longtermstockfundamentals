package com.helospark.financialdata.flags;

import static java.lang.String.format;

import java.util.List;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;

@Component
public class DataQualityFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags, double offset) {
        if (company.dataQualityIssue > 7) {
            flags.add(new FlagInformation(FlagType.RED, format("There is data quality issue with this company (cash, balance and income statements have gaps).")));
        }
    }

}
