package com.helospark.financialdata.management.watchlist.repository;

import java.time.LocalDate;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.helospark.financialdata.management.helper.LocalDateConverter;

@DynamoDBTable(tableName = "JobLastRunData")
public class JobLastRunData {
    private String job;
    private LocalDate date;

    public JobLastRunData() {
    }

    public JobLastRunData(String job, LocalDate date) {
        this.job = job;
        this.date = date;
    }

    @DynamoDBHashKey
    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    @DynamoDBTypeConverted(converter = LocalDateConverter.class)
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

}
