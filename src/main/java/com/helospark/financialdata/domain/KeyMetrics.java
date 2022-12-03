package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class KeyMetrics implements DateAware, Serializable {
    public LocalDate date; //2022-09-30,

    public double revenuePerShare; //36.00878622208945,
    public double netIncomePerShare; //0.06698807060501039,
    public double operatingCashFlowPerShare; //0.7395758457700957,
    public double freeCashFlowPerShare; //1.1780365524433358,
    public double cashPerShare; //5.398097051648837,
    public double bookValuePerShare; //9.998959912174435,
    public double tangibleBookValuePerShare; //8.702272798083673,
    public double shareholdersEquityPerShare; //9.998959912174435,
    public double interestDebtPerShare; //13.07613351245448,
    public double marketCap; //13502631369,
    public double enterpriseValue; //22644212369,
    public double peRatio; //46.17799814298026,
    public double priceToSalesRatio; //0.34362446775308203,
    public double pocfratio; //16.730535577613256,
    public double pfcfRatio; //10.503494118528357,
    public double pbRatio; //1.2374787086539267,
    public double ptbRatio; //1.2374787086539267,
    public double evToSales; //0.5762658559167677,
    public double enterpriseValueOverEBITDA; //86.27372321878283,
    public double evToOperatingCashFlow; //28.057479339646825,
    public double evToFreeCashFlow; //17.614592662475474,
    public double earningsYield; //0.005413833644887088,
    public double freeCashFlowYield; //0.09520641309599837,
    public double debtToEquity; //0.9244310883887089,
    public double debtToAssets; //0.6662076616528654,
    public double netDebtToEBITDA; //34.829130557695485,
    public double currentRatio; //1.5308172815307115,
    public double interestCoverage; //1.0251661179287639,
    public double incomeQuality; //11.040411211884926,
    public double dividendYield; //0.017807292032871905,
    public double payoutRatio; //3.2892203937018647,
    public double salesGeneralAndAdministrativeToRevenue; //0.00988992621199198,
    public double researchAndDdevelopementToRevenue; //0.0,
    public double intangiblesToTotalAssets; //0.055037422249916534,
    public double capexToOperatingCashFlow; //null,
    public double capexToRevenue; //null,
    public double capexToDepreciation; //null,
    public double stockBasedCompensationToRevenue; //0.0,
    public double grahamNumber; //3.882106159406478,
    public double roic; //0.022462532060006767,
    public double returnOnTangibleAssets; //0.002270767664598971,
    public double grahamNetNet; //-8.74773540348993,
    public double workingCapital; //5942612000,
    public double tangibleAssetValue; //9496390000,
    public double netCurrentAssetValue; //-5557983000,
    public double investedCapital; //1.2618203613558474,
    public double averageReceivables; //6658278500,
    public double averagePayables; //6389288000,
    public double averageInventory; //5172117500,
    public double daysSalesOutstanding; //15.550990651876695,
    public double daysPayablesOutstanding; //14.553700470091016,
    public double daysOfInventoryOnHand; //10.343299701355033,
    public double receivablesTurnover; //5.787412648797316,
    public double payablesTurnover; //6.183994248401428,
    public double inventoryTurnover; //8.70128514097,
    public double roe; //0.006699503867742055,
    public double capexPerShare; //0.0

    @Override
    public LocalDate getDate() {
        return date;
    }
}
