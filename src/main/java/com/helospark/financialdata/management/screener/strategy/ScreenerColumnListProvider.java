package com.helospark.financialdata.management.screener.strategy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.helospark.financialdata.domain.CompanySector;

public class ScreenerColumnListProvider {
    public static final String SECTOR_MAPPING = "sectorMapping";

    public static Map<String, Map<String, Integer>> cache;
    public static Map<String, Map<Integer, String>> cacheInverted;

    static {
        cache = new HashMap<>();
        cache.put(SECTOR_MAPPING, createSectorMapping());

        cacheInverted = cache.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        outerEntry -> outerEntry.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getValue,
                                        Map.Entry::getKey,
                                        (existing, replacement) -> existing // Merge rule: handles duplicate inner values safely
                                ))));
    }

    public static Map<String, Map<String, Integer>> provideValues() {
        return cache;
    }

    public static Map<String, Map<Integer, String>> provideValuesInverted() {
        return cacheInverted;
    }

    private static Map<String, Integer> createSectorMapping() {
        return Arrays.stream(CompanySector.values())
                .collect(Collectors.toMap(a -> a.getName(), a -> a.getId()));
    }

}
