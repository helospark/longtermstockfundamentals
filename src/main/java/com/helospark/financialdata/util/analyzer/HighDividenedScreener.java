package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.AltmanZCalculator.calculateAltmanZScore;
import static com.helospark.financialdata.service.DividendCalculator.getDividendsInfo;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;

import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.MeanAvg;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.RatioCalculator;

public class HighDividenedScreener {

    public void analyze(Set<String> symbols) {
        System.out.println("symbol\tPO%\tMean%\tavg%");
        for (var symbol : symbols) {
            CompanyFinancials company = DataLoader.readFinancials(symbol);
            var financials = company.financials;

            if (financials.isEmpty()) {
                continue;
            }

            boolean continouslyProfitable = isProfitableEveryYearSince(financials, 9, 0);
            FinancialsTtm firstElement = financials.get(0);
            double altmanZ = calculateAltmanZScore(firstElement, company.latestPrice);

            if (continouslyProfitable && altmanZ > 1.8) {
                Double dividendPayoutRatio = RatioCalculator.calculateCurrentRatio(firstElement).orElse(null);
                MeanAvg dividendYield = getDividendsInfo(company, 10);

                if (dividendPayoutRatio != null && dividendPayoutRatio < 0.5 && dividendPayoutRatio > 0.0 &&
                        dividendYield.mean > 0.04) {
                    System.out.printf("%s\t%.2f\t%.2f\t%.2f\n", symbol, dividendPayoutRatio * 100.0, dividendYield.avg * 100.0, dividendYield.mean * 100.0);
                }
            }

        }

    }

}
