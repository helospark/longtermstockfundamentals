package com.helospark.financialdata.management.user.repository;

import java.util.Optional;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Component
public class PersistentSigninRepository {
    DynamoDbTable<PersistentSignin> table;

    public PersistentSigninRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(
                "PersistentSignin",
                TableSchema.fromClass(PersistentSignin.class));
    }

    public Optional<PersistentSignin> getPersistentSignin(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }

    public void removePersistentSigning(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        table.deleteItem(key);
    }

    public void save(PersistentSignin persistentSignin) {
        table.putItem(persistentSignin);
    }
}
