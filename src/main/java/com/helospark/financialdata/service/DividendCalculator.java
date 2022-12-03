package com.helospark.financialdata.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.MeanAvg;

public class DividendCalculator {

    public static MeanAvg getDividendsInfo(CompanyFinancials company, int years) {
        List<Double> dividends = new ArrayList<>();
        for (int i = 0; i < years * 4 && i < company.financials.size(); i += 4) {
            FinancialsTtm financial = company.financials.get(i);
            double dividendPerShare = (double) -financial.cashFlowTtm.dividendsPaid / financial.incomeStatementTtm.weightedAverageShsOut;
            double yield = dividendPerShare / company.financials.get(i).price;

            dividends.add(yield);
        }

        Collections.sort(dividends);

        Double meanDividend = dividends.get(dividends.size() / 2);
        double avgDividend = dividends.stream().mapToDouble(a -> a).average().getAsDouble();
        return new MeanAvg(meanDividend, avgDividend);
    }

    public static double getDividendYield(CompanyFinancials company, int element) {
        if (company.financials.size() <= element) {
            return 0;
        }
        FinancialsTtm financial = company.financials.get(element);
        double dividendPerShare = (double) -financial.cashFlowTtm.dividendsPaid / financial.incomeStatementTtm.weightedAverageShsOut;
        double dividendYield = dividendPerShare > 0 ? dividendPerShare / financial.price : 0;
        return dividendYield;
    }

}
