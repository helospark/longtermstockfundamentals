package com.helospark.financialdata.util.glance;

import java.time.LocalDate;

import com.helospark.financialdata.management.screener.annotation.AtGlanceFormat;
import com.helospark.financialdata.management.screener.annotation.ScreenerElement;

public class AtGlanceData {
    public String companyName;
    public String symbol;
    public String tradingCurrency;
    public double latestStockPrice;
    public double latestStockPriceUsd;
    public double shareCount;
    public LocalDate actualDate;

    @ScreenerElement(name = "Market cap (millions)")
    public double marketCapUsd;
    @ScreenerElement(name = "Trailing PEG")
    public double trailingPeg;
    @ScreenerElement(name = "ROIC", format = AtGlanceFormat.PERCENT)
    public double roic;
    @ScreenerElement(name = "AltmanZ score")
    public double altman;
    @ScreenerElement(name = "Pietrosky score")
    public double pietrosky;
    public double eps;
    @ScreenerElement(name = "PE")
    public double pe;
    @ScreenerElement(name = "FCF/share")
    public double fcfPerShare;
    @ScreenerElement(name = "Current ratio")
    public double currentRatio;
    @ScreenerElement(name = "Quick ratio")
    public double quickRatio;
    @ScreenerElement(name = "5 year annual EPS growth", format = AtGlanceFormat.PERCENT)
    public double epsGrowth;
    @ScreenerElement(name = "5 year annual FCF growth", format = AtGlanceFormat.PERCENT)
    public double fcfGrowth;
    @ScreenerElement(name = "5 year annual revenue growth", format = AtGlanceFormat.PERCENT)
    public double revenueGrowth;
    @ScreenerElement(name = "5 year annual dividend growth", format = AtGlanceFormat.PERCENT)
    public double dividendGrowthRate;
    @ScreenerElement(name = "5 year annual share count growth", format = AtGlanceFormat.PERCENT)
    public double shareCountGrowth;
    @ScreenerElement(name = "5 year annual net margin growth", format = AtGlanceFormat.PERCENT)
    public double netMarginGrowth;
    @ScreenerElement(name = "Shiller PE")
    public double cape;

    @ScreenerElement(name = "EPS standard deviation")
    public double epsSD;
    @ScreenerElement(name = "Revenue standard deviation")
    public double revSD;
    @ScreenerElement(name = "FCF standard deviation")
    public double fcfSD;
    @ScreenerElement(name = "EPS FCF correlation")
    public double epsFcfCorrelation;

    @ScreenerElement(name = "Dividend yield", format = AtGlanceFormat.PERCENT)
    public double dividendYield;
    @ScreenerElement(name = "Dividend EPS payout ratio", format = AtGlanceFormat.PERCENT)
    public double dividendPayoutRatio;
    @ScreenerElement(name = "Dividend FCF payout ratio", format = AtGlanceFormat.PERCENT)
    public double dividendFcfPayoutRatio;

    @ScreenerElement(name = "Profitable year count")
    public double profitableYears;
    @ScreenerElement(name = "Stock based compensation per market cap", format = AtGlanceFormat.PERCENT)
    public double stockCompensationPerMkt;

    @ScreenerElement(id = "fcf_yield", name = "Free cash flow yield")
    public double getFreeCashFlowYield() {
        return fcfPerShare / latestStockPrice * 100.0;
    }

    @ScreenerElement(id = "earnings_yield", name = "Earnings yield")
    public double getEarningsYield() {
        return eps / latestStockPrice * 100.0;
    }
}
