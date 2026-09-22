package com.helospark.financialdata.management.user.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class ConfirmationEmailRepository {
    DynamoDbTable<ConfirmationEmail> table;

    public ConfirmationEmailRepository(DynamoDbEnhancedClient enhancedClient) {
        table = enhancedClient.table(
                "ConfirmationEmail",
                TableSchema.fromClass(ConfirmationEmail.class));
    }

    public Optional<ConfirmationEmail> getConfirmationEmail(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }

    public void removeConfirmationEmail(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        table.deleteItem(key);
    }

    public void save(ConfirmationEmail confirmationEmail) {
        table.putItem(confirmationEmail);
    }
}
