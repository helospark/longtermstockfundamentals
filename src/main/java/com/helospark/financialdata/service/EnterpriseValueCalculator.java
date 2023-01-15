package com.helospark.financialdata.service;

import com.helospark.financialdata.domain.FinancialsTtm;

public class EnterpriseValueCalculator {

    public static double calculateEv(FinancialsTtm financialsTtm, double price) {
        double marketCap = financialsTtm.incomeStatementTtm.weightedAverageShsOut * price;
        double totalDebt = financialsTtm.balanceSheet.totalDebt;
        double cashAndCashEq = financialsTtm.balanceSheet.cashAndCashEquivalents;

        return marketCap - cashAndCashEq + totalDebt;
    }

}
