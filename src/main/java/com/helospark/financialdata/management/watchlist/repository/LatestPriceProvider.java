package com.helospark.financialdata.management.watchlist.repository;

import java.io.File;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Policy.VarExpiration;
import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.util.StockDataDownloader2;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class LatestPriceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LatestPriceProvider.class);
    private static final long CACHE_STORE_TIME = 60 * 60 * 24;
    private static final long CACHE_STORE_VARIATION = 60 * 60 * 4;

    Cache<String, Double> tickerToPriceCache = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, Double>() {
                @Override
                public long expireAfterCreate(String key, Double value, long currentTime) {
                    long seconds = CACHE_STORE_TIME + new Random().nextLong(CACHE_STORE_VARIATION);
                    return TimeUnit.SECONDS.toNanos(seconds);
                }

                @Override
                public long expireAfterUpdate(String key, Double value,
                        long currentTime, long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(String key, Double value,
                        long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .maximumSize(40000)
            .build();
    ThreadPoolExecutor threadPoolExec = new ThreadPoolExecutor(10, 30, 10000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10000));

    @Autowired
    private ObjectMapper objectMapper;
    @Value("${diskcache.enabled}")
    private boolean diskCacheEnabled;
    @Value("${diskcache.location}")
    private String diskCacheLocation;

    VarExpiration<String, Double> variableExpiry;
    private boolean cacheChanged;

    @PostConstruct
    private void init() {
        if (diskCacheEnabled) {
            variableExpiry = tickerToPriceCache.policy().expireVariably().get();
            File file = new File(diskCacheLocation);
            Map<String, DoubleCacheEntry> diskCache = new HashMap<>();
            if (file.exists()) {
                try {
                    diskCache = objectMapper.readValue(file, new TypeReference<Map<String, DoubleCacheEntry>>() {
                    });
                } catch (Exception e) {
                    LOGGER.error("Unable to read cache", e);
                }
                long currentDate = new Date().getTime();
                for (var element : diskCache.entrySet()) {
                    if (currentDate < element.getValue().expiry) {
                        tickerToPriceCache.put(element.getKey(), element.getValue().value);
                        variableExpiry.setExpiresAfter(element.getKey(), Duration.ofNanos(element.getValue().expiry));
                    }
                }
            }
        }
    }

    @PreDestroy
    public void destroy() {
        if (diskCacheEnabled && cacheChanged) {
            writeCacheToFile();
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void scheduledWriteCache() {
        if (diskCacheEnabled && cacheChanged) {
            writeCacheToFile();
        }
        cacheChanged = false;
    }

    private void writeCacheToFile() {
        try {
            Map<String, DoubleCacheEntry> diskCache = new HashMap<>();
            Set<String> keys = tickerToPriceCache.asMap().keySet();
            for (var key : keys) {
                Double value = tickerToPriceCache.getIfPresent(key);
                Optional<Duration> expiry = variableExpiry.getExpiresAfter(key);
                if (expiry.isPresent() && value != null) {
                    long expiryNanos = expiry.get().toNanos();
                    diskCache.put(key, new DoubleCacheEntry(value, expiryNanos));
                }
            }
            File file = new File(diskCacheLocation);
            objectMapper.writeValue(file, diskCache);
        } catch (Exception e) {
            LOGGER.error("Unable to write cache", e);
        }
    }

    public double provideLatestPrice(String ticker) {
        return tickerToPriceCache.get(ticker, ticker2 -> provideApiBasedPrice(ticker2));
    }

    public CompletableFuture<Double> provideLatestPriceAsync(String ticker) {
        return CompletableFuture.supplyAsync(() -> tickerToPriceCache.get(ticker, ticker2 -> provideApiBasedPrice(ticker2)), threadPoolExec);
    }

    private double provideApiBasedPrice(String symbol) {
        try {
            double result = StockDataDownloader2.loadLatestPrice(symbol);
            if (result == 0.0) {
                result = provideFileBasedPrice(symbol);
            }
            cacheChanged = true;

            return result;
        } catch (Exception e) {
            LOGGER.warn("Unable to download price", e);
            return provideFileBasedPrice(symbol);
        }
    }

    private Double provideFileBasedPrice(String ticker2) {
        List<HistoricalPriceElement> historicalPrice = DataLoader.readHistoricalFile(ticker2, "historical-price.json");

        return historicalPrice.get(0).close;
    }

    static class DoubleCacheEntry {
        double value;
        long expiry;

        public DoubleCacheEntry() {
        }

        public DoubleCacheEntry(double value, long expiry) {
            this.value = value;
            this.expiry = expiry;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public long getExpiry() {
            return expiry;
        }

        public void setExpiry(long expiry) {
            this.expiry = expiry;
        }

    }

}
