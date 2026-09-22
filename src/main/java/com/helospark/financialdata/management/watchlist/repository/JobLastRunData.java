package com.helospark.financialdata.management.watchlist.repository;

import java.time.LocalDate;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class JobLastRunData {
    private String job;
    private LocalDate date;

    public JobLastRunData() {
    }

    public JobLastRunData(String job, LocalDate date) {
        this.job = job;
        this.date = date;
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("job")
    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

}
