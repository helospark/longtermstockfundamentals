package com.helospark.financialdata.flags;

import static java.lang.String.format;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.FlagInformation;
import com.helospark.financialdata.domain.FlagType;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.RatioCalculator;
import com.helospark.financialdata.service.RoicCalculator;

@Component
public class DebtFlagFlagProvider implements FlagProvider {

    @Override
    public void addFlags(CompanyFinancials company, List<FlagInformation> flags, double offset) {
        List<FinancialsTtm> financials = company.financials;
        int index = Helpers.findIndexWithOrBeforeDate(financials, LocalDate.now().minusMonths((long) (12.0 * offset)));
        if (index != -1) {
            double latestPrice = (index == 0 ? company.latestPrice : financials.get(index).price);
            FinancialsTtm latestEntry = company.financials.get(index);
            Double quickRatio = RatioCalculator.calculateQuickRatio(latestEntry);
            if (quickRatio != null && quickRatio < 1.0) {
                flags.add(new FlagInformation(FlagType.YELLOW, format("Quick ratio is less than 1.0 (%.2f)", quickRatio)));
            }

            var altmanScore = AltmanZCalculator.calculateAltmanZScore(latestEntry, latestPrice);
            if (altmanScore < 0.4) {
                flags.add(new FlagInformation(FlagType.RED, format("Extremely high risk of bankruptcy (altmanZ=%.2f)", altmanScore)));
            } else if (altmanScore < 1.0) {
                flags.add(new FlagInformation(FlagType.RED, format("AltmanZ score indicates high risk of bankruptcy (altmanZ=%.2f)", altmanScore)));
            } else if (altmanScore < 2.0) {
                flags.add(new FlagInformation(FlagType.YELLOW, format("AltmanZ score indicates moderate risk of bankruptcy (altmanZ=%.2f)", altmanScore)));
            } else if (altmanScore > 5.0) {
                flags.add(new FlagInformation(FlagType.STAR, format("Very high AltmanZ score (altmanZ=%.2f)", altmanScore)));
            } else if (altmanScore > 4.0) {
                flags.add(new FlagInformation(FlagType.GREEN, format("AltmanZ score indicates minimal risk of bankruptcy (altmanZ=%.2f)", altmanScore)));
            }
            double cash = latestEntry.balanceSheet.cashAndCashEquivalents;
            double currentLiabilities = latestEntry.balanceSheet.totalCurrentLiabilities;
            double totalLiabilities = latestEntry.balanceSheet.totalLiabilities;

            if (cash > currentLiabilities) {
                flags.add(new FlagInformation(FlagType.GREEN, "Company has enough cash to pay current liabilities"));
            }
            if (cash > totalLiabilities) {
                flags.add(new FlagInformation(FlagType.STAR, "Company has enough cash to pay all liabilities"));
            }

            double liabilitiesPerFcf = (double) latestEntry.balanceSheet.totalLiabilities / latestEntry.cashFlowTtm.freeCashFlow;
            if (liabilitiesPerFcf > 0.0 && liabilitiesPerFcf < 5.0) {
                flags.add(new FlagInformation(FlagType.STAR, format("Company could pay all it's liabilities less than 5yr of free cashflow (time=%.2f yrs)", liabilitiesPerFcf)));
            } else if (liabilitiesPerFcf > 20.0 && liabilitiesPerFcf > 0) {
                flags.add(new FlagInformation(FlagType.YELLOW, format("Low cashflow coverage, it would take more than 20 years of FCF to pay off it's debt (time=%.2f yrs)", liabilitiesPerFcf)));
            }

            double totalAssets = latestEntry.balanceSheet.totalAssets;

            if (totalLiabilities > totalAssets) {
                flags.add(new FlagInformation(FlagType.RED, format("Total assets less than total liabilities (assets/liabilities=%.2f)", totalAssets / totalLiabilities)));
            }

            double operatingCashFlowCoverage = ((double) latestEntry.cashFlowTtm.operatingCashFlow / latestEntry.balanceSheet.totalDebt) * 100.0;

            if (operatingCashFlowCoverage < 20.0) {
                flags.add(new FlagInformation(FlagType.YELLOW, format("Operating cashflow covers less than 20%% of the debt (coverage=%.2f%%)", operatingCashFlowCoverage)));
            }

            if (latestEntry.incomeStatementTtm.interestExpense > 0) {
                double ebit = RoicCalculator.calculateEbit(latestEntry);
                double interestCoverage = (ebit / latestEntry.incomeStatementTtm.interestExpense);

                if (interestCoverage < 3.0) {
                    flags.add(new FlagInformation(FlagType.RED, format("Poor interest coverage by EBIT (coverage=%.2fx)", interestCoverage)));
                } else {
                    flags.add(new FlagInformation(FlagType.GREEN, format("Good interest coverage by EBIT (coverage=%.2fx)", interestCoverage)));
                }
            }
        }
    }

}
