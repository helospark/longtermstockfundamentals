package com.helospark.financialdata.management.screener.strategy;

import com.helospark.financialdata.management.screener.ScreenerOperation;

public interface ScreenerStrategy {

    public String getSymbol();

    public int getNumberOfArguments();

    public boolean matches(Double fieldValue, ScreenerOperation operation);

}
