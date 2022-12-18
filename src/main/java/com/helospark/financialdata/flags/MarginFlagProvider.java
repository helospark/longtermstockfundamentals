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
import com.helospark.financialdata.service.MarginCalculator;

@Component
public class MarginFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags, double offset) {
        List<FinancialsTtm> financials = company.financials;
        int index = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.now().minusMonths((long) (12.0 * offset)));
        if (index != -1) {
            Optional<Double> netMarginGrowth = MarginCalculator.getNetMarginGrowthRate(financials, offset + 5.0, offset);

            if (netMarginGrowth.isPresent()) {
                if (netMarginGrowth.get() < -2.0) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Net margin decreased in the past 5 years (annual %.2f%%)", netMarginGrowth.get())));
                } else if (netMarginGrowth.get() > 2.0) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Net margin increased in the past 5 years (annual %.2f%%)", netMarginGrowth.get())));
                }
            }

            Optional<Double> grossMargin = MarginCalculator.getGrossMargin(financials, offset);

            if (grossMargin.isPresent() && grossMargin.get() < 0.0) {
                flags.add(new FlagInformation(FlagType.RED, format("Gross margin is negative (%.2f%%)", grossMargin.get())));
            }
        }
    }

}
