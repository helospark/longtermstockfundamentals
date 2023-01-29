package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class EconomicPriceElement implements DateAware, Serializable {
    public LocalDate date;
    public double value;

    @Override
    public LocalDate getDate() {
        return date;
    }

}
