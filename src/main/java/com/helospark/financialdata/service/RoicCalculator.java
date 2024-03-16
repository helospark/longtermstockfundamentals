package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.FinancialsTtm;

public class RoicCalculator {

    public static Optional<Double> getAverageRoic(List<FinancialsTtm> financials, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex == -1) {
            return Optional.empty();
        }

        List<Double> roics = new ArrayList<>();
        for (int i = oldIndex; i < oldIndex + (5 * 4) && i < financials.size(); ++i) {
            var financialsTtm = financials.get(i);
            double roic = calculateRoic(financialsTtm);
            roics.add(roic);
        }
        Collections.sort(roics);
        return Optional.of(roics.get(roics.size() / 2));
    }

    public static double calculateRoic(FinancialsTtm financialsTtm) {
        double ebit = calculateEbit(financialsTtm);
        return ebit / (financialsTtm.balanceSheet.totalAssets - financialsTtm.balanceSheet.totalCurrentLiabilities);
    }

    public static double calculateReturnOnTangibleCapital(FinancialsTtm financialsTtm) {
        double ebit = calculateEbit(financialsTtm);
        long netWorkingCapital = financialsTtm.balanceSheet.totalCurrentAssets - financialsTtm.balanceSheet.totalCurrentLiabilities;
        long fixedAssets = financialsTtm.balanceSheet.propertyPlantEquipmentNet;
        return ebit / (netWorkingCapital + fixedAssets);
    }

    public static double calculateFcfRoic(FinancialsTtm financialsTtm) {
        double fcf = financialsTtm.cashFlowTtm.freeCashFlow;
        double taxRate = (double) financialsTtm.incomeStatementTtm.incomeTaxExpense / financialsTtm.incomeStatementTtm.netIncome;
        return (fcf * (1.0 - taxRate)) / (financialsTtm.balanceSheet.totalStockholdersEquity + financialsTtm.balanceSheet.totalDebt);
    }

    public static Optional<Double> calculateRoiic(List<FinancialsTtm> financials, double offset, double intervalYears) {
        int newIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) ((offset + intervalYears) * 12.0)));

        if (newIndex == -1 || oldIndex == -1) {
            return Optional.empty();
        }

        FinancialsTtm oldData = financials.get(oldIndex);
        FinancialsTtm newData = financials.get(newIndex);

        double oldNopat = calculateNOPAT(oldData);
        double newNopat = calculateNOPAT(newData);

        double oldInvestedCapital = (oldData.balanceSheet.totalStockholdersEquity + oldData.balanceSheet.totalDebt);
        double newInvestedCapital = (newData.balanceSheet.totalStockholdersEquity + newData.balanceSheet.totalDebt);

        return Optional.of((newNopat - oldNopat) / (newInvestedCapital - oldInvestedCapital));
    }

    public static double calculateNOPAT(FinancialsTtm financialsTtm) {
        double ebit = calculateEbit(financialsTtm);
        double taxRate = (double) financialsTtm.incomeStatementTtm.incomeTaxExpense / financialsTtm.incomeStatementTtm.netIncome;

        return (ebit * (1.0 - taxRate));
    }

    public static long calculateEbit(FinancialsTtm financialsTtm) {
        return financialsTtm.incomeStatementTtm.ebitda + financialsTtm.incomeStatementTtm.depreciationAndAmortization;
    }

    public static double calculateROA(FinancialsTtm financialsTtm) {
        return ((double) financialsTtm.incomeStatementTtm.netIncome / financialsTtm.balanceSheet.totalAssets);
    }

    public static Double calculateROTA(FinancialsTtm financialsTtm) {
        if (financialsTtm.balanceSheet.goodwillAndIntangibleAssets == 0) {
            return Double.NaN;
        }
        return (financialsTtm.incomeStatementTtm.netIncome / calculateTangibleAssets(financialsTtm));
    }

    public static double calculateTangibleAssets(FinancialsTtm financialsTtm) {
        return financialsTtm.balanceSheet.totalAssets - financialsTtm.balanceSheet.goodwillAndIntangibleAssets - financialsTtm.balanceSheet.totalLiabilities;
    }

    public static double calculateROE(FinancialsTtm financial) {
        return ((double) financial.incomeStatementTtm.netIncome) / financial.balanceSheet.totalStockholdersEquity;
    }

}
