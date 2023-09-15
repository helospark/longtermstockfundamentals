package com.helospark.financialdata.management.watchlist.domain;

import java.util.List;

public class PieChart {
    public List<String> keys = List.of();
    public List<Double> values = List.of();

    public PieChart(List<String> keys, List<Double> values) {
        this.keys = keys;
        this.values = values;
    }

}
