package com.helospark.financialdata.service.exchanges;

public enum ExchangeRegion {
    CANADA(MarketType.DEVELOPED_MARKET),
    LATIN_AMERICA(MarketType.EMERGING_MARKET),
    EUROPE(MarketType.DEVELOPED_MARKET),
    AFRICA_SOUTH(MarketType.DEVELOPING_MARKET),
    INDIA(MarketType.EMERGING_MARKET),
    CHINA(MarketType.EMERGING_MARKET),
    US(MarketType.DEVELOPED_MARKET),
    TAIWAN(MarketType.DEVELOPED_MARKET),
    ASIA_SOUTH_EAST(MarketType.DEVELOPING_MARKET),
    ASIA_NORTH_WEST(MarketType.DEVELOPING_MARKET),
    UNKNOWN(MarketType.DEVELOPING_MARKET),
    JAPAN(MarketType.DEVELOPED_MARKET),
    ISRAEL(MarketType.DEVELOPED_MARKET),
    TURKEY(MarketType.EMERGING_MARKET);

    MarketType marketType;

    ExchangeRegion(MarketType marketType) {
        this.marketType = marketType;
    }
}
