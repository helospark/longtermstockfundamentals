package com.helospark.financialdata.management.chartorder;

import java.util.List;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@Repository
public class ChartOrderRepository {
    DynamoDbTable<ChartOrder> table;

    public ChartOrderRepository(DynamoDbEnhancedClient enhancedClient) {
        table = enhancedClient.table(
                "ChartOrder",
                TableSchema.fromClass(ChartOrder.class));
    }

    public void save(ChartOrder chartOrder) {
        table.putItem(chartOrder);
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

        return table.query(request)
                .items()
                .stream()
                .toList();
    }

    public void delete(String userEmail, String name) {
        ChartOrder orderToDelete = new ChartOrder();
        orderToDelete.setUserEmail(userEmail);
        orderToDelete.setName(name);

        table.deleteItem(orderToDelete);
    }
}
