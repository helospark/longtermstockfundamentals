package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;

public class WatchlistExpectationHistoryElement {
    public String symbol;
    public String saveDate;
    public List<String> dates;
    public List<Double> revenue;
    public List<Double> eps;
    public List<Double> margin;
    public List<Double> shareCount;
}
