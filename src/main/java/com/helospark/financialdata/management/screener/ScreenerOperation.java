package com.helospark.financialdata.management.screener;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.helospark.financialdata.management.screener.strategy.ScreenerStrategy;

public class ScreenerOperation {
    public AtGlanceField id;
    public String operation;
    public Double number1;
    public Double number2;

    @JsonIgnore
    public ScreenerStrategy screenerStrategy;

    @Override
    public String toString() {
        return id + " " + operation + " " + number1;
    }

    // For increased performance
    public static enum AtGlanceField {
        marketCapUsd,
        trailingPeg,
        roic,
        altman,
        pietrosky,
        pe,
        evToEbitda,
        ptb,
        pts,
        icr,
        currentRatio,
        quickRatio,
        dtoe,
        roe,
        epsGrowth,
        fcfGrowth,
        revenueGrowth,
        fYrIncomeGr,
        dividendGrowthRate,
        shareCountGrowth,
        netMarginGrowth,
        cape,
        epsSD,
        revSD,
        fcfSD,
        epsFcfCorrelation,
        dividendYield,
        dividendPayoutRatio,
        dividendFcfPayoutRatio,
        profitableYears,
        fcfProfitableYears,
        stockCompensationPerMkt,
        cpxToRev,
        sloan,
        ideal10yrRevCorrelation,
        ideal10yrEpsCorrelation,
        ideal10yrFcfCorrelation,
        fvCalculatorMoS,
        fvCompositeMoS,
        grahamMoS,
        starFlags,
        redFlags,
        yellowFlags,
        greenFlags,
        fYrPe,
        fYrPFcf,
        fiveYrRoic,
        ltl5Fcf,
        price10Gr,
        price15Gr,
        price20Gr,
        grMargin,
        opMargin,
        opCMargin,
        fcfMargin,
        tpr,
        ebitdaMargin,
        price5Gr,
        epsGrowth2yr,
        fcfGrowth2yr,
        revenueGrowth2yr,
        shareCountGrowth2yr,
        equityGrowth2yr,
        roa,
        rota,
        assetTurnoverRatio,
        priceToGrossProfit,
        equityGrowth,
        investmentScore,
        peExRnd,
        peExMnS,
        epsGrExRnd,
        epsGrExMnS,
        peCheapestYears,
        pfcfCheapestYears,
        evRevenueCheapestYears,
        evFcfCheapestYears,
        smoothRevenue5yr,
        smoothRevenue10yr,
        smoothEps5yr,
        smoothEps10yr,
        smoothFcf5yr,
        smoothFcf10yr,
        smoothEquity5yr,
        smoothEquity10yr,
        fcf_yield,
        earnings_yield;

        @JsonCreator
        public static AtGlanceField fromString(String id) {
            AtGlanceField result = AtGlanceField.valueOf(id);
            if (result == null) {
                throw new ScreenerClientSideException("Unexpected type " + id);
            }
            return result;
        }
    }

}
