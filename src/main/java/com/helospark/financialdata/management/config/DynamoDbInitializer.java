package com.helospark.financialdata.management.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.helospark.financialdata.management.user.repository.PersistentSignin;
import com.helospark.financialdata.management.user.repository.User;

import jakarta.annotation.PostConstruct;

@Component
public class DynamoDbInitializer {
    @Autowired
    AmazonDynamoDB amazonDynamoDB;
    @Autowired
    DynamoDBMapper mapper;

    @PostConstruct
    public void createTables() {
        createTable("User", User.class);
        createTable("PersistentSignin", PersistentSignin.class);
    }

    public void createTable(String tableName, Class<?> class1) {
        if (!doesTableExist(tableName)) {
            CreateTableRequest tableRequest = mapper.generateCreateTableRequest(class1);
            tableRequest.setBillingMode("PAY_PER_REQUEST");
            amazonDynamoDB.createTable(tableRequest);

            for (int i = 0; i < 10; ++i) {
                if (doesTableExist(tableName)) {
                    break;
                } else {
                    System.out.println("Waiting for " + tableName + " table to be created");
                    exceptionlessSleep(1);
                }
            }
        }
    }

    private void exceptionlessSleep(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean doesTableExist(String tableName) {
        try {
            return amazonDynamoDB.describeTable(tableName) != null;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

}
