package com.helospark.financialdata.util.analyzer;

import java.util.List;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.FlagsProviderService;

public class FlagsScreener implements StockScreeners {

    @Override
    public void analyze(Set<String> symbols) {
        System.out.println("symbol\tStar\tGreen\tYellow\tRed%\tCompany");

        for (var symbol : symbols) {
            CompanyFinancials company = DataLoader.readFinancials(symbol);
            var financials = company.financials;

            if (financials.isEmpty()) {
                continue;
            }

            List<FlagInformation> flags = FlagsProviderService.giveFlags(company, 0);

            int numRed = 0;
            int numYellow = 0;
            int numGreen = 0;
            int numStar = 0;
            for (var element : flags) {
                if (element.type == FlagType.RED) {
                    ++numRed;
                } else if (element.type == FlagType.YELLOW) {
                    ++numYellow;
                } else if (element.type == FlagType.GREEN) {
                    ++numGreen;
                } else if (element.type == FlagType.STAR) {
                    ++numStar;
                }
            }

            if (numYellow == 0 && numRed == 0 && numStar >= 2 && numGreen >= 4) {
                System.out.println(symbol + "\t" + numStar + "\t" + numGreen + "\t" + numYellow + "\t" + numRed + "\t" + company.profile.companyName + " | " + company.profile.sector);
            }

        }
    }

}
