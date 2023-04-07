package com.helospark.financialdata.management.screener;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.helospark.financialdata.management.screener.strategy.ScreenerStrategy;

public class ScreenerOperation {
    public String id;
    public String operation;
    public Double number1;
    public Double number2;

    @JsonIgnore
    public ScreenerStrategy screenerStrategy;

    @Override
    public String toString() {
        return id + " " + operation + " " + number1;
    }

}
