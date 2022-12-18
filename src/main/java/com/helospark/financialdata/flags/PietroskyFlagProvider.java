package com.helospark.financialdata.flags;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.service.PietroskyScoreCalculator;

@Component
public class PietroskyFlagProvider implements FlagProvider {
    static double EPS_SD = 15.0;
    static double REV_SD = 7.0;
    static double FCF_SD = 40.0;

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags, double offset) {
        List<FinancialsTtm> financials = company.financials;
        if (financials.size() > 0) {
            Optional<Integer> pietrosky = PietroskyScoreCalculator.calculatePietroskyScore(company, offset);
            if (pietrosky.isPresent()) {
                if (pietrosky.get() >= 8) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Good pietrosky score (%d)", pietrosky.get())));
                } else if (pietrosky.get() <= 2) {
                    flags.add(new FlagInformation(FlagType.RED, format("Poor pietrosky score (%d)", pietrosky.get())));
                }
            }
        }

    }

}
