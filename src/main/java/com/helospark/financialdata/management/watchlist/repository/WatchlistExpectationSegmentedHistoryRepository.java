package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class WatchlistExpectationSegmentedHistoryRepository {
    public static final String EMAIL_SYMBOL_SEPARATOR = " :: ";
    DynamoDbTable<WatchlistExpectationHistoryElement> table;

    public WatchlistExpectationSegmentedHistoryRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(
                "WatchlistExpectationHistorySegmented",
                TableSchema.fromClass(WatchlistExpectationHistoryElement.class));
    }

    public void save(WatchlistExpectationHistoryElement watchlist) {
        table.putItem(watchlist);
    }

    public void delete(String email, String symbol, String date) {
        Key key = Key.builder()
                .partitionValue(email + EMAIL_SYMBOL_SEPARATOR + symbol)
                .addSortValue(date)
                .build();

        table.deleteItem(key);
    }

    public List<WatchlistExpectationHistoryElement> readWatchlistByEmailAndSymbol(
            String email,
            String symbol) {
        Key key = Key.builder()
                .partitionValue(email + EMAIL_SYMBOL_SEPARATOR + symbol)
                .build();

        return table.query(
                r -> r.queryConditional(
                        software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(key)))
                .items()
                .stream()
                .toList();
    }

    public List<WatchlistExpectationHistoryElement> readAllExpectations() {
        return table.scan()
                .items()
                .stream()
                .toList();
    }
}
