package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class HistoricalPriceElement implements DateAware, Serializable {
    public LocalDate date;
    public double close;

    @Override
    public LocalDate getDate() {
        return date;
    }

}
