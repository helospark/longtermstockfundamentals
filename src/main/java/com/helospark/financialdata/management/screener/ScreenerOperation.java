package com.helospark.financialdata.management.screener;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.helospark.financialdata.management.screener.annotation.ScreenerElement;
import com.helospark.financialdata.management.screener.strategy.ScreenerColumnListProvider;
import com.helospark.financialdata.management.screener.strategy.ScreenerStrategy;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class ScreenerOperation {
    public AtGlanceField id;
    public String operation;
    public Double number1;
    public Double number2;
    public int[] numberList;

    @JsonIgnore
    public ScreenerStrategy screenerStrategy;

    @Override
    public String toString() {
        String value = "";

        if (numberList != null) {
            ScreenerElement annotation = getAnnotation();
            if (annotation != null) {
                Map<Integer, String> map = ScreenerColumnListProvider.provideValuesInverted().get(annotation.listProvider());
                value = "[" + Arrays.stream(numberList).mapToObj(a -> map.get(a)).collect(Collectors.joining(", ")) + "]";
            } else {
                value = "[" + Arrays.stream(numberList).mapToObj(a -> String.valueOf(a)).collect(Collectors.joining(", ")) + "]";
            }
        } else {
            value = String.valueOf(number1);
        }

        return id + " " + operation + " " + value;
    }

    public ScreenerElement getAnnotation() {
        try {
            Field field = AtGlanceData.class.getField(id.name());
            return field.getDeclaredAnnotation(ScreenerElement.class);
        } catch (Exception e) {
            return null;
        }
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
        earnings_yield,
        drawdown,
        sector;

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
