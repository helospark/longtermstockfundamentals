package com.helospark.financialdata.management.watchlist.repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

@Repository
public class JobLastRunRepository {
    @Autowired
    DynamoDBMapper mapper;

    public void save(JobLastRunData data) {
        mapper.save(data);
    }

    public Optional<JobLastRunData> readJobLastRanByName(String jobName) {
        return Optional.ofNullable(mapper.load(JobLastRunData.class, jobName));
    }
}
