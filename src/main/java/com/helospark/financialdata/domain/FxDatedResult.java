package com.helospark.financialdata.domain;

import java.time.LocalDate;
import java.util.Map;

public class FxDatedResult implements DateAware {
    public LocalDate date;
    public Map<String, Map<String, Double>> conversions;

    @Override
    public LocalDate getDate() {
        return date;
    }
}
