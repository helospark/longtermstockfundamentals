package com.helospark.financialdata.flags;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.service.MarginCalculator;

@Component
public class MarginFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags) {
        List<FinancialsTtm> financials = company.financials;
        if (financials.size() > 0) {
            Optional<Double> netMarginGrowth = MarginCalculator.getNetMarginGrowthRate(financials, 5.0, 0.0);

            if (netMarginGrowth.isPresent()) {
                if (netMarginGrowth.get() < -2.0) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Net margin decreased in the past 5 years (annual %.2f%%)", netMarginGrowth.get())));
                } else if (netMarginGrowth.get() > 2.0) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Net margin increased in the past 5 years (annual %.2f%%)", netMarginGrowth.get())));
                }
            }

            Optional<Double> grossMargin = MarginCalculator.getGrossMargin(financials, 0.0);

            if (grossMargin.isPresent() && grossMargin.get() < 0.0) {
                flags.add(new FlagInformation(FlagType.RED, format("Gross margin is negative (%.2f%%)", grossMargin.get())));
            }
        }
    }

}
