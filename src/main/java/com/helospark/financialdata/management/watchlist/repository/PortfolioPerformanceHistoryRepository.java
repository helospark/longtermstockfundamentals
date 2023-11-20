package com.helospark.financialdata.management.watchlist.repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Repository
public class PortfolioPerformanceHistoryRepository {
    @Autowired
    DynamoDBMapper mapper;

    Cache<String, Optional<PortfolioPerformanceHistory>> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(200)
            .build();

    public void save(PortfolioPerformanceHistory data) {
        mapper.save(data);
        cache.invalidate(data.getEmail());
    }

    public void deleteForUser(String user) {
        readHistoricalPortfolio(user).ifPresent(a -> mapper.delete(a));
        cache.invalidate(user);
    }

    public Optional<PortfolioPerformanceHistory> readHistoricalPortfolio(String email) {
        return cache.get(email, email2 -> Optional.ofNullable(mapper.load(PortfolioPerformanceHistory.class, email2)));
    }
}
