package com.helospark.financialdata.management.payment.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
public class StripeUserMappingRepository {
    DynamoDbTable<StripeUserMapping> table;

    public StripeUserMappingRepository(DynamoDbEnhancedClient enhancedClient) {
        table = enhancedClient.table(
                "StripeUserMapping",
                TableSchema.fromClass(StripeUserMapping.class));
    }

    public Optional<StripeUserMapping> getStripeUserMapping(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }

    public Optional<StripeUserMapping> findStripeUserMappingByEmail(String email) {
        return findAllStripeUsersWithEmail(email)
                .stream()
                .findFirst();
    }

    public List<StripeUserMapping> findAllStripeUsersWithEmail(String email) {
        return table.scan(r -> r.filterExpression(
                Expression.builder()
                        .expression("email = :val1")
                        .expressionValues(Map.of(
                                ":val1", AttributeValue.builder()
                                        .s(email)
                                        .build()))
                        .build()))
                .items()
                .stream()
                .toList();
    }

    public void removeAllEntriesWithEmail(String email) {
        findAllStripeUsersWithEmail(email)
                .forEach(table::deleteItem);
    }

    public void removeConfirmationEmail(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        table.deleteItem(key);
    }

    public void save(StripeUserMapping stripeUserMapping) {
        table.putItem(stripeUserMapping);
    }
}
