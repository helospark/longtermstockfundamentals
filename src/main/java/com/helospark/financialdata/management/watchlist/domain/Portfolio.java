package com.helospark.financialdata.management.watchlist.domain;

import java.util.List;
import java.util.Map;

public class Portfolio {
    public List<String> columns = List.of();
    public List<Map<String, String>> portfolio = List.of();

    public List<String> returnsColumns = List.of();
    public List<Map<String, String>> returnsPortfolio = List.of();

    public PieChart industry;
    public PieChart sector;
    public PieChart cap;
    public PieChart country;
    public PieChart profitability;
    public PieChart investments;
    public PieChart peChart;

    public PieChart roicChart;
    public PieChart altmanChart;
    public PieChart growthChart;
    public PieChart icrChart;
    public PieChart grossMarginChart;
    public PieChart shareChangeChart;
    public PieChart piotroskyChart;
}
