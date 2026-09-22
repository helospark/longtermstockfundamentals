package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Repository
public class WatchlistSegmentedRepository {

    DynamoDbEnhancedClient enhancedClient;

    public WatchlistSegmentedRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<WatchlistElement> getTable() {
        return enhancedClient.table(
                "WatchlistElement",
                EnhancedSchemaCache.getSchema(WatchlistElement.class));
    }

    public void save(WatchlistElement watchlist) {
        getTable().putItem(watchlist);
    }

    public List<WatchlistElement> readWatchlistByEmail(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        return getTable().query(
                r -> r.queryConditional(
                        software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
                                .keyEqualTo(key)))
                .items().stream().toList();
    }

    public List<WatchlistElement> readAllWatchlists() {
        return getTable().scan()
                .items()
                .stream()
                .toList();
    }
}