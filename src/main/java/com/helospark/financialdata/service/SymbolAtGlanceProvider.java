package com.helospark.financialdata.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.domain.FxRatesResponse;
import com.helospark.financialdata.domain.SearchElement;
import com.helospark.financialdata.util.StockDataDownloader;
import com.helospark.financialdata.util.StockDataDownloader.YearMonthPair;
import com.helospark.financialdata.util.glance.AtGlanceData;

@Component
public class SymbolAtGlanceProvider {
    List<String> symbols = new ArrayList<>();
    List<String> companyNames = new ArrayList<>();
    List<String> upperCompanyNames = new ArrayList<>();

    int longestCompanyName;
    int longestSymbol;

    LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache;

    Cache<YearMonthPair, Optional<Map<String, AtGlanceData>>> cache = Caffeine.newBuilder()
            .expireAfterWrite(100, TimeUnit.DAYS)
            .maximumSize(500)
            .build();

    public SymbolAtGlanceProvider() {
        initCache();
    }

    public void initCache() {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.registerModule(new JSR310Module());
        File file = new File(StockDataDownloader.SYMBOL_CACHE_FILE);

        Kryo kryo = new Kryo();
        kryo.register(AtGlanceData.class);
        kryo.register(LinkedHashMap.class);
        kryo.register(LocalDate.class);
        kryo.setDefaultSerializer(VersionFieldSerializer.class);

        try {
            Input input = new Input(new FileInputStream(file));
            symbolCompanyNameCache = kryo.readObject(input, LinkedHashMap.class);
            input.close();

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
        Set<SearchElement> result = new LinkedHashSet<>();
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
        return result.stream().toList();
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
        if (stock.startsWith("CASH.")) {
            String currency = stock.replace("CASH.", "");
            Optional<FxRatesResponse> fxFile = DataLoader.loadFxFile(currency, LocalDate.now());
            if (fxFile.isPresent()) {
                return Optional.of(null);
            } else {
                return createCurrencyAtGlance(currency);
            }

        }
        return Optional.ofNullable(symbolCompanyNameCache.get(stock));
    }

    private Optional<AtGlanceData> createCurrencyAtGlance(String currency) {
        AtGlanceData data = new AtGlanceData();

        for (var field : data.getClass().getDeclaredFields()) {
            if (field.getType().equals(float.class)) {
                try {
                    field.set(data, Float.NaN);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        data.companyName = currency;
        data.symbol = currency;
        data.latestStockPrice = 1.0;
        data.latestStockPriceTradingCur = 1.0;
        data.latestStockPriceUsd = DataLoader.convertFx(1.0, currency, "USD", LocalDate.now(), false).orElse(0.0);
        data.price5Gr = 0.0f;
        data.price10Gr = 0.0f;
        data.price15Gr = 0.0f;
        data.price20Gr = 0.0f;
        return Optional.of(data);
    }

    public LinkedHashMap<String, AtGlanceData> getSymbolCompanyNameCache() {
        return symbolCompanyNameCache;
    }

    public Optional<Map<String, AtGlanceData>> loadAtGlanceDataAtYear(int year, int month) {
        return cache.get(YearMonthPair.of(year, month), y -> DataLoader.loadHistoricalAtGlanceData(year, month));
    }

    public boolean doesCompanyExists(String stock) {
        if (stock == null) {
            return false;
        }
        if (stock.startsWith("CASH.")) {
            String currency = stock.replace("CASH.", "");
            return DataLoader.isValidCurrency(currency);
        }
        for (int i = 0; i < symbols.size(); ++i) {
            if (symbols.get(i).equalsIgnoreCase(stock)) {
                return true;
            }
        }
        return false;
    }

}
