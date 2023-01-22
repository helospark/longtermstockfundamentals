package com.helospark.financialdata.domain;

import java.time.LocalDate;

public class InsiderTradingElement implements DateAware {
    public LocalDate date;
    public double amount;
    public boolean only10PercentOwner;

    @Override
    public LocalDate getDate() {
        return date;
    }

}
