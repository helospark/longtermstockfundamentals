package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.management.watchlist.domain.CalculatorParameters;

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

        Optional<Double> pastGrowth = getDcfGrowth(financials, offsetYear, i -> GrowthCalculator.getEpsGrowthInInterval(financials, offsetYear + i, offsetYear));

        double result = 0.0;

        result += (double) financial.balanceSheet.cashAndShortTermInvestments / financial.incomeStatementTtm.weightedAverageShsOut;

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
            if (pastGrowth.get() > 100) {
                pastGrowth = Optional.of(100.0);
            }
            result += doStockDcfAnalysisWithGrowth(financial.incomeStatementTtm.eps, pastGrowth.get() * startMultiplier, pastGrowth.get() * endMultiplier);
        }
        Optional<Double> dividendGrowth = getDcfGrowth(financials, offsetYear, i -> GrowthCalculator.getDividendGrowthInInterval(financials, offsetYear + i, offsetYear));

        if (dividendGrowth.isPresent()) {
            double startMultiplier = 0.9;
            double endMultiplier = 0.75;
            Double dividendGrowth2 = dividendGrowth.get();
            if (dividendGrowth2 > 20.0) {
                dividendGrowth2 = 20.0;
            }
            if (dividendGrowth2 < 0.0) {
                dividendGrowth2 = 0.0;
            }

            double divPerShare = calculateDividendPerShare(financial);
            result += doCashFlowDcfAnalysisWithGrowth(divPerShare, dividendGrowth2 * startMultiplier, dividendGrowth2 * endMultiplier);
        }
        if (result < 0.0) {
            return Optional.of(0.0);
        }
        if (result > financial.price * 10) {
            result = financial.price * 10;
        }
        return Optional.of(result);
    }

    public static Optional<Double> doDcfAnalysisRevenueWithDefaultParameters(CompanyFinancials company, double offsetYear) {
        return doDcfAnalysisRevenueWithDefaultParametersAndDiscount(company, offsetYear, 10.0);
    }

    public static Optional<Double> doDcfAnalysisRevenueWithDefaultParametersAndDiscount(CompanyFinancials company, double offsetYear, double discount) {
        var financials = company.financials;
        int index = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offsetYear * 12.0)));

        if (index == -1) {
            return Optional.empty();
        }

        CalculatorParameters calculatorParameters = fillCalculatorParameters(company, offsetYear, discount);

        double revenue = company.financials.get(index).incomeStatementTtm.revenue;
        double shareCount = company.financials.get(index).incomeStatementTtm.weightedAverageShsOut;

        return doDcf(revenue, shareCount, calculatorParameters);
    }

    public static CalculatorParameters fillCalculatorParameters(CompanyFinancials company, double offsetYear, double discount) {
        var financials = company.financials;
        int index = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offsetYear * 12.0)));

        if (index == -1) {
            return new CalculatorParameters();
        }

        double startGrowth = GrowthCalculator.getMedianRevenueGrowth(company.financials, 8, offsetYear).orElse(10.0);
        double startMargin = MarginCalculator.getAvgNetMargin(company.financials, index) * 100.0;
        double startShareCountGrowth = GrowthCalculator.getShareCountGrowthInInterval(company.financials, 5 + offsetYear, offsetYear).orElse(0.0);
        double endGrowth = startGrowth * 0.5;

        double endShareCountGrowth = startShareCountGrowth;

        double endMultiple = 12.0;
        if (endGrowth > 12) {
            endMultiple = endGrowth;
        }
        if (endMultiple > 24) {
            endMultiple = 24.0;
        }
        double endMargin = startMargin;

        CalculatorParameters calculatorParameters = new CalculatorParameters();
        calculatorParameters.discount = discount;
        calculatorParameters.endGrowth = endGrowth;
        calculatorParameters.startGrowth = startGrowth;
        calculatorParameters.endMargin = endMargin;
        calculatorParameters.startMargin = startMargin;
        calculatorParameters.endMultiple = endMultiple;
        calculatorParameters.endPayout = 100.0;
        calculatorParameters.startPayout = 100.0;
        calculatorParameters.startShChange = startShareCountGrowth;
        calculatorParameters.endShChange = endShareCountGrowth;
        return calculatorParameters;
    }

    public static Optional<Double> doDcf(double revenue, double shareCount, CalculatorParameters calculatorParameters) {
        double discount;
        double startGrowth;
        double startMargin;
        double startShareCountGrowth;
        double endGrowth;
        double endShareCountGrowth;
        double endMultiple;
        double endMargin;
        discount = calculatorParameters.discount / 100.0;
        endMultiple = calculatorParameters.endMultiple;

        startGrowth = calculatorParameters.startGrowth / 100.0 + 1.0;
        endGrowth = calculatorParameters.endGrowth / 100.0 + 1.0;

        startMargin = calculatorParameters.startMargin / 100.0;
        endMargin = calculatorParameters.endMargin / 100.0;

        startShareCountGrowth = calculatorParameters.startShChange / 100.0 + 1.0;
        endShareCountGrowth = calculatorParameters.endShChange / 100.0 + 1.0;

        int years = 10;
        double value = 0.0;
        double previousRevenue = revenue;
        double previousShareCount = shareCount;
        double eps = 0.0;
        for (int i = 0; i < years; ++i) {
            double currentGrowth = startGrowth - ((startGrowth - endGrowth) * i) / (years - 1);
            double currentMargin = startMargin - ((startMargin - endMargin) * i) / (years - 1);
            double currentShareChange = startShareCountGrowth - ((startShareCountGrowth - endShareCountGrowth) * i) / (years - 1);

            previousRevenue = previousRevenue * currentGrowth;
            previousShareCount = previousShareCount * currentShareChange;

            double netIncome = previousRevenue * currentMargin;

            eps = netIncome / previousShareCount;

            double discountedEps = (eps / Math.pow(1.0 + discount, i + 1));
            value += discountedEps;
        }

        value += ((eps * endMultiple) / Math.pow(1.0 + discount, years));

        if (!Double.isFinite(value)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public static Optional<Double> doDcfReverseDcfAnalysis(CompanyFinancials company, CalculatorParameters calculatorParameters) {
        int count = 0;
        double lowerBound = -100.0;
        double upperBound = 100.0;
        double offsetYear = 0.0;

        if (company.financials.size() < 10) {
            return Optional.empty();
        }

        if (calculatorParameters == null) {
            calculatorParameters = fillCalculatorParameters(company, offsetYear, 10.0);
        } else {
            calculatorParameters = CalculatorParameters.deepClone(calculatorParameters);
        }

        double revenue = company.financials.get(0).incomeStatementTtm.revenue;
        double shareCount = company.financials.get(0).incomeStatementTtm.weightedAverageShsOut;

        double discountToTest = (upperBound + lowerBound) / 2.0;

        while (lowerBound < upperBound && count < 10) {
            discountToTest = (upperBound + lowerBound) / 2.0;
            calculatorParameters.discount = discountToTest;
            Optional<Double> resultOptional = doDcf(revenue, shareCount, calculatorParameters);
            if (!resultOptional.isPresent()) {
                return Optional.empty();
            }
            double result = (resultOptional.get() / company.latestPrice - 1.0) * 100.0;

            if (Math.abs(result) < 0.05) {
                break;
            }

            if (result < 0.0) {
                upperBound = discountToTest;
            } else {
                lowerBound = discountToTest;
            }
            ++count;
        }
        return Optional.of(discountToTest);
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
        for (int i = 10; i >= 3; --i) {
            Optional<Double> growthInInterval = growthFunc.apply(i);
            if (growthInInterval.isPresent()) {
                values.add(growthInInterval.get());
            }
        }
        Collections.sort(values);
        return values.isEmpty() ? getShortTermGrowth(financials, offsetYear, growthFunc) : Optional.of(values.get(values.size() / 2));
    }

    private static Optional<Double> getShortTermGrowth(List<FinancialsTtm> financials, double offsetYear, Function<Integer, Optional<Double>> growthFunc) {
        for (int i = 2; i >= 1; --i) {
            Optional<Double> growthInInterval = growthFunc.apply(i);
            if (growthInInterval.isPresent()) {
                return growthInInterval.map(a -> a * 0.7);
            }
        }
        return Optional.empty();
    }

}
