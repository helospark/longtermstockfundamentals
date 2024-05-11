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
    public PieChart pfcfChart;

    public PieChart roicChart;
    public PieChart altmanChart;
    public PieChart growthChart;
    public PieChart icrChart;
    public PieChart grossMarginChart;
    public PieChart shareChangeChart;
    public PieChart piotroskyChart;
    public PieChart investmentScoreChart;

    public double totalPrice = 0.0;
    public double totalNetAssets = 0.0;
    public double totalEarnings = 0.0;
    public double totalFcf = 0.0;
    public int numberOfStocks = 0;
    public double dividend;

    public double totalRevGrowth = 0.0;
    public double totalEpsGrowth = 0.0;
    public double totalAltman = 0.0;
    public double totalRoic = 0.0;
    public double totalRoe = 0.0;
    public double totalFcfRoic = 0.0;
    public double totalOpMargin = 0;
    public double totalGrossMargin = 0;
    public double totalNetMargin = 0;
    public double totalShareChange = 0;
    public double totalDebtToEquity = 0.0;
    public double investmentScore = 0.0;

    public double oneYearReturn = 0.0;
    public double twoYearReturn = 0.0;
    public double threeYearReturn = 0.0;
    public double fiveYearReturn = 0.0;
    public double tenYearReturn = 0.0;
    public double fifteenYearReturn = 0.0;
    public double twentyYearReturn = 0.0;
    public double expectedTenYrReturn = 0.0;
}
