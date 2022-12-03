package com.helospark.financialdata.domain;

import java.time.LocalDate;

public class CpiData implements DateAware {
    public LocalDate date;
    public double value;

    @Override
    public LocalDate getDate() {
        return date;
    }

}
