package com.helospark.financialdata.management.user.repository;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Component
public class PersistentSigninRepository {
    DynamoDbEnhancedClient enhancedClient;

    public PersistentSigninRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<PersistentSignin> getTable() {
        return enhancedClient.table(
                "PersistentSignin",
                EnhancedSchemaCache.getSchema(PersistentSignin.class));
    }

    public Optional<PersistentSignin> getPersistentSignin(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        return Optional.ofNullable(getTable().getItem(key));
    }

    public void removePersistentSigning(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        getTable().deleteItem(key);
    }

    public void save(PersistentSignin persistentSignin) {
        getTable().putItem(persistentSignin);
    }
}
