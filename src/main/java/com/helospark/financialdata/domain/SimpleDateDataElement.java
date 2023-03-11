package com.helospark.financialdata.domain;

import java.time.LocalDate;

public class SimpleDateDataElement implements DateAware {
    public LocalDate date;
    public Double value;

    public SimpleDateDataElement(LocalDate date, Double value) {
        this.date = date;
        this.value = value;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

}
