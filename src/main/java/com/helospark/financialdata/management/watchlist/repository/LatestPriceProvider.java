package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.service.DataLoader;

@Component
public class LatestPriceProvider {
    Cache<String, Double> tickerToPriceCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(40000)
            .build();

    public double provideLatestPrice(String ticker) {
        return tickerToPriceCache.get(ticker, ticker2 -> provideFileBasedPrice(ticker2));
    }

    // TODO: load from API: https://financialmodelingprep.com/api/v3/quotes/nyse?apikey=xxx
    // use this only as a backup
    private Double provideFileBasedPrice(String ticker2) {
        List<HistoricalPriceElement> historicalPrice = DataLoader.readHistoricalFile(ticker2, "historical-price.json");

        return historicalPrice.get(0).close;
    }

}
