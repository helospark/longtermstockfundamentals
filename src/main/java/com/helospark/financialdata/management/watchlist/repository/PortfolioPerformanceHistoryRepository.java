package com.helospark.financialdata.management.watchlist.repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Repository
public class PortfolioPerformanceHistoryRepository {
    Cache<String, Optional<PortfolioPerformanceHistory>> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(200)
            .build();

    DynamoDbEnhancedClient enhancedClient;

    public PortfolioPerformanceHistoryRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<PortfolioPerformanceHistory> getTable() {
        return enhancedClient.table(
                "PortfolioPerformanceHistory",
                EnhancedSchemaCache.getSchema(PortfolioPerformanceHistory.class));
    }

    public void save(PortfolioPerformanceHistory data) {
        this.getTable().putItem(data);
        this.cache.invalidate(data.getEmail());
    }

    public void deleteForUser(String user) {
        Key key = Key.builder()
                .partitionValue(user)
                .build();
        this.getTable().deleteItem(key);
        this.cache.invalidate(user);
    }

    public Optional<PortfolioPerformanceHistory> readHistoricalPortfolio(String email) {
        return this.cache.get(email, emailKey -> {
            Key key = Key.builder()
                    .partitionValue(emailKey)
                    .build();
            PortfolioPerformanceHistory item = this.getTable().getItem(key);
            return Optional.ofNullable(item);
        });
    }
}
