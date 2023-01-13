package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class FinancialsTtm implements DateAware, Serializable {
    public LocalDate date;
    public double price;
    public double priceUsd;

    public BalanceSheet balanceSheet;

    public CashFlow cashFlow;
    public CashFlow cashFlowTtm;

    public IncomeStatement incomeStatement;
    public IncomeStatement incomeStatementTtm;
    public KeyMetrics keyMetrics;

    public FinancialsTtm() {
    }

    public FinancialsTtm(FinancialsTtm latestReport, boolean keepTtm) {
        this.price = latestReport.price;
        this.priceUsd = latestReport.priceUsd;
        this.date = latestReport.date;
        this.incomeStatement = latestReport.incomeStatement;
        this.incomeStatementTtm = keepTtm ? latestReport.incomeStatementTtm : latestReport.incomeStatement;
        this.cashFlow = latestReport.cashFlow;
        this.cashFlowTtm = keepTtm ? latestReport.cashFlowTtm : latestReport.cashFlow;
        this.balanceSheet = latestReport.balanceSheet;
        this.keyMetrics = latestReport.keyMetrics;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }
}
