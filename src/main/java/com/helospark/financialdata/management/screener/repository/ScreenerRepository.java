package com.helospark.financialdata.management.screener.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Repository
public class ScreenerRepository {
    DynamoDbEnhancedClient enhancedClient;

    public ScreenerRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<Screener> getTable() {
        return enhancedClient.table(
                "Screener",
                EnhancedSchemaCache.getSchema(Screener.class));
    }

    public void save(Screener watchlist) {
        getTable().putItem(watchlist);
    }

    public Optional<Screener> readScreenerByEmail(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        return Optional.ofNullable(getTable().getItem(key));
    }
}