package com.helospark.financialdata.management.user.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Repository
public class ConfirmationEmailRepository {
    DynamoDbEnhancedClient enhancedClient;

    public ConfirmationEmailRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<ConfirmationEmail> getTable() {
        return enhancedClient.table(
                "ConfirmationEmail",
                EnhancedSchemaCache.getSchema(ConfirmationEmail.class));
    }

    public Optional<ConfirmationEmail> getConfirmationEmail(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        return Optional.ofNullable(getTable().getItem(key));
    }

    public void removeConfirmationEmail(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        getTable().deleteItem(key);
    }

    public void save(ConfirmationEmail confirmationEmail) {
        getTable().putItem(confirmationEmail);
    }
}
