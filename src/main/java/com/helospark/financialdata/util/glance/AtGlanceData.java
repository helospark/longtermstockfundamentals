package com.helospark.financialdata.util.glance;

import com.helospark.financialdata.management.screener.annotation.AtGlanceFormat;
import com.helospark.financialdata.management.screener.annotation.ScreenerElement;

public class AtGlanceData {
    public String companyName;
    public String symbol;
    public String tradingCurrency;
    public double latestStockPrice;
    public Double latestStockPriceUsd;
    public double shareCount;

    @ScreenerElement(name = "Market cap (billions)")
    public Double marketCap;
    @ScreenerElement(name = "Trailing PEG")
    public Double trailingPeg;
    @ScreenerElement(name = "ROIC", format = AtGlanceFormat.PERCENT)
    public Double roic;
    @ScreenerElement(name = "AltmanZ score")
    public Double altman;
    @ScreenerElement(name = "Pietrosky score")
    public Double pietrosky;
    public Double eps;
    @ScreenerElement(name = "PE")
    public Double pe;
    @ScreenerElement(name = "P/FCF")
    public Double fcfPerShare;
    @ScreenerElement(name = "Current ratio")
    public Double currentRatio;
    @ScreenerElement(name = "Quick ratio")
    public Double quickRatio;
    @ScreenerElement(name = "5 year annual EPS growth", format = AtGlanceFormat.PERCENT)
    public Double epsGrowth;
    @ScreenerElement(name = "5 year annual FCF growth", format = AtGlanceFormat.PERCENT)
    public Double fcfGrowth;
    @ScreenerElement(name = "5 year annual revenue growth", format = AtGlanceFormat.PERCENT)
    public Double revenueGrowth;
    @ScreenerElement(name = "5 year annual dividend growth", format = AtGlanceFormat.PERCENT)
    public Double dividendGrowthRate;
    @ScreenerElement(name = "5 year annual share count growth", format = AtGlanceFormat.PERCENT)
    public Double shareCountGrowth;
    @ScreenerElement(name = "5 year annual net margin growth", format = AtGlanceFormat.PERCENT)
    public Double netMarginGrowth;
    @ScreenerElement(name = "Shiller PE")
    public Double cape;

    @ScreenerElement(name = "EPS standard deviation")
    public Double epsSD;
    @ScreenerElement(name = "Revenue standard deviation")
    public Double revSD;
    @ScreenerElement(name = "FCF standard deviation")
    public Double fcfSD;
    @ScreenerElement(name = "EPS FCF correlation")
    public Double epsFcfCorrelation;

    @ScreenerElement(name = "Dividend yield", format = AtGlanceFormat.PERCENT)
    public Double dividendYield;
    @ScreenerElement(name = "Dividend EPS payout ratio", format = AtGlanceFormat.PERCENT)
    public Double dividendPayoutRatio;
    @ScreenerElement(name = "Dividend FCF payout ratio", format = AtGlanceFormat.PERCENT)
    public Double dividendFcfPayoutRatio;

    @ScreenerElement(name = "Profitable year count")
    public Double profitableYears;
    @ScreenerElement(name = "Stock based compensation per market cap", format = AtGlanceFormat.PERCENT)
    public Double stockCompensationPerMkt;

}
