package com.helospark.financialdata.management.screener.domain;

import com.helospark.financialdata.management.screener.ScreenerRequest;

public class BacktestRequest extends ScreenerRequest {
    public int startYear;
    public int endYear;

    @Override
    public String toString() {
        return "BacktestRequest [startYear=" + startYear + ", endYear=" + endYear + ", operations=" + operations + ", exchanges=" + exchanges + ", lastItem=" + lastItem + ", prevItem=" + prevItem
                + "]";
    }

}
