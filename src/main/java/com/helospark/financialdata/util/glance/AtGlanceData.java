package com.helospark.financialdata.util.glance;

import java.time.LocalDate;

import com.helospark.financialdata.management.screener.annotation.AtGlanceFormat;
import com.helospark.financialdata.management.screener.annotation.ScreenerElement;

public class AtGlanceData {
    public String companyName;
    public String symbol;
    public double latestStockPrice;
    public double latestStockPriceTradingCur;
    public double latestStockPriceUsd;
    public long shareCount;
    public LocalDate actualDate;
    public float dividendPaid;

    @ScreenerElement(name = "Market cap ($ million)", format = AtGlanceFormat.MILLION_DOLLAR)
    public double marketCapUsd;
    @ScreenerElement(name = "Trailing PEG")
    public float trailingPeg;
    @ScreenerElement(name = "ROIC", format = AtGlanceFormat.PERCENT)
    public float roic;
    @ScreenerElement(name = "AltmanZ score")
    public float altman;
    @ScreenerElement(name = "Piotrosky score")
    public float pietrosky;

    public double eps;
    public double fcfPerShare;

    @ScreenerElement(name = "PE")
    public float pe;
    @ScreenerElement(name = "Shiller PE")
    public float cape;
    @ScreenerElement(name = "Enterprise value to EBIT")
    public float evToEbitda;
    @ScreenerElement(name = "Price to book ratio")
    public float ptb;
    @ScreenerElement(name = "Price to sales ratio")
    public float pts;
    @ScreenerElement(name = "Price to gross profit ratio")
    public float priceToGrossProfit;
    @ScreenerElement(name = "Interest coverage ratio")
    public float icr;
    @ScreenerElement(name = "Current ratio")
    public float currentRatio;
    @ScreenerElement(name = "Quick ratio")
    public float quickRatio;
    @ScreenerElement(name = "Debt to equity ratio")
    public float dtoe;
    @ScreenerElement(name = "Asset turnover ratio")
    public float assetTurnoverRatio;

    @ScreenerElement(name = "Return on equity", format = AtGlanceFormat.PERCENT)
    public float roe;
    @ScreenerElement(name = "Return on assets", format = AtGlanceFormat.PERCENT)
    public float roa;
    @ScreenerElement(name = "Return on tangible assets", format = AtGlanceFormat.PERCENT)
    public float rota;

    @ScreenerElement(name = "5 year annual EPS growth", format = AtGlanceFormat.PERCENT)
    public float epsGrowth;
    @ScreenerElement(name = "5 year annual FCF growth", format = AtGlanceFormat.PERCENT)
    public float fcfGrowth;
    @ScreenerElement(name = "5 year annual revenue growth", format = AtGlanceFormat.PERCENT)
    public float revenueGrowth;
    @ScreenerElement(name = "5 year net income growth", format = AtGlanceFormat.PERCENT)
    public float fYrIncomeGr;
    @ScreenerElement(name = "5 year annual dividend growth", format = AtGlanceFormat.PERCENT)
    public float dividendGrowthRate;
    @ScreenerElement(name = "5 year annual share count growth", format = AtGlanceFormat.PERCENT)
    public float shareCountGrowth;
    @ScreenerElement(name = "5 year annual net margin growth", format = AtGlanceFormat.PERCENT)
    public float netMarginGrowth;
    @ScreenerElement(name = "5 year equity per share growth", format = AtGlanceFormat.PERCENT)
    public float equityGrowth;

    @ScreenerElement(name = "2 year annual EPS growth", format = AtGlanceFormat.PERCENT)
    public float epsGrowth2yr;
    @ScreenerElement(name = "2 year annual FCF growth", format = AtGlanceFormat.PERCENT)
    public float fcfGrowth2yr;
    @ScreenerElement(name = "2 year annual revenue growth", format = AtGlanceFormat.PERCENT)
    public float revenueGrowth2yr;
    @ScreenerElement(name = "2 year annual share count growth", format = AtGlanceFormat.PERCENT)
    public float shareCountGrowth2yr;
    @ScreenerElement(name = "2 year equity per share growth", format = AtGlanceFormat.PERCENT)
    public float equityGrowth2yr;

    @ScreenerElement(name = "EPS standard deviation")
    public float epsSD;
    @ScreenerElement(name = "Revenue standard deviation")
    public float revSD;
    @ScreenerElement(name = "FCF standard deviation")
    public float fcfSD;
    @ScreenerElement(name = "EPS FCF correlation")
    public float epsFcfCorrelation;

    @ScreenerElement(name = "Dividend yield", format = AtGlanceFormat.PERCENT)
    public float dividendYield;
    @ScreenerElement(name = "Dividend net income payout ratio", format = AtGlanceFormat.PERCENT)
    public float dividendPayoutRatio;
    @ScreenerElement(name = "Dividend FCF payout ratio", format = AtGlanceFormat.PERCENT)
    public float dividendFcfPayoutRatio;

    @ScreenerElement(name = "Total payout ratio", format = AtGlanceFormat.PERCENT)
    public float tpr;

    @ScreenerElement(name = "Profitable year count")
    public short profitableYears;
    @ScreenerElement(name = "FCF profitable year count")
    public short fcfProfitableYears;
    @ScreenerElement(name = "Stock based compensation per market cap", format = AtGlanceFormat.PERCENT)
    public float stockCompensationPerMkt;
    @ScreenerElement(name = "CAPEX to revenue percent", format = AtGlanceFormat.PERCENT)
    public float cpxToRev;
    @ScreenerElement(name = "Sloan ratio")
    public float sloan;

    @ScreenerElement(name = "Ideal 10yr revenue growth correlation")
    public float ideal10yrRevCorrelation;
    @ScreenerElement(name = "Ideal 10yr EPS growth correlation")
    public float ideal10yrEpsCorrelation;
    @ScreenerElement(name = "Ideal 10yr FCF growth correlation")
    public float ideal10yrFcfCorrelation;

    /*
    @ScreenerElement(name = "Ideal 20yr revenue growth correlation")
    public float ideal20yrRevCorrelation;
    @ScreenerElement(name = "Ideal 20yr EPS growth correlation")
    public float ideal20yrEpsCorrelation;
    @ScreenerElement(name = "Ideal 20yr FCF growth correlation")
    public float ideal20yrFcfCorrelation;*/

    @ScreenerElement(name = "Default calculator fair value margin of safety", format = AtGlanceFormat.PERCENT)
    public float fvCalculatorMoS;
    @ScreenerElement(name = "Composite fair value margin of safety", format = AtGlanceFormat.PERCENT)
    public float fvCompositeMoS;
    @ScreenerElement(name = "Graham number margin of safety", format = AtGlanceFormat.PERCENT)
    public float grahamMoS;
    @ScreenerElement(name = "Flag count (star)")
    public byte starFlags;
    @ScreenerElement(name = "Flag count (red)")
    public byte redFlags;
    @ScreenerElement(name = "Flag count (yellow)")
    public byte yellowFlags;
    @ScreenerElement(name = "Flag count (green)")
    public byte greenFlags;

    public int dataQualityIssue;

    // EM
    @ScreenerElement(name = "5 year PE")
    public float fYrPe;
    @ScreenerElement(name = "5 year P/FCF")
    public float fYrPFcf;
    @ScreenerElement(name = "5 year ROIC", format = AtGlanceFormat.PERCENT)
    public float fiveYrRoic;
    @ScreenerElement(name = "LTL / 5 yr FCF")
    public float ltl5Fcf;
    @ScreenerElement(name = "5 years annual returns", format = AtGlanceFormat.PERCENT)
    public float price5Gr;
    @ScreenerElement(name = "10 years annual returns", format = AtGlanceFormat.PERCENT)
    public float price10Gr;
    @ScreenerElement(name = "15 years annual returns", format = AtGlanceFormat.PERCENT)
    public float price15Gr;
    @ScreenerElement(name = "20 years annual returns", format = AtGlanceFormat.PERCENT)
    public float price20Gr;
    @ScreenerElement(name = "Gross margin", format = AtGlanceFormat.PERCENT)
    public float grMargin;
    @ScreenerElement(name = "Operating margin", format = AtGlanceFormat.PERCENT)
    public float opMargin;
    @ScreenerElement(name = "Operating cashflow margin", format = AtGlanceFormat.PERCENT)
    public float opCMargin;
    @ScreenerElement(name = "FCF margin", format = AtGlanceFormat.PERCENT)
    public float fcfMargin;
    @ScreenerElement(name = "EBITDA margin", format = AtGlanceFormat.PERCENT)
    public float ebitdaMargin;

    @ScreenerElement(name = "Investment score")
    public float investmentScore;

    @ScreenerElement(id = "fcf_yield", name = "Free cash flow yield", format = AtGlanceFormat.PERCENT)
    public double getFreeCashFlowYield() {
        return fcfPerShare / latestStockPrice * 100.0;
    }

    @ScreenerElement(id = "earnings_yield", name = "Earnings yield", format = AtGlanceFormat.PERCENT)
    public double getEarningsYield() {
        return eps / latestStockPrice * 100.0;
    }
}
