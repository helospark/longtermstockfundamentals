package com.helospark.financialdata.service;

import java.util.ArrayList;
import java.util.List;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.flags.CompounderFlagProvider;
import com.helospark.financialdata.flags.DebtFlagFlagProvider;
import com.helospark.financialdata.flags.DividendFlagProvider;
import com.helospark.financialdata.flags.EarningQualityFlagProvider;
import com.helospark.financialdata.flags.FlagProvider;
import com.helospark.financialdata.flags.GrowthFlagProvider;
import com.helospark.financialdata.flags.IpoFlagProvider;
import com.helospark.financialdata.flags.MarginFlagProvider;
import com.helospark.financialdata.flags.PietroskyFlagProvider;
import com.helospark.financialdata.flags.ProfitabilityFlagProvider;
import com.helospark.financialdata.flags.ShareCountFlagProvider;

public class FlagsProviderService {
    static List<FlagProvider> flagProviders = new ArrayList<>();

    static {
        flagProviders.add(new CompounderFlagProvider());
        flagProviders.add(new DebtFlagFlagProvider());
        flagProviders.add(new DividendFlagProvider());
        flagProviders.add(new EarningQualityFlagProvider());
        flagProviders.add(new GrowthFlagProvider());
        flagProviders.add(new IpoFlagProvider());
        flagProviders.add(new MarginFlagProvider());
        flagProviders.add(new PietroskyFlagProvider());
        flagProviders.add(new ProfitabilityFlagProvider());
        flagProviders.add(new ShareCountFlagProvider());
    }

    public static List<FlagInformation> giveFlags(CompanyFinancials company, double offsetYears) {
        List<FlagInformation> flags = new ArrayList<>();

        for (var element : flagProviders) {
            element.addFlags(company, flags, offsetYears);
        }

        return flags;
    }

}
