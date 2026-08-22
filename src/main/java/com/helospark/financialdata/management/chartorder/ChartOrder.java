package com.helospark.financialdata.management.chartorder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "ChartOrder")
public class ChartOrder {
    private String userEmail;
    private String name;
    private String formatJson;

    public ChartOrder() {
    }

    public ChartOrder(String userEmail, String name, String formatJson) {
        this.userEmail = userEmail;
        this.name = name;
        this.formatJson = formatJson;
    }

    @DynamoDBHashKey(attributeName = "userEmail")
    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    @DynamoDBRangeKey(attributeName = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormatJson() {
        return formatJson;
    }

    public void setFormatJson(String formatJson) {
        this.formatJson = formatJson;
    }

}
