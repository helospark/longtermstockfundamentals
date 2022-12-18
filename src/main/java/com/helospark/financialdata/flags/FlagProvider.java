package com.helospark.financialdata.flags;

import java.util.List;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FlagInformation;

public interface FlagProvider {

    public void addFlags(CompanyFinancials company, List<FlagInformation> flags, double offset);

}
