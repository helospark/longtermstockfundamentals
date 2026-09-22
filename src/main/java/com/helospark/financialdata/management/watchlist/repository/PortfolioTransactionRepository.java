package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@Repository
public class PortfolioTransactionRepository {
    DynamoDbEnhancedClient enhancedClient;

    public PortfolioTransactionRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<PortfolioTransaction> getTable() {
        return enhancedClient.table(
                "PortfolioTransactionT",
                EnhancedSchemaCache.getSchema(PortfolioTransaction.class));
    }

    public void saveTransaction(PortfolioTransaction transaction) {
        getTable().putItem(transaction);
    }

    public List<PortfolioTransaction> getTransactionsByUserEmail(String userEmail) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(
                        QueryConditional.keyEqualTo(
                                Key.builder()
                                        .partitionValue(userEmail)
                                        .build()))
                .scanIndexForward(false)
                .build();

        return getTable().query(request)
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    public List<PortfolioTransaction> getLatestTransactionsByUserEmail(
            String userEmail,
            int limit) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(
                        QueryConditional.keyEqualTo(
                                Key.builder()
                                        .partitionValue(userEmail)
                                        .build()))
                .scanIndexForward(false)
                .limit(limit)
                .build();

        return getTable().query(request)
                .items()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}