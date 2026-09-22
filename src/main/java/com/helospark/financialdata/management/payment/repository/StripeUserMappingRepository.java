package com.helospark.financialdata.management.payment.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
public class StripeUserMappingRepository {
    DynamoDbEnhancedClient enhancedClient;

    public StripeUserMappingRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<StripeUserMapping> getTable() {
        return enhancedClient.table(
                "StripeUserMapping",
                EnhancedSchemaCache.getSchema(StripeUserMapping.class));
    }

    public Optional<StripeUserMapping> getStripeUserMapping(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        return Optional.ofNullable(getTable().getItem(key));
    }

    public Optional<StripeUserMapping> findStripeUserMappingByEmail(String email) {
        return findAllStripeUsersWithEmail(email)
                .stream()
                .findFirst();
    }

    public List<StripeUserMapping> findAllStripeUsersWithEmail(String email) {
        return getTable().scan(r -> r.filterExpression(
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
                .forEach(getTable()::deleteItem);
    }

    public void removeConfirmationEmail(String value) {
        Key key = Key.builder()
                .partitionValue(value)
                .build();

        getTable().deleteItem(key);
    }

    public void save(StripeUserMapping stripeUserMapping) {
        getTable().putItem(stripeUserMapping);
    }
}
