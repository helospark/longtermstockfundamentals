package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class PortfolioPerformanceHistorySegmentedRepository {
    DynamoDbTable<PortfolioPerformanceHistoryElement> table;

    public PortfolioPerformanceHistorySegmentedRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(
                "PortfolioPerformanceHistorySegmented",
                TableSchema.fromClass(PortfolioPerformanceHistoryElement.class));
    }

    public void save(PortfolioPerformanceHistoryElement data) {
        this.table.putItem(data);
    }

    public void deleteForUser(String user) {
        Key key = Key.builder()
                .partitionValue(user)
                .build();
        this.table.deleteItem(key);
    }

    public List<PortfolioPerformanceHistoryElement> readHistoricalPortfolio(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();
        return table.query(
                r -> r.queryConditional(
                        software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
                                .keyEqualTo(key)))
                .items().stream().toList();
    }
}
