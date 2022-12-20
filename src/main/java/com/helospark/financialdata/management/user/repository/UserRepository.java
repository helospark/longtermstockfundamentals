package com.helospark.financialdata.management.user.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

@Repository
public class UserRepository {
    @Autowired
    DynamoDBMapper mapper;

    public Optional<User> findByUserName(String userName) {
        return Optional.ofNullable(mapper.load(User.class, userName));
    }

    public void save(User user) {
        mapper.save(user);
    }

    public List<User> findByEmail(String email) {
        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":val1", new AttributeValue().withS(email));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("email = :val1")
                .withExpressionAttributeValues(eav);

        PaginatedScanList<User> result = mapper.scan(User.class, scanExpression);
        return new ArrayList<>(result);
    }

}
