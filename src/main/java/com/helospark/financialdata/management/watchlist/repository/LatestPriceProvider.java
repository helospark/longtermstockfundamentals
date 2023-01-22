package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.util.StockDataDownloader2;

@Component
public class LatestPriceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LatestPriceProvider.class);
    Cache<String, Double> tickerToPriceCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(40000)
            .build();
    ThreadPoolExecutor threadPoolExec = new ThreadPoolExecutor(10, 30, 10000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(100));

    public double provideLatestPrice(String ticker) {
        return tickerToPriceCache.get(ticker, ticker2 -> provideApiBasedPrice(ticker2));
    }

    public CompletableFuture<Double> provideLatestPriceAsync(String ticker) {
        return CompletableFuture.supplyAsync(() -> tickerToPriceCache.get(ticker, ticker2 -> provideApiBasedPrice(ticker2)), threadPoolExec);
    }

    private double provideApiBasedPrice(String symbol) {
        try {
            return StockDataDownloader2.loadLatestPrice(symbol);
        } catch (Exception e) {
            LOGGER.warn("Unable to download price", e);
            return provideFileBasedPrice(symbol);
        }
    }

    // TODO: load from API: https://financialmodelingprep.com/api/v3/quotes/nyse?apikey=xxx
    // use this only as a backup
    private Double provideFileBasedPrice(String ticker2) {
        List<HistoricalPriceElement> historicalPrice = DataLoader.readHistoricalFile(ticker2, "historical-price.json");

        return historicalPrice.get(0).close;
    }

}
