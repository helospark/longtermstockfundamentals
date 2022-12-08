package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.FinancialsTtm;

public class DcfCalculator {

    public static double doStockDcfAnalysis(double eps, double pastGrowth) {
        double startGrowth = pastGrowth * 0.8;
        double endGrowth = pastGrowth * 0.4;
        return doStockDcfAnalysisWithGrowth(eps, startGrowth, endGrowth);
    }

    public static double doStockDcfAnalysisWithGrowth(double eps, double startGrowth, double endGrowth) {
        double dcf = 0.0;
        int years = 10;
        double discount = 0.15;
        double endMultiple = endGrowth;
        if (endMultiple > 18) {
            endMultiple = 18;
        }
        if (endMultiple < 8) {
            endMultiple = 8;
        }

        for (int i = 0; i < years; ++i) {
            double currentGrowth = startGrowth - ((startGrowth - endGrowth) * i) / (years - 1);

            eps *= 1.0 + (currentGrowth / 100.0);

            dcf += (eps / Math.pow(1.0 + discount, i + 1));
        }

        dcf += ((eps * endMultiple) / Math.pow(1.0 + discount, years));

        return dcf;
    }

    public static double doCashFlowDcfAnalysisWithGrowth(double eps, double startGrowth, double endGrowth) {
        double dcf = 0.0;
        int years = 10;
        double discount = 0.15;

        for (int i = 0; i < years; ++i) {
            double currentGrowth = startGrowth - ((startGrowth - endGrowth) * i) / (years - 1);

            eps *= 1.0 + (currentGrowth / 100.0);

            dcf += (eps / Math.pow(1.0 + discount, i + 1));
        }

        return dcf;
    }

    public static Optional<Double> doFullDcfAnalysisWithGrowth(List<FinancialsTtm> financials, double offsetYear) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offsetYear * 12.0)));

        if (oldIndex == -1) {
            return Optional.empty();
        }
        FinancialsTtm financial = financials.get(oldIndex);

        Optional<Double> pastGrowth = getDcfGrowth(financials, offsetYear, i -> GrowthCalculator.getGrowthInInterval(financials, offsetYear + i, offsetYear));

        double result = 0.0;

        result += financial.keyMetrics.cashPerShare;

        if (pastGrowth.isPresent()) {
            double startMultiplier = 0.7;
            double endMultiplier = 0.5;
            Optional<Double> deviation = GrowthStandardDeviationCounter.calculateEpsGrowthDeviation(financials, offsetYear);
            if (deviation.isPresent()) {
                if (deviation.get() < 10.0) {
                    startMultiplier = 0.9;
                    endMultiplier = 0.7;
                }
            }
            result += doStockDcfAnalysisWithGrowth(financial.incomeStatementTtm.eps, pastGrowth.get() * startMultiplier, pastGrowth.get() * endMultiplier);
        }
        Optional<Double> dividendGrowth = getDcfGrowth(financials, offsetYear, i -> GrowthCalculator.getDividendGrowthInInterval(financials, offsetYear + i, offsetYear));

        if (dividendGrowth.isPresent()) {
            double startMultiplier = 0.9;
            double endMultiplier = 0.75;
            Double dividendGrowth2 = dividendGrowth.get();
            if (dividendGrowth2 > 10.0) {
                dividendGrowth2 = 10.0;
            }

            double divPerShare = calculateDividendPerShare(financial);
            result += doCashFlowDcfAnalysisWithGrowth(divPerShare, dividendGrowth2 * startMultiplier, dividendGrowth2 * endMultiplier);
        }
        return Optional.of(result);
    }

    private static double calculateDividendPerShare(FinancialsTtm financial) {
        if (financial.incomeStatementTtm.weightedAverageShsOut > 0) {
            return -(double) financial.cashFlowTtm.dividendsPaid / financial.incomeStatementTtm.weightedAverageShsOut;
        } else {
            return 0.0;
        }
    }

    private static Optional<Double> getDcfGrowth(List<FinancialsTtm> financials, double offsetYear, Function<Integer, Optional<Double>> growthFunc) {
        List<Double> values = new ArrayList<>();
        for (int i = 10; i >= 5; --i) {
            Optional<Double> growthInInterval = growthFunc.apply(i);
            if (growthInInterval.isPresent()) {
                values.add(growthInInterval.get());
            }
        }
        return values.isEmpty() ? Optional.empty() : Optional.of(values.get(values.size() / 2));
    }

}
