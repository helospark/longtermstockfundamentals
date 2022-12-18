package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.AltmanZCalculator.calculateAltmanZScore;
import static com.helospark.financialdata.service.DcfCalculator.doStockDcfAnalysis;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthAnalyzer.isStableGrowth;
import static com.helospark.financialdata.service.GrowthCalculator.getEpsGrowthInInterval;
import static com.helospark.financialdata.service.Helpers.min;

import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.DataLoader;

public class DcfScreener implements StockScreeners {
    static double UPSIDE_CUTOFF = 95;
    static double PROFITABLE_YEAR = 5;

    @Override
    public void analyze(Set<String> symbols) {
        System.out.println("symbol\t(Growth1, Growth2, Growth3, Growth4)\tDCF\tPE\tUpside%\tfcfUpside%");
        for (var symbol : symbols) {
            CompanyFinancials company = DataLoader.readFinancials(symbol);
            var financials = company.financials;

            if (financials.isEmpty()) {
                continue;
            }

            Optional<Double> tenYearAvgGrowth = getEpsGrowthInInterval(financials, 10, 0);
            Optional<Double> fiveYearAvgGrowth = getEpsGrowthInInterval(financials, 5, 0);
            Optional<Double> threeYearAvgGrowth = getEpsGrowthInInterval(financials, 3, 0);
            Optional<Double> preCovidYearAvgGrowth = getEpsGrowthInInterval(financials, 10, 3);

            boolean continouslyProfitable = isProfitableEveryYearSince(financials, 9, 0);
            boolean stableGrowth = isStableGrowth(financials, 8, 0);
            double altmanZ = calculateAltmanZScore(financials.get(0), company.latestPrice);

            if (tenYearAvgGrowth.isPresent() && preCovidYearAvgGrowth.isPresent() && continouslyProfitable &&
                    stableGrowth &&
                    altmanZ > 2.2 &&
                    financials.size() > 14 &&
                    financials.get(0).incomeStatementTtm.eps > 0.0 &&
                    tenYearAvgGrowth.get() > 0.0) {
                double growth = tenYearAvgGrowth.get();
                double preCovidGrowth = preCovidYearAvgGrowth.get();
                double fiveYearGrowth = fiveYearAvgGrowth.get();

                double dcf = doStockDcfAnalysis(financials.get(0).incomeStatementTtm.eps, min(growth, preCovidGrowth));

                long fcf = financials.get(0).cashFlowTtm.freeCashFlow;
                double fcfPerShare = (double) fcf / financials.get(0).incomeStatementTtm.weightedAverageShsOut;
                double dcfFcf = doStockDcfAnalysis(fcfPerShare, min(growth, preCovidGrowth));

                double currentPe = company.latestPrice / financials.get(0).incomeStatementTtm.eps;

                double upside = (dcf / company.latestPrice - 1.0) * 100;
                double fcfUpside = (dcfFcf / company.latestPrice - 1.0) * 100;

                if (upside > UPSIDE_CUTOFF && fcfUpside > UPSIDE_CUTOFF) {
                    double twoYearGrowth = threeYearAvgGrowth.get();
                    System.out.printf("%s\t(%.2f, %.2f, %.2f, %.2f)\t\t%.2f\t%.2f\t%.2f%%\t\t%.2f%%\n", symbol, growth, fiveYearGrowth, twoYearGrowth, preCovidGrowth, dcf, currentPe, upside,
                            fcfUpside);
                }
            }
        }
    }

}
