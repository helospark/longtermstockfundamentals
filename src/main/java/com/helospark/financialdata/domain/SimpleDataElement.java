package com.helospark.financialdata.domain;

import java.time.LocalDate;

public class SimpleDataElement implements DateAware {
    public String date;
    public Double value;

    public SimpleDataElement(String date, Double value) {
        this.date = date;
        this.value = value;
    }

    // hack to make this searchable
    @Override
    public LocalDate getDate() {
        return LocalDate.parse(date);
    }

}
