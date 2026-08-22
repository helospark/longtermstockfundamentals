package com.helospark.financialdata.management.chartorder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

@Repository
public class ChartOrderRepository {
    @Autowired
    private DynamoDBMapper mapper;

    public void save(ChartOrder chartOrder) {
        mapper.save(chartOrder);
    }

    public List<ChartOrder> getAll(String userEmail) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1", new AttributeValue().withS(userEmail));

        DynamoDBQueryExpression<ChartOrder> queryExpression = new DynamoDBQueryExpression<ChartOrder>()
                .withKeyConditionExpression("userEmail = :v1")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(false);

        return mapper.query(ChartOrder.class, queryExpression);
    }

    public void delete(String userEmail, String name) {
        ChartOrder orderToDelete = new ChartOrder();
        orderToDelete.setUserEmail(userEmail);
        orderToDelete.setName(name);

        mapper.delete(orderToDelete);
    }
}
