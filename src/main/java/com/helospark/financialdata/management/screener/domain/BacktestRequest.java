package com.helospark.financialdata.management.screener.domain;

import java.util.ArrayList;
import java.util.List;

import com.helospark.financialdata.management.screener.ScreenerRequest;

public class BacktestRequest extends ScreenerRequest {
    public int startYear;
    public int endYear;
    public boolean addResultTable = true;
    public List<String> excludedStocks = new ArrayList<>();

    @Override
    public String toString() {
        return "BacktestRequest [startYear=" + startYear + ", endYear=" + endYear + ", operations=" + operations + ", exchanges=" + exchanges + ", lastItem=" + lastItem + ", prevItem=" + prevItem
                + "]";
    }

}
