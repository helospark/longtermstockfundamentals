package com.helospark.financialdata.management.watchlist.repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class PortfolioPerformanceHistoryRepository {
    Cache<String, Optional<PortfolioPerformanceHistory>> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(200)
            .build();
    DynamoDbTable<PortfolioPerformanceHistory> table;

    public PortfolioPerformanceHistoryRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(
                "PortfolioPerformanceHistory",
                TableSchema.fromClass(PortfolioPerformanceHistory.class));
    }

    public void save(PortfolioPerformanceHistory data) {
        this.table.putItem(data);
        this.cache.invalidate(data.getEmail());
    }

    public void deleteForUser(String user) {
        Key key = Key.builder()
                .partitionValue(user)
                .build();
        this.table.deleteItem(key);
        this.cache.invalidate(user);
    }

    public Optional<PortfolioPerformanceHistory> readHistoricalPortfolio(String email) {
        return this.cache.get(email, emailKey -> {
            Key key = Key.builder()
                    .partitionValue(emailKey)
                    .build();
            PortfolioPerformanceHistory item = this.table.getItem(key);
            return Optional.ofNullable(item);
        });
    }
}
