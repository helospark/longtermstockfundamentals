package com.helospark.financialdata.domain;

import java.time.LocalDate;

public class PortfolioElement implements DateAware {
    public LocalDate date; // 2022-09-30",
    public String tickercusip; // JNJ",
    public long shares; // 327100,
    public long value; // 53435000,

    @Override
    public LocalDate getDate() {
        return date;
    }
}
