package com.helospark.financialdata.management.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class UserRepository {
    DynamoDbEnhancedClient enhancedClient;
    DynamoDbTable<User> table;

    public UserRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;

        TableSchema<User> schema = TableSchema.fromBean(User.class);

        this.table = enhancedClient.table("User", schema);
    }

    public Optional<User> findByEmail(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }

    public User findByEmailOrThrow(String email) {
        var optionalUser = findByEmail(email);

        if (!optionalUser.isPresent()) {
            throw new RuntimeException("User does not exist");
        }

        return optionalUser.get();
    }

    public void save(User user) {
        table.putItem(user);
    }

    public void delete(User user) {
        table.deleteItem(user);
    }

    public List<User> getAllUsers() {
        return table.scan()
                .items()
                .stream()
                .toList();
    }
}
