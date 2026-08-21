package com.helospark.financialdata.management.watchlist.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

@Repository
public class PortfolioTransactionRepository {

    @Autowired
    private DynamoDBMapper mapper;

    public void saveTransaction(PortfolioTransaction transaction) {
        mapper.save(transaction);
    }

    public List<PortfolioTransaction> getTransactionsByUserEmail(String userEmail) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1", new AttributeValue().withS(userEmail));

        DynamoDBQueryExpression<PortfolioTransaction> queryExpression = new DynamoDBQueryExpression<PortfolioTransaction>()
                .withKeyConditionExpression("userEmail = :v1")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(false); // Newest first based on ISO-8601 String Range Key

        return mapper.query(PortfolioTransaction.class, queryExpression);
    }

    public List<PortfolioTransaction> getLatestTransactionsByUserEmail(String userEmail, int limit) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1", new AttributeValue().withS(userEmail));

        DynamoDBQueryExpression<PortfolioTransaction> queryExpression = new DynamoDBQueryExpression<PortfolioTransaction>()
                .withKeyConditionExpression("userEmail = :v1")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(false) // Newest first
                .withLimit(limit); // Limit response count at DynamoDB engine level

        return mapper.query(PortfolioTransaction.class, queryExpression);
    }
}