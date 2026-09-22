package com.helospark.financialdata.management.user.repository;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Component
public class ViewedStocksRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewedStocksRepository.class);
    DynamoDbEnhancedClient enhancedClient;

    public ViewedStocksRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<ViewedStocks> getTable() {
        return enhancedClient.table(
                "ViewedStocks",
                EnhancedSchemaCache.getSchema(ViewedStocks.class));
    }

    public Optional<ViewedStocks> getViewedStocks(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        return Optional.ofNullable(getTable().getItem(key));
    }

    public void clearViewedStocks(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        getTable().deleteItem(key);
    }

    public void save(ViewedStocks viewedStocks) {
        getTable().putItem(viewedStocks);
    }

    public void removeAll() {
        getTable().scan()
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
