package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class BalanceSheet implements DateAware, Serializable {
    public LocalDate date; //2022-09-24,
    public String reportedCurrency; //USD,
    public String period; //Q4,
    public long cashAndCashEquivalents; //23646000000,
    public long shortTermInvestments; //24658000000,
    public long cashAndShortTermInvestments; //48304000000,
    public long netReceivables; //60932000000,
    public long inventory; //4946000000,
    public long otherCurrentAssets; //21223000000,
    public long totalCurrentAssets; //135405000000,
    public long propertyPlantEquipmentNet; //42117000000,
    public long goodwill; //0.0,
    public long intangibleAssets; //0.0,
    public long goodwillAndIntangibleAssets; //0.0,
    public long longTermInvestments; //120805000000,
    public long taxAssets; //0.0,
    public long otherNonCurrentAssets; //54428000000,
    public long totalNonCurrentAssets; //217350000000,
    public long otherAssets; //0.0,
    public long totalAssets; //352755000000,
    public long accountPayables; //64115000000,
    public long shortTermDebt; //21110000000,
    public long taxPayables; //0.0,
    public long deferredRevenue; //7912000000,
    public long otherCurrentLiabilities; //60845000000,
    public long totalCurrentLiabilities; //153982000000,
    public long longTermDebt; //98959000000,
    public long deferredRevenueNonCurrent; //0.0,
    public long deferredTaxLiabilitiesNonCurrent; //0.0,
    public long otherNonCurrentLiabilities; //49142000000,
    public long totalNonCurrentLiabilities; //148101000000,
    public long otherLiabilities; //0.0,
    public long capitalLeaseObligations; //0.0,
    public long totalLiabilities; //302083000000,
    public long preferredStock; //0,
    public long commonStock; //64849000000,
    public long retainedEarnings; //-3068000000,
    public long accumulatedOtherComprehensiveIncomeLoss; //-11109000000,
    public long othertotalStockholdersEquity; //0.0,
    public long totalStockholdersEquity; //50672000000,
    public long totalLiabilitiesAndStockholdersEquity; //352755000000,
    public long minorityInterest; //0,
    public long totalEquity; //50672000000,
    public long totalLiabilitiesAndTotalEquity; //352755000000,
    public long totalInvestments; //145463000000,
    public long totalDebt; //120069000000,
    public long netDebt; //96423000000,

    @Override
    public LocalDate getDate() {
        return date;
    }

}
