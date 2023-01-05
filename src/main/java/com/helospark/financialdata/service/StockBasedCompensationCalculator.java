package com.helospark.financialdata.service;

import com.helospark.financialdata.domain.FinancialsTtm;

public class StockBasedCompensationCalculator {

    public static Double stockBasedCompensationPerMarketCap(FinancialsTtm financialsTtm) {
        return financialsTtm.cashFlowTtm.stockBasedCompensation / (financialsTtm.price * financialsTtm.incomeStatementTtm.weightedAverageShsOut) * 100.0;
    }

}
