package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;

public class WatchlistElement {
    public String symbol;
    public List<String> tags = List.of();
    public Double targetPrice;
    public String notes;

}
