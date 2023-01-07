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
import com.helospark.financialdata.service.CapeCalculator;
import com.helospark.financialdata.service.GrowthAnalyzer;
import com.helospark.financialdata.service.GrowthCorrelationCalculator;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.RatioCalculator;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.TrailingPegCalculator;

@Component
public class EarningQualityFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags, double offset) {
        List<FinancialsTtm> financials = company.financials;
        int index = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.now().minusMonths((long) (12.0 * offset)));
        if (index != -1 && financials.size() > index + 3) {
            var financialsTtm = financials.get(index);

            Optional<Double> trailingPegOpt = calculateAvgTrailingPeg(company, offset, 3);

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

            Double bookRatio = RatioCalculator.calculatePriceToBookRatio(company.financials.get(index));
            if (bookRatio != null) {
                if (bookRatio > 5.0) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Expensive based on book ratio (book ratio=%.2f)", bookRatio)));
                } else if (bookRatio > 0.0 && bookRatio < 1.3) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Fairly priced based on book ratio (book ratio=%.2f)", bookRatio)));
                }
            }

            Double peRatio = RatioCalculator.calculatePriceToEarningsRatio(company.financials.get(index));
            if (peRatio != null) {
                if (peRatio > 0.0 && peRatio < 11.0) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Low PE ratio (PE=%.2f)", peRatio)));
                } else if (peRatio > 100.0) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Very high PE ratio (PE=%.2f)", peRatio)));
                }
            }

            Double cape = CapeCalculator.calculateCapeRatioQ(company.financials, 10, index);
            if (cape != null) {
                if (cape > 0.0 && cape <= 10.0) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Low CAPE ratio (CAPE=%.2f)", cape)));
                }
            }

            if (financialsTtm.cashFlowTtm.freeCashFlow > 0) {
                double pfcf = financialsTtm.price / ((double) financialsTtm.cashFlowTtm.freeCashFlow / financialsTtm.incomeStatementTtm.weightedAverageShsOut);
                if (pfcf > 0.0 && pfcf < 20.0) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Good price to free cash flow ratio (PFCF=%.2f)", pfcf)));
                } else if (pfcf > 100.0) {
                    flags.add(new FlagInformation(FlagType.RED, format("Very high price to free cash flow ratio (PFCF=%.2f)", pfcf)));
                }
            }

            Optional<Double> roic2 = RoicCalculator.getAverageRoic(financials, offset);
            if (roic2.isPresent()) {
                var roic = roic2.get();
                if (roic > 0.30) {
                    flags.add(new FlagInformation(FlagType.STAR, format("Very high ROIC (ROIC=%.2f%%)", roic * 100.0)));
                } else if (roic > 0.20) {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Good ROIC (ROIC=%.2f%%)", roic * 100.0)));
                } else if (roic > 0.0 && roic < 0.08) {
                    flags.add(new FlagInformation(FlagType.YELLOW, format("Low ROIC (ROIC=%.2f%%)", roic * 100.0)));
                } else if (roic < 0.0) {
                    flags.add(new FlagInformation(FlagType.RED, format("Negative ROIC (ROIC=%.2f%%)", roic * 100.0)));
                }
            }

            Optional<Double> correlation = GrowthCorrelationCalculator.calculateEpsFcfCorrelation(financials, offset + 5, offset);

            if (correlation.isPresent() && correlation.get() > 0.9) {
                flags.add(new FlagInformation(FlagType.GREEN, format("High correlation between EPS and FCF (correlation=%.2f)", correlation.get())));
            }

            boolean stableGrowth = GrowthAnalyzer.isStableGrowth(financials, offset + 7.0, offset);
            if (!stableGrowth) {
                flags.add(new FlagInformation(FlagType.YELLOW, format("Growth is not stable in the past 7 yrs")));
            }
        }

    }

    private Optional<Double> calculateAvgTrailingPeg(CompanyFinancials company, double offsetYears, int limit) {
        double sum = 0.0;
        int count = 0;
        for (double year = offsetYears; year < offsetYears + limit && year < company.financials.size(); ++year) {
            Optional<Double> trailingPegOpt1 = TrailingPegCalculator.calculateTrailingPeg(company, year);
            if (trailingPegOpt1.isPresent()) {
                sum += trailingPegOpt1.get();
                ++count;
            }
        }
        return count > 0 ? Optional.of(sum / count) : Optional.empty();
    }

}
