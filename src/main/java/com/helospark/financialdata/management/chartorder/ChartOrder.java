package com.helospark.financialdata.management.chartorder;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
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

    @DynamoDbPartitionKey
    @DynamoDbAttribute("userEmail")
    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("name")
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
