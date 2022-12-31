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
import com.helospark.financialdata.service.DividendCalculator;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.RatioCalculator;

@Component
public class DividendFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags, double offset) {
        List<FinancialsTtm> financials = company.financials;
        int index = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.now().minusMonths((long) (12.0 * offset)));
        if (index != -1) {

            double dividend = DividendCalculator.getDividendYield(company, index);
            if (dividend > 0) {
                Optional<Double> payoutRatio = calculateAvgPayoutRatio(company, index, 3);
                if (payoutRatio.isPresent() && payoutRatio.get() > 0.9) {
                    flags.add(new FlagInformation(FlagType.RED, format("Avg payout ratio is above 90%% (ratio=%.2f%%)", payoutRatio.get() * 100.0)));
                }
                Optional<Double> fcfPayoutRatio = calculateAvgFcfPayoutRatio(company, index, 3);
                if (fcfPayoutRatio.isPresent() && fcfPayoutRatio.get() > 0.9) {
                    flags.add(new FlagInformation(FlagType.RED, format("Avg FCF payout ratio is above 90%% (ratio=%.2f%%)", fcfPayoutRatio.get() * 100.0)));
                }

                Optional<Double> dividendGrowth = GrowthCalculator.getDividendGrowthInInterval(financials, offset + 5, offset);
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

    private Optional<Double> calculateAvgFcfPayoutRatio(CompanyFinancials company, int offset, int limit) {
        double sum = 0.0;
        int count = 0;
        for (int i = offset; i < offset + limit && i < company.financials.size(); ++i) {
            var financialsTtm = company.financials.get(i);
            Double payoutRatio = (double) -financialsTtm.cashFlowTtm.dividendsPaid / financialsTtm.cashFlowTtm.freeCashFlow;
            if (payoutRatio != null) {
                sum += payoutRatio;
                ++count;
            }
        }
        return count > 0 ? Optional.of(sum / count) : Optional.empty();
    }

    private Optional<Double> calculateAvgPayoutRatio(CompanyFinancials company, int offset, int limit) {
        double sum = 0.0;
        int count = 0;
        for (int i = offset; i < offset + limit && i < company.financials.size(); ++i) {
            Double payoutRatio = RatioCalculator.calculatePayoutRatio(company.financials.get(i));
            if (payoutRatio != null) {
                sum += payoutRatio;
                ++count;
            }
        }
        return count > 0 ? Optional.of(sum / count) : Optional.empty();
    }

}
