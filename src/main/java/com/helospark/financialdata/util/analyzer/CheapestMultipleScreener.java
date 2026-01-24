package com.helospark.financialdata.util.analyzer;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.EnterpriseValueCalculator;
import com.helospark.financialdata.service.Helpers;

public class CheapestMultipleScreener {
    public static void main(String[] args) {
        Set<String> symbols = DataLoader.provideSp500Symbols();

        new CheapestMultipleScreener().analyze(symbols);
    }

    public void analyze(Set<String> symbols) {
        //        System.out.println("--- PE ---");
        //        compareMultiple(symbols, f -> calculatePe(f));

        System.out.println("--- p/fcf ---");
        compareMultiple(symbols, f -> calculatePricePerFcf(f));

        //        System.out.println("--- EV/revenue ---");
        //        compareMultiple(symbols, f -> calculateEvPerRevenue(f));
        //
        //        System.out.println("--- EV/FCF ---");
        //        compareMultiple(symbols, f -> calculateEvPerFcf(f));
    }

    public void compareMultiple(Set<String> symbols, Function<FinancialsTtm, Double> function) {
        List<CheapestResult> result = new ArrayList<>();

        symbols.parallelStream().forEach(symbol -> {
            CompanyFinancials company = DataLoader.readFinancials(symbol);

            if (company.financials.size() < 4) {
                return;
            }

            FinancialsTtm latestData2 = company.financials.get(0);
            double multiple2 = function.apply(latestData2);

            double marketCap = latestData2.priceUsd * latestData2.incomeStatement.weightedAverageShsOut;
            if (multiple2 < 0 || marketCap < 1_000_000_000 || !Double.isFinite(multiple2)) {
                return;
            }

            int index = Helpers.findIndexWithOrBeforeDate(company.financials, LocalDate.of(2005, 1, 1));

            if (index == -1) {
                return;
            }

            double years = calculateNumberOfYearsCheapest(company, index, function);

            if (years > 5) {
                result.add(new CheapestResult(symbol, marketCap, multiple2, (int) years));
                //                System.out.println(symbol + "\t" + multiple + "\t" + years);
            }
        });

        Collections.sort(result, (b, a) -> Double.compare(a.lowestSinceYear, b.lowestSinceYear));

        System.out.println("Symbol\tMultiple\tLower since");
        for (var a : result) {
            System.out.printf("%s\t%.2f\t%d years\n", a.symbol, a.currentMultiple, a.lowestSinceYear);
        }
        System.out.println();
        System.out.println();
    }

    private short calculateNumberOfYearsCheapest(CompanyFinancials company, int index, Function<FinancialsTtm, Double> function) {
        if (index + 4 >= company.financials.size()) { // TODO: maybe calculate years from index, because not every company reports 4 times per year
            return 0;
        }
        FinancialsTtm latestData = company.financials.get(index);
        double multiple = function.apply(latestData);
        int i = index + 3;
        for (i = index + 3; i < company.financials.size() - 1; ++i) {
            double newMultiple = function.apply(company.financials.get(i));

            if (newMultiple < multiple || newMultiple < 0) {
                break;
            }
        }

        LocalDate dateThen = company.financials.get(i).date;

        return (short) calculateYearsDiff(dateThen, latestData.date);
    }

    private double calculateYearsDiff(LocalDate date, LocalDate laterDate) {
        return Math.abs(ChronoUnit.DAYS.between(date, laterDate) / 365.0);
    }

    public double calculatePe(FinancialsTtm financialsTtm) {
        //return EnterpriseValueCalculator.calculateEv(financialsTtm, financialsTtm.price) / financialsTtm.incomeStatement.revenue;
        return financialsTtm.price / financialsTtm.incomeStatementTtm.eps;
    }

    public double calculateEvPerRevenue(FinancialsTtm financialsTtm) {
        return EnterpriseValueCalculator.calculateEv(financialsTtm, financialsTtm.price) / financialsTtm.incomeStatement.revenue;
    }

    public double calculateEvPerFcf(FinancialsTtm financialsTtm) {
        return EnterpriseValueCalculator.calculateEv(financialsTtm, financialsTtm.price) / financialsTtm.cashFlowTtm.freeCashFlow;
    }

    public double calculatePricePerFcf(FinancialsTtm financialsTtm) {
        if (financialsTtm.cashFlowTtm.freeCashFlow == 0 || financialsTtm.incomeStatementTtm.weightedAverageShsOut == 0) {
            return Double.NaN;
        }
        return financialsTtm.price / ((double) financialsTtm.cashFlowTtm.freeCashFlow / financialsTtm.incomeStatementTtm.weightedAverageShsOut);
    }

    static class CheapestResult {
        String symbol;
        double size;
        double currentMultiple;
        int lowestSinceYear;

        public CheapestResult(String symbol, double size, double currentMultiple, int lowestSinceYear) {
            this.symbol = symbol;
            this.size = size;
            this.currentMultiple = currentMultiple;
            this.lowestSinceYear = lowestSinceYear;
        }

    }

}
