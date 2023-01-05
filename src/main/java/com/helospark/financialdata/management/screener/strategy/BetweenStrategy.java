package com.helospark.financialdata.management.screener.strategy;

import com.helospark.financialdata.management.screener.ScreenerOperation;

//@Component
//@Order(10)
public class BetweenStrategy implements ScreenerStrategy {

    @Override
    public String getSymbol() {
        return "between";
    }

    @Override
    public int getNumberOfArguments() {
        return 2;
    }

    @Override
    public boolean matches(Double fieldValue, ScreenerOperation operation) {
        return fieldValue >= operation.number1 && fieldValue <= operation.number2;
    }
}
