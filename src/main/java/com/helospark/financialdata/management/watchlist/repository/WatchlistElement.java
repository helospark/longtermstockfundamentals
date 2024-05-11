package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;

import com.helospark.financialdata.management.watchlist.domain.CalculatorParameters;
import com.helospark.financialdata.management.watchlist.domain.Moats;

public class WatchlistElement {
    public String symbol;
    public List<String> tags = List.of();
    public Double targetPrice;
    public String notes;
    public int ownedShares = 0;
    public CalculatorParameters calculatorParameters;
    public Moats moats;
}
