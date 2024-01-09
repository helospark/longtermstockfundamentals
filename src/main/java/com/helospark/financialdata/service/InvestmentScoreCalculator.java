package com.helospark.financialdata.service;

import java.time.LocalDate;
import java.util.Optional;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.management.watchlist.PortfolioController;

public class InvestmentScoreCalculator {
    private static final double ROIC_WEIGHT = 1.5;
    private static final double FCF_ROIC_WEIGHT = 1.5;
    private static final double ALTMAN_WEIGHT = 1.0;
    private static final double ROE_WEIGHT = 0.3;
    private static final double SHARE_CHANGE_WEIGHT_PER_YEAR = 0.2;
    private static final double POSITIVE_EQUITY_WEIGHT = 0.15;
    private static final double EQUITY_GROWTH_WEIGHT_PER_YEAR = 0.03;
    private static final double PIOTROSKY_WEIGHT = 0.7;
    private static final double DCF_WEIGHT = 0.65;
    private static final double OPERATIVE_MARGIN_WEIGHT = 0.2;
    private static final double OPERATIVE_CASHFLOW_MARGIN_WEIGHT = 0.2;
    private static final double GROSS_MARGIN_WEIGHT = 0.35;
    private static final double NET_MARGIN_WEIGHT = 0.3;
    private static final double ICR_WEIGHT = 0.3;

    private static final double REVENUE_GROWTH_WEIGHT_PER_YEAR = 0.10;
    private static final double EPS_GROWTH_WEIGHT_PER_YEAR = 0.12;
    private static final double FCF_GROWTH_WEIGHT_PER_YEAR = 0.08;

    private static final double[] EQUITY_GROWTH_RANGES = new double[] { 0, 3, 7, 10, 14 };
    public static final double[] FCF_GROWTH_RANGES = new double[] { 0, 6, 10, 15, 20 };
    public static final double[] NET_MARGIN_RANGES = new double[] { 0, 10, 12, 17, 20 };

    public static Optional<Double> calculate(CompanyFinancials company, double offsetYear) {
        int i = Helpers.findIndexWithOrBeforeDate(company.financials, LocalDate.now().minusMonths((int) (offsetYear * 12.0)));
        if (i == -1 || i >= company.financials.size()) {
            return Optional.empty();
        }

        double maxScore = 0.0;
        double currentScore = 0.0;

        FinancialsTtm currentFinancial = company.financials.get(i);

        boolean isBank = company.profile.industry != null && company.profile.industry.toUpperCase().contains("BANKS");

        if (!isBank) {
            double roic = RoicCalculator.calculateRoic(currentFinancial) * 100.0;

            maxScore += ROIC_WEIGHT;
            currentScore += getScoreAsc(roic, ROIC_WEIGHT, PortfolioController.ROIC_RANGES);

            maxScore += FCF_ROIC_WEIGHT;
            double fcfRoic = RoicCalculator.calculateFcfRoic(currentFinancial) * 100.0;
            currentScore += getScoreAsc(fcfRoic, FCF_ROIC_WEIGHT, PortfolioController.FCF_ROIC_RANGES);

            double altman = AltmanZCalculator.calculateAltmanZScore(currentFinancial, currentFinancial.price);
            maxScore += ALTMAN_WEIGHT;
            currentScore += getScoreAsc(altman, ALTMAN_WEIGHT, PortfolioController.ALTMAN_RANGES);
        }

        for (int year : new int[] { 1, 2, 5, 10 }) {
            Optional<Double> shareChange5yr = GrowthCalculator.getShareCountGrowthInInterval(company.financials, offsetYear + year, offsetYear + 0);
            if (shareChange5yr.isPresent()) {
                maxScore += SHARE_CHANGE_WEIGHT_PER_YEAR;
                currentScore += getScoreDesc(shareChange5yr.get(), SHARE_CHANGE_WEIGHT_PER_YEAR, PortfolioController.SHARE_CHANGE_RANGES);
            }
        }

        double equity = currentFinancial.balanceSheet.totalStockholdersEquity;
        maxScore += POSITIVE_EQUITY_WEIGHT;
        if (equity > 0.0) {
            currentScore += POSITIVE_EQUITY_WEIGHT;
        }

        if (equity > 0.0) {
            double roe = RoicCalculator.calculateROE(currentFinancial) * 100.0;

            maxScore += ROE_WEIGHT;
            currentScore += getScoreAsc(roe, ROE_WEIGHT, PortfolioController.ROE_RANGES);
        }

        for (int year : new int[] { 1, 2, 3, 5, 7, 10 }) {
            Optional<Double> equityGrowth = GrowthCalculator.getEquityPerShareGrowthInInterval(company.financials, offsetYear + year, offsetYear + 0);
            if (equityGrowth.isPresent()) {
                maxScore += EQUITY_GROWTH_WEIGHT_PER_YEAR;
                currentScore += getScoreAsc(equityGrowth.get(), EQUITY_GROWTH_WEIGHT_PER_YEAR, EQUITY_GROWTH_RANGES);
            }
        }

        maxScore += PIOTROSKY_WEIGHT;
        Optional<Integer> piotrosky = PietroskyScoreCalculator.calculatePietroskyScore(company, currentFinancial);
        if (piotrosky.isPresent()) {
            currentScore += getScoreAsc(piotrosky.get(), PIOTROSKY_WEIGHT, PortfolioController.PIOTROSKY_RANGES);
        }

        for (int year : new int[] { 1, 2, 3, 5, 7, 10 }) {
            Optional<Double> revGrowth = GrowthCalculator.getRevenueGrowthInInterval(company.financials, offsetYear + year, offsetYear + 0);
            if (revGrowth.isPresent()) {
                maxScore += REVENUE_GROWTH_WEIGHT_PER_YEAR;
                currentScore += getScoreAsc(revGrowth.get(), REVENUE_GROWTH_WEIGHT_PER_YEAR, PortfolioController.REVENUE_RANGES);
            }
        }
        for (int year : new int[] { 1, 2, 3, 5, 7, 10 }) {
            Optional<Double> revGrowth = GrowthCalculator.getEpsGrowthInInterval(company.financials, offsetYear + year, offsetYear + 0);
            if (revGrowth.isPresent()) {
                maxScore += EPS_GROWTH_WEIGHT_PER_YEAR;
                currentScore += getScoreAsc(revGrowth.get(), EPS_GROWTH_WEIGHT_PER_YEAR, PortfolioController.GROWTH_RANGES);
            }
        }
        for (int year : new int[] { 1, 2, 3, 5, 7, 10 }) {
            Optional<Double> revGrowth = GrowthCalculator.getFcfGrowthInInterval(company.financials, offsetYear + year, offsetYear + 0);
            if (revGrowth.isPresent()) {
                maxScore += FCF_GROWTH_WEIGHT_PER_YEAR;
                currentScore += getScoreAsc(revGrowth.get(), FCF_GROWTH_WEIGHT_PER_YEAR, FCF_GROWTH_RANGES);
            }
        }

        maxScore += DCF_WEIGHT;
        Optional<Double> dcfTargetPrice = DcfCalculator.doDcfAnalysisRevenueWithDefaultParameters(company, offsetYear);
        if (dcfTargetPrice.isPresent()) {
            double dcfMos = (dcfTargetPrice.get() / currentFinancial.price - 1.0) * 100.0;
            if (dcfMos > 50.0) {
                currentScore += DCF_WEIGHT;
            } else if (dcfMos > 0.0) {
                currentScore += DCF_WEIGHT / 1.3;
            }
        }

        double opMargin = RatioCalculator.calculateOperatingMargin(currentFinancial) * 100.0;
        if (Double.isFinite(opMargin)) {
            maxScore += OPERATIVE_MARGIN_WEIGHT;
            currentScore += getScoreAsc(opMargin, OPERATIVE_MARGIN_WEIGHT, PortfolioController.OP_MARGIN_RANGES);
        }

        double opCMargin = RatioCalculator.calculateOperatingCashflowMargin(currentFinancial) * 100.0;
        if (Double.isFinite(opCMargin)) {
            maxScore += OPERATIVE_CASHFLOW_MARGIN_WEIGHT;
            currentScore += getScoreAsc(opCMargin, OPERATIVE_CASHFLOW_MARGIN_WEIGHT, PortfolioController.OP_MARGIN_RANGES);
        }

        double grossMargin = RatioCalculator.calculateGrossProfitMargin(currentFinancial) * 100.0;
        if (Double.isFinite(grossMargin)) {
            maxScore += GROSS_MARGIN_WEIGHT;
            currentScore += getScoreAsc(grossMargin, GROSS_MARGIN_WEIGHT, PortfolioController.GROSS_MARGIN_RANGES);
        }

        double netMargin = RatioCalculator.calculateNetMargin(currentFinancial) * 100.0;
        if (Double.isFinite(netMargin)) {
            maxScore += NET_MARGIN_WEIGHT;
            currentScore += getScoreAsc(netMargin, NET_MARGIN_WEIGHT, NET_MARGIN_RANGES);
        }

        Double icr = RatioCalculator.calculateInterestCoverageRatio(currentFinancial);
        if (icr != null) {
            maxScore += ICR_WEIGHT;
            currentScore += getScoreAsc(icr, ICR_WEIGHT, PortfolioController.ICR_RANGES);
        }

        return Optional.of((currentScore / maxScore) * 10.0);
    }

    public static double getScoreAsc(double value, double weight, double... ranges) {
        double awfulThreshold = ranges[0];
        double badThreshold = ranges[1];
        double neutralThreshold = ranges[2];
        double goodThreshold = ranges[3];
        double greatThreshold = ranges[4];

        if (value < awfulThreshold) {
            return 0.0;
        } else if (value < badThreshold) {
            return weight / 4.0;
        } else if (value < neutralThreshold) {
            return weight / 2.0;
        } else if (value < goodThreshold) {
            return weight / 1.7;
        } else if (value < greatThreshold) {
            return weight / 1.3;
        } else {
            return weight;
        }
    }

    public static double getScoreDesc(double value, double weight, double... ranges) {
        double awfulThreshold = ranges[0];
        double badThreshold = ranges[1];
        double neutralThreshold = ranges[2];
        double goodThreshold = ranges[3];
        double greatThreshold = ranges[4];

        if (value > awfulThreshold) {
            return 0.0;
        } else if (value > badThreshold) {
            return weight / 4.0;
        } else if (value > neutralThreshold) {
            return weight / 2.0;
        } else if (value > goodThreshold) {
            return weight / 1.7;
        } else if (value > greatThreshold) {
            return weight / 1.3;
        } else {
            return weight;
        }
    }

}
