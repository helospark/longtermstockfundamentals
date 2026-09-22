package com.helospark.financialdata.management.screener.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class ScreenerRepository {
    DynamoDbTable<Screener> table;

    public ScreenerRepository(DynamoDbEnhancedClient enhancedClient) {
        table = enhancedClient.table(
                "Screener",
                TableSchema.fromClass(Screener.class));
    }

    public void save(Screener watchlist) {
        table.putItem(watchlist);
    }

    public Optional<Screener> readScreenerByEmail(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }
}