package com.helospark.financialdata.management.payment.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class UserLastPaymentRepository {
    DynamoDbTable<UserLastPayment> table;

    public UserLastPaymentRepository(DynamoDbEnhancedClient enhancedClient) {
        table = enhancedClient.table(
                "UserLastPayment",
                TableSchema.fromClass(UserLastPayment.class));
    }

    public Optional<UserLastPayment> findByEmail(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }

    public void save(UserLastPayment stripeUserMapping) {
        table.putItem(stripeUserMapping);
    }
}
