package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class WatchlistRepository {
    DynamoDbTable<Watchlist> table;

    public WatchlistRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(
                "Watchlist",
                TableSchema.fromClass(Watchlist.class));
    }

    public void save(Watchlist watchlist) {
        table.putItem(watchlist);
    }

    public Optional<Watchlist> readWatchlistByEmail(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }

    public List<Watchlist> readAllWatchlists() {
        return table.scan()
                .items()
                .stream()
                .toList();
    }
}