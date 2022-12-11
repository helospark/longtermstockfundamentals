package com.helospark.financialdata.flags;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.service.DividendCalculator;
import com.helospark.financialdata.service.GrowthCalculator;

@Component
public class DividendFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags) {
        List<FinancialsTtm> financials = company.financials;
        if (financials.size() > 0) {

            double dividend = DividendCalculator.getDividendYield(company, 0);
            if (dividend > 0) {
                Optional<Double> payoutRatio = calculateAvgPayoutRatio(company, 3);
                if (payoutRatio.isPresent() && payoutRatio.get() > 0.9) {
                    flags.add(new FlagInformation(FlagType.RED, format("Avg payout ratio is above 90%% (ratio=%.2f%%)", payoutRatio.get() * 100.0)));
                }
                Optional<Double> fcfPayoutRatio = calculateAvgFcfPayoutRatio(company, 3);
                if (fcfPayoutRatio.isPresent() && fcfPayoutRatio.get() > 0.9) {
                    flags.add(new FlagInformation(FlagType.RED, format("Avg FCF payout ratio is above 90%% (ratio=%.2f%%)", fcfPayoutRatio.get() * 100.0)));
                }

                Optional<Double> dividendGrowth = GrowthCalculator.getDividendGrowthInInterval(financials, 5, 0.0);
                if (dividendGrowth.isPresent()) {
                    if (dividendGrowth.get() < 0.0) {
                        flags.add(new FlagInformation(FlagType.YELLOW, format("Dividend decreased in the past 5 years (annually %.2f%%)", dividendGrowth.get().doubleValue())));
                    } else if (dividendGrowth.get() > 10.0) {
                        flags.add(
                                new FlagInformation(FlagType.GREEN, format("Dividend increased more than 10%% annually in the past 5 years (annually %.2f%%)", (dividendGrowth.get().doubleValue()))));
                    }
                }
            }
        }

    }

    private Optional<Double> calculateAvgFcfPayoutRatio(CompanyFinancials company, int limit) {
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < limit && i < company.financials.size(); ++i) {
            var financialsTtm = company.financials.get(i);
            Double payoutRatio = (double) -financialsTtm.cashFlowTtm.dividendsPaid / financialsTtm.cashFlowTtm.freeCashFlow;
            if (payoutRatio != null) {
                sum += payoutRatio;
                ++count;
            }
        }
        return count > 0 ? Optional.of(sum / count) : Optional.empty();
    }

    private Optional<Double> calculateAvgPayoutRatio(CompanyFinancials company, int limit) {
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < limit && i < company.financials.size(); ++i) {
            Double payoutRatio = company.financials.get(i).remoteRatio.dividendPayoutRatio;
            if (payoutRatio != null) {
                sum += payoutRatio;
                ++count;
            }
        }
        return count > 0 ? Optional.of(sum / count) : Optional.empty();
    }

}
