package com.helospark.financialdata.management.user.repository;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Component
public class ViewedStocksRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewedStocksRepository.class);
    DynamoDbTable<ViewedStocks> table;

    public ViewedStocksRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(
                "ViewedStocks",
                TableSchema.fromClass(ViewedStocks.class));
    }

    public Optional<ViewedStocks> getViewedStocks(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }

    public void clearViewedStocks(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        table.deleteItem(key);
    }

    public void save(ViewedStocks viewedStocks) {
        table.putItem(viewedStocks);
    }

    public void removeAll() {
        table.scan()
                .items()
                .forEach(element -> {
                    LOGGER.debug(
                            "User '{}' had stocks '{}'",
                            element.getEmail(),
                            element.getStocks());

                    clearViewedStocks(element.getEmail());
                });
    }
}
