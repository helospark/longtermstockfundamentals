package com.helospark.financialdata.flags;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.service.GrowthCorrelationCalculator;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.TrailingPegCalculator;

@Component
public class EarningQualityFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags) {
        List<FinancialsTtm> financials = company.financials;
        if (financials.size() > 3) {
            var financialsTtm = financials.get(0);

            Optional<Double> trailingPegOpt = calculateAvgTrailingPeg(company, 3);

            if (trailingPegOpt.isPresent()) {
                Double trailingPeg = trailingPegOpt.get();
                if (trailingPeg > 2.0) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Expensive based on trailing PEG (trailing PEG=%.2f)", trailingPeg)));
                } else if (trailingPeg < 1.5 && trailingPeg > 0.7) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Fairly priced based on trailing PEG (trailing PEG=%.2f)", trailingPeg)));
                } else if (trailingPeg < 0.7) {
                    flags.add(new FlagInformation(FlagType.STAR, format("Cheap based on trailing PEG (trailing PEG=%.2f)", trailingPeg)));
                }
            }

            double bookRatio = company.financials.get(0).remoteRatio.priceToBookRatio;
            if (bookRatio > 5.0) {
                flags.add(new FlagInformation(FlagType.YELLOW, format("Expensive based on book ratio (book ratio=%.2f)", bookRatio)));
            } else if (bookRatio > 0.0 && bookRatio < 1.3) {
                flags.add(new FlagInformation(FlagType.GREEN, format("Fairly priced based on book ratio (book ratio=%.2f)", bookRatio)));
            }

            double peRatio = company.financials.get(0).remoteRatio.priceEarningsRatio;
            if (peRatio > 0.0 && peRatio < 11.0) {
                flags.add(new FlagInformation(FlagType.GREEN, format("Low PE ratio (PE=%.2f)", peRatio)));
            } else if (peRatio > 100.0) {
                flags.add(new FlagInformation(FlagType.YELLOW, format("Very high PE ratio (PE=%.2f)", peRatio)));
            }

            double pfcf = financialsTtm.price / ((double) financialsTtm.cashFlowTtm.freeCashFlow / financialsTtm.incomeStatementTtm.weightedAverageShsOut);
            if (pfcf > 0.0 && pfcf < 20.0) {
                flags.add(new FlagInformation(FlagType.GREEN, format("Good price to free cash flow ratio (PFCF=%.2f%%)", pfcf)));
            } else if (pfcf > 100.0) {
                flags.add(new FlagInformation(FlagType.RED, format("Very high price to free cash flow ratio (PFCF=%.2f%%)", pfcf)));
            }

            Optional<Double> roic2 = RoicCalculator.getAverageRoic(financials, 0.0);
            if (roic2.isPresent()) {
                var roic = roic2.get();
                if (roic > 0.20) {
                    flags.add(new FlagInformation(FlagType.STAR, format("Very high ROIC (ROIC=%.2f%%)", roic * 100.0)));
                } else if (roic > 0.10) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Good ROIC (ROIC=%.2f%%)", roic * 100.0)));
                } else if (roic > 0.0 && roic < 0.03) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Low ROIC (ROIC=%.2f%%)", roic * 100.0)));
                } else if (roic < 0.0) {
                    flags.add(new FlagInformation(FlagType.RED, format("Negative ROIC (ROIC=%.2f%%)", roic * 100.0)));
                }
            }

            Optional<Double> correlation = GrowthCorrelationCalculator.calculateEpsFcfCorrelation(financials, 5, 0);

            if (correlation.isPresent() && correlation.get() > 0.9) {
                flags.add(new FlagInformation(FlagType.GREEN, format("High correlation between EPS and FCF (correlation=%.2f)", correlation.get())));
            }
        }

    }

    private Optional<Double> calculateAvgTrailingPeg(CompanyFinancials company, int limit) {
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < limit && i < company.financials.size(); ++i) {
            Optional<Double> trailingPegOpt1 = TrailingPegCalculator.calculateTrailingPeg(company, i);
            if (trailingPegOpt1.isPresent()) {
                sum += trailingPegOpt1.get();
                ++count;
            }
        }
        return count > 0 ? Optional.of(sum / count) : Optional.empty();
    }

}
