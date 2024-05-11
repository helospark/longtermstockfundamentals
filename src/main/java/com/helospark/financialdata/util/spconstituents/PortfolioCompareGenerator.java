package com.helospark.financialdata.util.spconstituents;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.watchlist.PortfolioController;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class PortfolioCompareGenerator {
    @Autowired
    private Sp500MetricSaverJob metricsJob;
    @Autowired
    private PortfolioController portfolioController;

    public List<PortfolioCompareData> generate(HttpServletRequest request) {
        GeneralCompanyMetrics sp = metricsJob.loadCachedMetric();
        GeneralCompanyMetrics portfolio = portfolioController.calculateGeneralMetrics(request);

        List<PortfolioCompareData> compare = new ArrayList<>();
        compare.add(createCompareDataNumberSmallerBetter("PE", sp.pe, portfolio.pe, false));
        compare.add(createCompareDataNumberSmallerBetter("PFCF", sp.pfcf, portfolio.pfcf, true));

        compare.add(createCompareDataPercentGreaterBetter("ROIC", sp.roic, portfolio.roic, false));
        compare.add(createCompareDataPercentGreaterBetter("FCF Roic", sp.fcfRoic, portfolio.fcfRoic, false));
        compare.add(createCompareDataPercentGreaterBetter("ROE", sp.roe, portfolio.roe, true));

        compare.add(createCompareDataNumberGreaterBetter("Altman", sp.altman, portfolio.altman, false));
        compare.add(createCompareDataNumberSmallerBetter("D2E", sp.d2e, portfolio.d2e, true));

        compare.add(createCompareDataPercentGreaterBetter("Revenue growth", sp.revGrowth, portfolio.revGrowth, false));
        compare.add(createCompareDataPercentGreaterBetter("EPS growth", sp.epsGrowth, portfolio.epsGrowth, false));
        compare.add(createCompareDataPercentSmallerBetter("Share growth", sp.shareCountGrowth, portfolio.shareCountGrowth, true));

        compare.add(createCompareDataPercentGreaterBetter("Op margin", sp.opMargin, portfolio.opMargin, false));
        compare.add(createCompareDataPercentGreaterBetter("Gross margin", sp.grossMargin, portfolio.grossMargin, false));
        compare.add(createCompareDataPercentGreaterBetter("Net margin", sp.netMargin, portfolio.netMargin, true));

        compare.add(createCompareDataNumberGreaterBetter("IS", sp.investScore, portfolio.investScore, false));

        return compare;
    }

    private PortfolioCompareData createCompareDataPercentGreaterBetter(String name, double sp, double port, boolean rightSeparator) {
        return new PortfolioCompareData(name, formatPercent(sp), formatPercent(port), sp > port, rightSeparator);
    }

    private PortfolioCompareData createCompareDataPercentSmallerBetter(String name, double sp, double port, boolean rightSeparator) {
        return new PortfolioCompareData(name, formatPercent(sp), formatPercent(port), sp < port, rightSeparator);
    }

    private PortfolioCompareData createCompareDataNumberGreaterBetter(String name, double sp, double port, boolean rightSeparator) {
        return new PortfolioCompareData(name, formatNumber(sp), formatNumber(port), sp > port, rightSeparator);
    }

    private PortfolioCompareData createCompareDataNumberSmallerBetter(String name, double sp, double port, boolean rightSeparator) {
        return new PortfolioCompareData(name, formatNumber(sp), formatNumber(port), sp < port, rightSeparator);
    }

    private String formatNumber(double sp) {
        return String.format("%.2f", sp);
    }

    private String formatPercent(double roic) {
        return String.format("%.2f%%", roic);
    }

}
