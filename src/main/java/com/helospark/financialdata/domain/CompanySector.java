package com.helospark.financialdata.domain;

import java.util.List;

public enum CompanySector {
    FINANCIALS(1, "Financials", List.of("Financial Services", "Financial", "Financials", "Banking", "Insurance")),
    MATERIALS(2, "Materials", List.of("Basic Materials", "Materials", "Paper & Forest", "Chemicals", "Metals & Mining")),
    INDUSTRIALS(3, "Industrials",
            List.of("Industrial Goods", "Industrials", "Transportation Infrastructure", "Road & Rail", "Machinery", "Transportation", "Airlines", "Logistics & Transportation", "Auto Components", "Marine", "Automobiles", "Electrical Equipment",
                    "Aerospace & Defense")),
    UTILITIES(4, "Utilities", List.of("Utilities")),
    COMMUNICATION_SERVICES(5, "Communication services", List.of("Communication Services", "Media", "Telecommunication")),
    TECHNOLOGY(6, "Technology", List.of("Technology", "Semiconductors", "Information Technology")),
    REAL_ESTATE(7, "Real estate", List.of("Real Estate")),
    HEALTHCARE(8, "Healthcare", List.of("Healthcare", "Biotechnology", "Pharmaceuticals")),
    CONSUMER_DEFENSIVE(9, "Consumer defensive", List.of("Consumer Defensive", "Consumer Staples", "Consumer products", "Consumer Services", "Consumer Durables", "Beverages", "Retail", "Food Products")),
    CONSUMER_CYCLICAL(10, "Consumer cyclical",
            List.of("Consumer Cyclical", "Consumer Cyclicals", "Consumer Discretionary", "Consumer Goods", "Textiles, Apparel & Luxury Goods", "Trading Companies & Distributors", "Construction", "Consumer Non-Durables")),
    ENERGY(11, "Energy", List.of("Energy")),
    OTHER(12, "Other", List.of()),
    UNKNOWN(13, "Unknown", List.of());

    int id;
    String name;
    List<String> names;

    CompanySector(int id, String name, List<String> names) {
        this.id = id;
        this.name = name;
        this.names = names;
    }

    public static CompanySector getByProfile(Profile profile) {
        if (profile == null) {
            return UNKNOWN;
        }
        return getByName(profile.sector);
    }

    public static CompanySector getByName(String name) {
        if (name == null || name.isBlank()) {
            return UNKNOWN;
        }
        for (var val : CompanySector.values()) {
            if (val.names.contains(name)) {
                return val;
            }
        }
        return OTHER;
    }

    public static CompanySector valueOf(int id) {
        for (var val : CompanySector.values()) {
            if (val.id == id) {
                return val;
            }
        }
        return UNKNOWN;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
