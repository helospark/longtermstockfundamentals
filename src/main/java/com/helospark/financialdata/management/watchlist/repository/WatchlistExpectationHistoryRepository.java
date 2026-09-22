package com.helospark.financialdata.management.watchlist.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Repository
public class WatchlistExpectationHistoryRepository {
    public static final String EMAIL_SYMBOL_SEPARATOR = " :: ";
    DynamoDbEnhancedClient enhancedClient;

    public WatchlistExpectationHistoryRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<WatchlistExpectationHistory> getTable() {
        return enhancedClient.table(
                "WatchlistExpectationHistory",
                EnhancedSchemaCache.getSchema(WatchlistExpectationHistory.class));
    }

    public void save(WatchlistExpectationHistory watchlist) {
        getTable().putItem(watchlist);
    }

    public Optional<WatchlistExpectationHistory> readWatchlistByEmailAndSymbol(
            String email,
            String symbol) {
        Key key = Key.builder()
                .partitionValue(email + EMAIL_SYMBOL_SEPARATOR + symbol)
                .build();

        return Optional.ofNullable(getTable().getItem(key));
    }
}
