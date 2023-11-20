package com.helospark.financialdata.management.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;

@Repository
public class UserRepository {
    @Autowired
    DynamoDBMapper mapper;

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(mapper.load(User.class, email));
    }

    public void save(User user) {
        mapper.save(user);
    }

    public void delete(User user) {
        mapper.delete(user);
    }

    public List<User> getAllUsers() {
        return mapper.scan(User.class, new DynamoDBScanExpression());
    }

}
