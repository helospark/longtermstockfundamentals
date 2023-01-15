package com.helospark.financialdata.util.glance;

import java.time.LocalDate;

import com.helospark.financialdata.management.screener.annotation.AtGlanceFormat;
import com.helospark.financialdata.management.screener.annotation.ScreenerElement;

public class AtGlanceData {
    public String companyName;
    public String symbol;
    public double latestStockPrice;
    public double latestStockPriceUsd;
    public long shareCount;
    public LocalDate actualDate;

    @ScreenerElement(name = "Market cap (millions)")
    public double marketCapUsd;
    @ScreenerElement(name = "Trailing PEG")
    public float trailingPeg;
    @ScreenerElement(name = "ROIC", format = AtGlanceFormat.PERCENT)
    public float roic;
    @ScreenerElement(name = "AltmanZ score")
    public float altman;
    @ScreenerElement(name = "Pietrosky score")
    public float pietrosky;

    public double eps;
    public double fcfPerShare;

    @ScreenerElement(name = "PE")
    public float pe;
    @ScreenerElement(name = "Current ratio")
    public float currentRatio;
    @ScreenerElement(name = "Quick ratio")
    public float quickRatio;
    @ScreenerElement(name = "5 year annual EPS growth", format = AtGlanceFormat.PERCENT)
    public float epsGrowth;
    @ScreenerElement(name = "5 year annual FCF growth", format = AtGlanceFormat.PERCENT)
    public float fcfGrowth;
    @ScreenerElement(name = "5 year annual revenue growth", format = AtGlanceFormat.PERCENT)
    public float revenueGrowth;
    @ScreenerElement(name = "5 year annual dividend growth", format = AtGlanceFormat.PERCENT)
    public float dividendGrowthRate;
    @ScreenerElement(name = "5 year annual share count growth", format = AtGlanceFormat.PERCENT)
    public float shareCountGrowth;
    @ScreenerElement(name = "5 year annual net margin growth", format = AtGlanceFormat.PERCENT)
    public float netMarginGrowth;
    @ScreenerElement(name = "Shiller PE")
    public float cape;

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
    @ScreenerElement(name = "Dividend EPS payout ratio", format = AtGlanceFormat.PERCENT)
    public float dividendPayoutRatio;
    @ScreenerElement(name = "Dividend FCF payout ratio", format = AtGlanceFormat.PERCENT)
    public float dividendFcfPayoutRatio;

    @ScreenerElement(name = "Profitable year count")
    public float profitableYears;
    @ScreenerElement(name = "Stock based compensation per market cap", format = AtGlanceFormat.PERCENT)
    public float stockCompensationPerMkt;

    @ScreenerElement(name = "Ideal 10yr revenue growth correlation")
    public float ideal10yrRevCorrelation;
    @ScreenerElement(name = "Ideal 10yr EPS growth correlation")
    public float ideal10yrEpsCorrelation;
    @ScreenerElement(name = "Ideal 10yr FCF growth correlation")
    public float ideal10yrFcfCorrelation;

    @ScreenerElement(name = "Ideal 20yr revenue growth correlation")
    public float ideal20yrRevCorrelation;
    @ScreenerElement(name = "Ideal 20yr EPS growth correlation")
    public float ideal20yrEpsCorrelation;
    @ScreenerElement(name = "Ideal 20yr FCF growth correlation")
    public float ideal20yrFcfCorrelation;

    @ScreenerElement(name = "Default calculator fair value margin of safety")
    public float fvCalculatorMoS;
    @ScreenerElement(name = "Composite fair value margin of safety")
    public float fvCompositeMoS;

    @ScreenerElement(id = "fcf_yield", name = "Free cash flow yield")
    public double getFreeCashFlowYield() {
        return fcfPerShare / latestStockPrice * 100.0;
    }

    @ScreenerElement(id = "earnings_yield", name = "Earnings yield")
    public double getEarningsYield() {
        return eps / latestStockPrice * 100.0;
    }
}
