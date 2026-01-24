package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;

public class SmoothnessCalculator {

    public static double calculateSmoothnessOfRevenue(CompanyFinancials company) {
        return calculateSmoothnessBetweenIndices(company.financials, company.financials.size() - 1, 0, f -> (double) f.incomeStatementTtm.revenue);
    }

    public static double calculateSmoothnessOfEps(CompanyFinancials company) {
        return calculateSmoothnessBetweenIndices(company.financials, company.financials.size() - 1, 0, f -> (double) f.incomeStatementTtm.eps);
    }

    public static double calculateSmoothnessOfFcf(CompanyFinancials company) {
        return calculateSmoothnessBetweenIndices(company.financials, company.financials.size() - 1, 0, f -> (double) f.cashFlowTtm.freeCashFlow / f.incomeStatementTtm.weightedAverageShsOut);
    }

    public static double calculateSmoothnessOfEquity(CompanyFinancials company) {
        return calculateSmoothnessBetweenIndices(company.financials, company.financials.size() - 1, 0, f -> (double) f.balanceSheet.totalStockholdersEquity / f.incomeStatementTtm.weightedAverageShsOut);
    }

    public static double calculateSmoothnessOfRevenue(CompanyFinancials company, double offset, double years) {
        return calculateSmoothessYears(company, offset, years, f -> (double) f.incomeStatementTtm.revenue);
    }

    public static double calculateSmoothnessOfEps(CompanyFinancials company, double offset, double years) {
        return calculateSmoothessYears(company, offset, years, f -> (double) f.incomeStatementTtm.eps);
    }

    public static double calculateSmoothnessOfFcf(CompanyFinancials company, double offset, double years) {
        return calculateSmoothessYears(company, offset, years, f -> (double) f.cashFlowTtm.freeCashFlow / f.incomeStatementTtm.weightedAverageShsOut);
    }

    public static double calculateSmoothnessOfEquity(CompanyFinancials company, double offset, double years) {
        return calculateSmoothessYears(company, offset, years, f -> (double) f.balanceSheet.totalStockholdersEquity / f.incomeStatementTtm.weightedAverageShsOut);
    }

    public static double calculateSmoothessYears(CompanyFinancials company, double offset, double years, Function<FinancialsTtm, Double> func) {
        List<FinancialsTtm> financials = company.financials;
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) ((offset + years) * 12.0)));
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex == -1 || newIndex == -1 || oldIndex <= newIndex) {
            return 0.0;
        }

        return calculateSmoothnessBetweenIndices(financials, oldIndex, newIndex, func);
    }

    public static double calculateSmoothnessBetweenIndices(List<FinancialsTtm> financials, int oldIndex, int newIndex, Function<FinancialsTtm, Double> func) {
        List<DateValuePair> listOfPairs = new ArrayList<>();
        for (int i = newIndex; i < oldIndex; ++i) {
            FinancialsTtm financial = financials.get(i);
            listOfPairs.add(new DateValuePair(financial.date, func.apply(financial)));
        }

        return calculateSmoothnessScore(listOfPairs);
    }

    private static double calculateSmoothnessScore(List<DateValuePair> listOfPairs) {
        if (listOfPairs.size() < 3) {
            return 0.0;
        }

        int n = listOfPairs.size();
        double[] x = new double[n];
        double[] y = new double[n];

        int i = 0;
        long firstEpochDay = listOfPairs.get(0).date.toEpochDay();

        for (DateValuePair entry : listOfPairs) {
            x[i] = entry.date.toEpochDay() - firstEpochDay;
            y[i] = Math.log(entry.value > 0 ? entry.value : 1.0);
            i++;
        }

        return calculateRSquared(x, y);
    }

    private static double calculateRSquared(double[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumY = 0, sumXX = 0, sumYY = 0, sumXY = 0;

        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXX += x[i] * x[i];
            sumYY += y[i] * y[i];
            sumXY += x[i] * y[i];
        }

        double numerator = (n * sumXY - sumX * sumY);
        double denominator = Math.sqrt((n * sumXX - sumX * sumX) * (n * sumYY - sumY * sumY));

        if (denominator == 0)
            return 0.0;

        double r = numerator / denominator;
        return r * r;
    }

    static class DateValuePair {
        LocalDate date;
        double value;

        public DateValuePair(LocalDate date, double value) {
            this.date = date;
            this.value = value;
        }

        @Override
        public String toString() {
            return "DateValuePair [date=" + date + ", value=" + value + "]";
        }

    }

}
