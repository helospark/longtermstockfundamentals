package com.helospark.financialdata.service.exchanges;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum Exchanges {
    NASDAQ("NASDAQ Global Select", ExchangeRegion.US),
    NYSE("New York Stock Exchange", ExchangeRegion.US),
    HKSE("Hong Kong Stock Exchange", ExchangeRegion.CHINA),
    MCX("MCX", ExchangeRegion.INDIA),
    NSE("National Stock Exchange of India", ExchangeRegion.INDIA),
    TAI("Taiwan", ExchangeRegion.TAIWAN),
    TWO("Taipei Exchange", ExchangeRegion.TAIWAN),
    JPX("Tokyo", ExchangeRegion.JAPAN),
    LSE("London Stock Exchange", ExchangeRegion.EUROPE),
    ASX("Australian Securities Exchange", ExchangeRegion.OCEANIA),
    XETRA("XETRA", ExchangeRegion.EUROPE),
    BUD("Budapest", ExchangeRegion.EUROPE),
    STO("Stockholm Stock Exchange", ExchangeRegion.EUROPE),
    SHH("Shanghai", ExchangeRegion.CHINA),
    KLS("Kuala Lumpur", ExchangeRegion.ASIA_SOUTH_EAST),
    YHD("YHD", ExchangeRegion.UNKNOWN),
    BSE("BSE", ExchangeRegion.INDIA),
    DOH("Qatar", ExchangeRegion.ASIA_NORTH_WEST),
    FKA("Fukuoka", ExchangeRegion.JAPAN),
    SHZ("Shenzhen", ExchangeRegion.CHINA),
    AMS("Amsterdam", ExchangeRegion.EUROPE),
    MUN("Munich", ExchangeRegion.EUROPE),
    MIL("Milan", ExchangeRegion.EUROPE),
    JKT("Jakarta Stock Exchange", ExchangeRegion.ASIA_SOUTH_EAST),
    TSX("Toronto Stock Exchange", ExchangeRegion.CANADA),
    AMEX("New York Stock Exchange Arca", ExchangeRegion.US),
    MEX("Mexico", ExchangeRegion.LATIN_AMERICA),
    SIX("Swiss Exchange", ExchangeRegion.EUROPE),
    SET("Thailand", ExchangeRegion.ASIA_SOUTH_EAST),
    SAU("Saudi", ExchangeRegion.ASIA_NORTH_WEST),
    BUE("Buenos Aires", ExchangeRegion.LATIN_AMERICA),
    KSC("Pakistan stock exchange", ExchangeRegion.ASIA_NORTH_WEST),
    IOB("IOB", ExchangeRegion.EUROPE),
    TLV("Tel Aviv", ExchangeRegion.ISRAEL),
    OSE("Oslo Stock Exchange", ExchangeRegion.EUROPE),
    DUS("Dusseldorf", ExchangeRegion.EUROPE),
    WSE("Warsaw Stock Exchange", ExchangeRegion.EUROPE),
    ICE("Iceland", ExchangeRegion.EUROPE),
    IST("Istanbul Stock Exchange", ExchangeRegion.TURKEY),
    EURONEXT("Paris", ExchangeRegion.EUROPE),
    HEL("Helsinki", ExchangeRegion.EUROPE),
    BER("Berlin", ExchangeRegion.EUROPE),
    VIE("Vienna", ExchangeRegion.EUROPE),
    HAM("Hamburg", ExchangeRegion.EUROPE),
    ETF("BATS", ExchangeRegion.US),
    LIS("Lisbon", ExchangeRegion.EUROPE),
    CPH("Copenhagen", ExchangeRegion.EUROPE),
    SGO("Santiago", ExchangeRegion.LATIN_AMERICA),
    PRA("Prague", ExchangeRegion.EUROPE),
    TAL("Tallinn", ExchangeRegion.EUROPE),
    OTC("Other OTC", ExchangeRegion.UNKNOWN),
    JNB("Johannesburg", ExchangeRegion.AFRICA_SOUTH),
    UNKNOWN("Unknown exchange", ExchangeRegion.UNKNOWN);

    String name;
    ExchangeRegion region;

    Exchanges(String name, ExchangeRegion region) {
        this.name = name;
        this.region = region;
    }

    public static Set<Exchanges> getExchangesByRegion(Set<ExchangeRegion> regions) {
        return Arrays.stream(Exchanges.values())
                .filter(a -> regions.contains(a.region))
                .collect(Collectors.toSet());
    }

    public static Set<Exchanges> getExchangesByRegion(ExchangeRegion region) {
        return getExchangesByRegion(Set.of(region));
    }

    public static Set<Exchanges> getExchangesByType(Set<MarketType> types) {
        return Arrays.stream(Exchanges.values())
                .filter(a -> types.contains(a.region.marketType))
                .collect(Collectors.toSet());
    }

    public static Set<Exchanges> getExchangesByType(MarketType type) {
        return getExchangesByType(Set.of(type));
    }

    public String getName() {
        return name;
    }

    public static Exchanges fromString(String exchange) {
        for (var value : values()) {
            if (value.name().equals(exchange)) {
                return value;
            }
        }
        throw new RuntimeException("No such exchange");
    }

    public static Map<String, String> getIdToNameMap() {
        Map<String, String> result = new LinkedHashMap<>();
        for (var element : Exchanges.values()) {
            result.put(element.name(), element.getName());
        }
        return result;
    }
}
