package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class FinancialsTtm implements DateAware, Serializable {
    public LocalDate date;
    public double price;

    public BalanceSheet balanceSheet;

    public CashFlow cashFlow;
    public CashFlow cashFlowTtm;

    public IncomeStatement incomeStatement;
    public IncomeStatement incomeStatementTtm;

    public RemoteRatio remoteRatio;
    public KeyMetrics keyMetrics;

    @Override
    public LocalDate getDate() {
        return date;
    }
}