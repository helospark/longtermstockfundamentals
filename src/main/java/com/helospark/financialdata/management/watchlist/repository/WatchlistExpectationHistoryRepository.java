package com.helospark.financialdata.management.watchlist.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class WatchlistExpectationHistoryRepository {
    public static final String EMAIL_SYMBOL_SEPARATOR = " :: ";
    DynamoDbTable<WatchlistExpectationHistory> table;

    public WatchlistExpectationHistoryRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(
                "WatchlistExpectationHistory",
                TableSchema.fromClass(WatchlistExpectationHistory.class));
    }

    public void save(WatchlistExpectationHistory watchlist) {
        table.putItem(watchlist);
    }

    public Optional<WatchlistExpectationHistory> readWatchlistByEmailAndSymbol(
            String email,
            String symbol) {
        Key key = Key.builder()
                .partitionValue(email + EMAIL_SYMBOL_SEPARATOR + symbol)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }
}
