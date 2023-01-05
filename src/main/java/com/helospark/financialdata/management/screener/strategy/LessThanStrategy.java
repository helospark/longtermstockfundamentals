package com.helospark.financialdata.management.screener.strategy;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.screener.ScreenerOperation;

@Component
public class LessThanStrategy implements ScreenerStrategy {

    @Override
    public String getSymbol() {
        return "<";
    }

    @Override
    public int getNumberOfArguments() {
        return 1;
    }

    @Override
    public boolean matches(Double fieldValue, ScreenerOperation operation) {
        return fieldValue < operation.number1;
    }
}
