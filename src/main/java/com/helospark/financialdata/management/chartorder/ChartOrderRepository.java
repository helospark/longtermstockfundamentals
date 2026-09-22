package com.helospark.financialdata.management.chartorder;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@Repository
public class ChartOrderRepository {
    DynamoDbEnhancedClient enhancedClient;

    public ChartOrderRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<ChartOrder> getTable() {
        return enhancedClient.table(
                "ChartOrder",
                EnhancedSchemaCache.getSchema(ChartOrder.class));
    }

    public void save(ChartOrder chartOrder) {
        getTable().putItem(chartOrder);
    }

    public List<ChartOrder> getAll(String userEmail) {
        Key key = Key.builder()
                .partitionValue(userEmail)
                .build();

        QueryConditional queryConditional = QueryConditional.keyEqualTo(key);

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .scanIndexForward(false)
                .build();

        return getTable().query(request)
                .items()
                .stream()
                .toList();
    }

    public void delete(String userEmail, String name) {
        ChartOrder orderToDelete = new ChartOrder();
        orderToDelete.setUserEmail(userEmail);
        orderToDelete.setName(name);

        getTable().deleteItem(orderToDelete);
    }
}
