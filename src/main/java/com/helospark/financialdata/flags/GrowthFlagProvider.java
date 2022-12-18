package com.helospark.financialdata.flags;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.service.GrowthCalculator;

@Component
public class GrowthFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags, double offset) {
        List<FinancialsTtm> financials = company.financials;
        if (financials.size() > 0) {
            Optional<Double> revenueGrowth = GrowthCalculator.getRevenueGrowthInInterval(financials, offset + 5.0, offset, true);

            if (revenueGrowth.isPresent()) {
                if (revenueGrowth.get() > 12.0) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("In 5yrs revenue has increased more than 12%% annually (annual %.2f%%)", revenueGrowth.get())));
                } else if (revenueGrowth.get() < 0.0) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("In 5yrs revenue has decreased (annual %.2f%%)", revenueGrowth.get())));
                }
            }

            Optional<Double> epsGrowth = GrowthCalculator.getEpsGrowthInInterval(financials, offset + 5.0, offset, true);
            if (epsGrowth.isPresent()) {
                if (epsGrowth.get() > 25.0) {
                    flags.add(new FlagInformation(FlagType.STAR, format("In 5yrs EPS has increased more than 25%% annually (%.2f%%)", epsGrowth.get())));
                } else if (epsGrowth.get() > 12.0) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("In 5yrs EPS has increased more than 12%% annually (%.2f%%)", epsGrowth.get())));
                } else if (epsGrowth.get() < 0.0) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("In 5yrs EPS has decreased (annual %.2f%%)", epsGrowth.get())));
                }
            }

            Optional<Double> fcfGrowth = GrowthCalculator.getFcfGrowthInInterval(financials, offset + 5.0, offset, true);
            if (fcfGrowth.isPresent()) {
                if (fcfGrowth.get() > 25.0) {
                    flags.add(new FlagInformation(FlagType.STAR, format("In 5yrs FCF per share has increased more than 25%% annually (%.2f%%)", fcfGrowth.get())));
                } else if (fcfGrowth.get() > 12.0) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("In 5yrs FCF per share has increased more than 12%% annually (%.2f%%)", fcfGrowth.get())));
                } else if (fcfGrowth.get() < 0.0) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("In 5yrs FCF per share has decreased (annual %.2f%%)", fcfGrowth.get())));
                }
            }

        }

    }

}
