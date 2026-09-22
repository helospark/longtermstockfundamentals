package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class WatchlistSegmentedRepository {
    DynamoDbTable<WatchlistElement> table;

    public WatchlistSegmentedRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(
                "WatchlistSegmented",
                TableSchema.fromClass(WatchlistElement.class));
        ;
    }

    public void save(WatchlistElement watchlist) {
        table.putItem(watchlist);
    }

    public List<WatchlistElement> readWatchlistByEmail(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        return table.query(
                r -> r.queryConditional(
                        software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
                                .keyEqualTo(key)))
                .items().stream().toList();
    }

    public List<WatchlistElement> readAllWatchlists() {
        return table.scan()
                .items()
                .stream()
                .toList();
    }

    public void remove(String email, String symbol) {
        Key key = Key.builder()
                .partitionValue(email)
                .addSortValue(symbol)
                .build();
        table.deleteItem(key);
    }

    public Optional<WatchlistElement> readWatchlistByEmailAndStock(String email, String symbol) {
        Key key = Key.builder()
                .partitionValue(email)
                .addSortValue(symbol)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }
}