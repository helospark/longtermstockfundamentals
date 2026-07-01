package com.helospark.financialdata.management.screener.strategy;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.screener.ScreenerOperation;

@Component
public class NotStrategy implements ScreenerStrategy {

    @Override
    public String getSymbol() {
        return "not in";
    }

    @Override
    public int getNumberOfArguments() {
        return -1;
    }

    @Override
    public boolean matches(Double fieldValue, ScreenerOperation operation) {
        int actualValue = (int) Math.round(fieldValue);

        for (var element : operation.numberList) {
            if (actualValue == element) {
                return false;
            }
        }

        return true;
    }
}
