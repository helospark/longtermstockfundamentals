package com.helospark.financialdata.management.payment.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Repository
public class UserLastPaymentRepository {
    DynamoDbEnhancedClient enhancedClient;

    public UserLastPaymentRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<UserLastPayment> getTable() {
        return enhancedClient.table(
                "UserLastPayment",
                EnhancedSchemaCache.getSchema(UserLastPayment.class));
    }

    public Optional<UserLastPayment> findByEmail(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        return Optional.ofNullable(getTable().getItem(key));
    }

    public void save(UserLastPayment stripeUserMapping) {
        getTable().putItem(stripeUserMapping);
    }
}
