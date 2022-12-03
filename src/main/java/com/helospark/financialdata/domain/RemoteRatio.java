package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class RemoteRatio implements DateAware, Serializable {
    public LocalDate date; //2022-09-30,
    public Double currentRatio; //1.4525290215588722,
    public Double quickRatio; //1.2392205638474296,
    public Double cashRatio; //1.2184908789386402,
    public Double daysOfSalesOutstanding; //317.39130434782606,
    public Double daysOfInventoryOutstanding; //0.0,
    public Double operatingCycle; //78.26086956521739,
    public Double daysOfPayablesOutstanding; //638.3333333333334,
    public Double cashConversionCycle; //-560.072463768116,
    public Double grossProfitMargin; //0.5304347826086957,
    public Double operatingProfitMargin; //-24.495652173913044,
    public Double pretaxProfitMargin; //-24.77391304347826,
    public Double netProfitMargin; //-24.77391304347826,
    public Double effectiveTaxRate; //0.0,
    public Double returnOnAssets; //-0.18055643576906014,
    public Double returnOnEquity; //-0.2644574398960364,
    public Double returnOnCapitalEmployed; //-0.2571428571428571,
    public Double netIncomePerEBT; //1.0,
    public Double ebtPerEbit; //1.0113596024139155,
    public Double ebitPerRevenue; //-24.495652173913044,
    public Double debtRatio; //0.31725711388554406,
    public Double debtEquityRatio; //0.46468021906618395,
    public Double longTermDebtToCapitalization; //0.006180811808118082,
    public Double totalDebtToCapitalization; //0.1478405315614618,
    public Double interestCoverage; //-68.70731707317073,
    public Double cashFlowToDebtRatio; //-0.19743178170144463,
    public Double companyEquityMultiplier; //1.464680219066184,
    public Double receivablesTurnover; //1.15,
    public Double payablesTurnover; //0.1409921671018277,
    public Double inventoryTurnover; //null,
    public Double fixedAssetTurnover; //0.8041958041958042,
    public Double assetTurnover; //0.007288167817985931,
    public Double operatingCashFlowPerShare; //-0.02271291977353557,
    public Double freeCashFlowPerShare; //-0.1189196775134708,
    public Double cashPerShare; //0.3618063480456425,
    public Double payoutRatio; //0.0,
    public Double operatingCashFlowSalesRatio; //-3.208695652173913,
    public Double freeCashFlowOperatingCashFlowRatio; //5.235772357723577,
    public Double cashFlowCoverageRatios; //-0.19743178170144463,
    public Double shortTermCoverageRatios; //-0.20477247502774695,
    public Double capitalExpenditureCoverageRatio; //0.236084452975048,
    public Double dividendPaidAndCapexCoverageRatio; //null,
    public Double dividendPayoutRatio; //null,
    public Double priceBookValueRatio; //1.0330166248955723,
    public Double priceToBookRatio; //1.0330166248955723,
    public Double priceToSalesRatio; //96.77120086956523,
    public Double priceEarningsRatio; //-0.9765433573183574,
    public Double priceToFreeCashFlowsRatio; //-5.760190527950312,
    public Double priceToOperatingCashFlowsRatio; //-30.159046341463416,
    public Double priceCashFlowRatio; //-30.159046341463416,
    public Double priceEarningsToGrowthRatio; //-0.03487654847565562,
    public Double priceSalesRatio; //96.77120086956523,
    public Double dividendYield; //null,
    public Double enterpriseValueMultiple; //-2.572141654624278,
    public Double priceFairValue; //1.0330166248955723

    @Override
    public LocalDate getDate() {
        return date;
    }
}
