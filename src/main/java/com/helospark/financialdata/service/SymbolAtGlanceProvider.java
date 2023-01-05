package com.helospark.financialdata.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.domain.SearchElement;
import com.helospark.financialdata.util.StockDataDownloader;
import com.helospark.financialdata.util.glance.AtGlanceData;

@Component
public class SymbolAtGlanceProvider {
    List<String> symbols = new ArrayList<>();
    List<String> companyNames = new ArrayList<>();
    List<String> upperCompanyNames = new ArrayList<>();

    int longestCompanyName;
    int longestSymbol;

    LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache;

    Cache<Integer, Optional<Map<String, AtGlanceData>>> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.DAYS)
            .maximumSize(100)
            .build();

    public SymbolAtGlanceProvider() {
        ObjectMapper om = new ObjectMapper();
        File file = new File(StockDataDownloader.SYMBOL_CACHE_FILE);

        TypeReference<LinkedHashMap<String, AtGlanceData>> typeRef = new TypeReference<LinkedHashMap<String, AtGlanceData>>() {
        };

        try {
            symbolCompanyNameCache = om.readValue(file, typeRef);

            for (var entry : symbolCompanyNameCache.entrySet()) {
                if (entry.getKey().length() > longestSymbol) {
                    longestSymbol = entry.getKey().length();
                }
                symbols.add(entry.getKey());
                if (entry.getValue().companyName != null) {
                    companyNames.add(entry.getValue().companyName);
                    upperCompanyNames.add(entry.getValue().companyName.toUpperCase());

                    if (entry.getValue().companyName.length() > longestCompanyName) {
                        longestCompanyName = entry.getValue().companyName.length();
                    }
                } else {
                    companyNames.add("");
                    upperCompanyNames.add("");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<SearchElement> getTopResult(String parts) {
        if (parts.length() > longestCompanyName || parts.isBlank()) {
            return List.of();
        }
        List<SearchElement> result = new ArrayList<>();
        String partsUppercase = parts.toUpperCase();

        List<SearchElement> bestSymbols = new ArrayList<>();
        if (parts.length() <= longestSymbol) {
            for (int i = 0; i < symbols.size(); ++i) {
                var element = symbols.get(i);
                if (element.equals(partsUppercase)) {
                    bestSymbols.add(0, new SearchElement(element, companyNames.get(i)));
                } else if (element.startsWith(partsUppercase)) {
                    bestSymbols.add(new SearchElement(element, companyNames.get(i)));
                }

            }

            for (int i = 0; i < 3 && i < bestSymbols.size(); ++i) {
                result.add(bestSymbols.get(i));
            }
        }
        if (parts.length() > 1) {
            for (int i = 0; i < companyNames.size(); ++i) {
                SearchElement asd = new SearchElement(symbols.get(i), companyNames.get(i));
                if (upperCompanyNames.get(i).contains(partsUppercase) && !result.contains(asd)) {
                    result.add(asd);
                }
                if (result.size() > 6) {
                    break;
                }
            }
        }
        return result;
    }

    public Optional<String> getCompanyName(String stock) {
        for (int i = 0; i < symbols.size(); ++i) {
            if (symbols.get(i).equalsIgnoreCase(stock) && !companyNames.get(i).isBlank()) {
                return Optional.of(companyNames.get(i));
            }
        }
        return Optional.empty();
    }

    public Optional<AtGlanceData> getAtGlanceData(String stock) {
        return Optional.ofNullable(symbolCompanyNameCache.get(stock));
    }

    public LinkedHashMap<String, AtGlanceData> getSymbolCompanyNameCache() {
        return symbolCompanyNameCache;
    }

    public Optional<Map<String, AtGlanceData>> loadAtGlanceDataAtYear(int year) {
        return cache.get(year, y -> DataLoader.loadHistoricalAtGlanceData(year).map(a -> fixSymbols(a)));
    }

    private Map<String, AtGlanceData> fixSymbols(Map<String, List<AtGlanceData>> toFix) {
        Map<String, AtGlanceData> result = new LinkedHashMap<>();
        for (var entry : toFix.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get(0));
            entry.getValue().get(0).symbol = entry.getKey();
        }
        return result;
    }

}
