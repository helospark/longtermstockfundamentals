package com.helospark.financialdata.service;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.BalanceSheet;
import com.helospark.financialdata.domain.CashFlow;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.IncomeStatement;
import com.helospark.financialdata.domain.NoTtmNeeded;
import com.helospark.financialdata.management.watchlist.repository.PortfolioPerformanceHistoryElement;
import com.helospark.financialdata.util.spconstituents.WeightedConstituent;

@Component
public class FinancialDataMerger {

    public List<FinancialsTtm> createdWeightedPortfolio(List<PortfolioPerformanceHistoryElement> historicalPerformance) {
        return historicalPerformance.parallelStream()
                .map(a -> processEntity(a))
                .collect(Collectors.toList());
    }

    public FinancialsTtm processEntity(PortfolioPerformanceHistoryElement element) {
        System.out.println("Calculating: " + element.getDate());
        Map<String, CompanyFinancials> localCache = new HashMap<>();
        LocalDate date = element.getDate();
        var holdings = element.getHoldings();

        List<WeightedConstituent> constituents = new ArrayList<>();

        for (var holding : holdings) {
            if (holding.ticket.startsWith("CASH.")) {
                continue;
            }

            CompanyFinancials company = localCache.get(holding.ticket);

            if (company == null) {
                company = DataLoader.readFinancials(holding.ticket);
                localCache.put(holding.ticket, company);
            }
            int index = Helpers.findIndexWithOrBeforeDate(company.financials, date);

            if (index == -1) {
                continue;
            }

            double weight = (double) holding.count / company.financials.get(index).incomeStatementTtm.weightedAverageShsOut;

            constituents.add(new WeightedConstituent(holding.ticket, weight));

        }
        FinancialsTtm mergedEntity = createdWeightedMergedEntity(constituents, localCache, date);
        return mergedEntity;
    }

    public FinancialsTtm createdWeightedMergedEntity(List<WeightedConstituent> constituents, Map<String, CompanyFinancials> localCache, LocalDate date) {
        Map<String, Double> mergedIncomeStatementData = new HashMap<>();
        Map<String, Double> mergedBalanceSheetData = new HashMap<>();
        Map<String, Double> mergedCashflowStatementData = new HashMap<>();

        double price = 0.0;

        for (var cons : constituents) {
            CompanyFinancials financials = localCache.get(cons.symbol);

            int index = Helpers.findIndexWithOrBeforeDate(financials.financials, date);

            FinancialsTtm currentElement = financials.financials.get(index);

            IncomeStatement incomeStatement = currentElement.incomeStatementTtm;
            BalanceSheet balanceSheet = currentElement.balanceSheet;
            CashFlow cashflowStatement = currentElement.cashFlowTtm;
            String reportedCurrency = financials.profile.reportedCurrency;

            calculateFor(mergedIncomeStatementData, cons, incomeStatement, reportedCurrency, date);
            calculateFor(mergedBalanceSheetData, cons, balanceSheet, reportedCurrency, date);
            calculateFor(mergedCashflowStatementData, cons, cashflowStatement, reportedCurrency, date);

            price += currentElement.priceUsd * cons.weight;
        }

        IncomeStatement incomeStatement = convertTo(mergedIncomeStatementData, IncomeStatement.class);
        BalanceSheet balanceSheet = convertTo(mergedBalanceSheetData, BalanceSheet.class);
        CashFlow cashflowStatement = convertTo(mergedCashflowStatementData, CashFlow.class);

        incomeStatement.eps = incomeStatement.netIncome / incomeStatement.weightedAverageShsOut;

        FinancialsTtm ttm = new FinancialsTtm();
        ttm.balanceSheet = balanceSheet;
        ttm.incomeStatement = incomeStatement;
        ttm.incomeStatementTtm = incomeStatement;
        ttm.cashFlowTtm = cashflowStatement;
        ttm.cashFlow = cashflowStatement;
        ttm.price = price;
        ttm.priceTradingCurrency = price;
        ttm.priceUsd = price;
        ttm.date = date;
        return ttm;
    }

    private <T> T convertTo(Map<String, Double> mergedIncomeStatementData, Class<T> class1) {
        try {
            T newInstance = class1.newInstance();

            for (var entry : mergedIncomeStatementData.entrySet()) {
                Field field = class1.getField(entry.getKey());
                Class<?> type = field.getType();
                double value = entry.getValue();

                if (long.class.equals(type)) {
                    field.set(newInstance, (long) value);
                } else if (double.class.equals(type)) {
                    field.set(newInstance, value);
                }

            }

            return newInstance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void calculateFor(Map<String, Double> mergedData, WeightedConstituent cons, Object incomeStatement, String reportedCurrency, LocalDate date) {
        try {
            for (var field : incomeStatement.getClass().getDeclaredFields()) {
                String name = field.getName();
                double value = 0.0;
                boolean found = false;
                if (field.getType().equals(long.class)) {
                    value = (long) field.get(incomeStatement);
                    found = true;
                } else if (field.getType().equals(double.class)) {
                    value = (double) field.get(incomeStatement);
                    found = true;
                }

                if (!reportedCurrency.equals("USD") && found && Double.isFinite(value)) {
                    NoTtmNeeded noTtmNeededAnnotation = field.getAnnotation(NoTtmNeeded.class);
                    if (noTtmNeededAnnotation == null) {
                        value = DataLoader.convertFx(value, reportedCurrency, "USD", date, false).orElse(0.0);
                    }
                }

                if (found && Double.isFinite(value)) {
                    double prevValue = mergedData.getOrDefault(name, 0.0);
                    prevValue += value * cons.weight;
                    mergedData.put(name, prevValue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
