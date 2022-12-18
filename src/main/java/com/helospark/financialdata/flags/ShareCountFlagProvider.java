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
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.Helpers;

@Component
public class ShareCountFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags, double offset) {
        List<FinancialsTtm> financials = company.financials;
        int index = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.now().minusMonths((long) (12.0 * offset)));
        if (index != -1) {
            FinancialsTtm financialsTtm = company.financials.get(index);
            Optional<Double> shareCountGrowth = GrowthCalculator.getShareCountGrowthInInterval(financials, offset + 5, offset);
            if (shareCountGrowth.isPresent()) {
                double shareCount = shareCountGrowth.get();
                if (shareCount > 10.0) {
                    flags.add(new FlagInformation(FlagType.RED, format("Share count increased more than 10%% annually in the past 5 years (annual increase=%.2f%%)", shareCount)));
                } else if (shareCount > 0.0) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Share count increased in the past 5 years (annual increase=%.2f%%)", shareCount)));
                } else if (shareCount < 0.0) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Share count decreased in the past 5 years (annual decrease=%.2f%%)", -shareCount)));
                }
            }

            double shareBasedCompensationPerMkt = financialsTtm.cashFlowTtm.stockBasedCompensation / (financialsTtm.price * financialsTtm.incomeStatementTtm.weightedAverageShsOut) * 100.0;
            if (shareBasedCompensationPerMkt > 5.0) {
                flags.add(new FlagInformation(FlagType.RED, format("More than 5%% share based compensation per market cap (compensation=%.2f%%)", shareBasedCompensationPerMkt)));
            } else if (shareBasedCompensationPerMkt > 1.0) {
                flags.add(new FlagInformation(FlagType.YELLOW, format("More than 1%% share based compensation per market cap (compensation=%.2f%%)", shareBasedCompensationPerMkt)));
            }

            if (financialsTtm.incomeStatementTtm.revenue > 0) {
                double shareBasedCompensationPerRev = financialsTtm.cashFlowTtm.stockBasedCompensation / financialsTtm.incomeStatementTtm.revenue * 100.0;
                if (shareBasedCompensationPerRev > 10.0) {
                    flags.add(new FlagInformation(FlagType.RED, format("More than 10%% share based compensation per revenue (compensation=%.2f%%)", shareBasedCompensationPerRev)));
                } else if (shareBasedCompensationPerRev > 1.0) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("More than 1%% share based compensation per revenue (compensation=%.2f%%)", shareBasedCompensationPerRev)));
                }
            }
        }

    }

}
