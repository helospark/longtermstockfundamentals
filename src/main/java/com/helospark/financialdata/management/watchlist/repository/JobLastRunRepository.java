package com.helospark.financialdata.management.watchlist.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.helospark.financialdata.management.config.EnhancedSchemaCache;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Repository
public class JobLastRunRepository {
    DynamoDbEnhancedClient enhancedClient;

    public JobLastRunRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public DynamoDbTable<JobLastRunData> getTable() {
        return enhancedClient.table(
                "JobLastRunData",
                EnhancedSchemaCache.getSchema(JobLastRunData.class));
    }

    public void save(JobLastRunData data) {
        getTable().putItem(data);
    }

    public Optional<JobLastRunData> readJobLastRanByName(String jobName) {
        Key key = Key.builder()
                .partitionValue(jobName)
                .build();

        JobLastRunData result = this.getTable().getItem(key);

        return Optional.ofNullable(result);
    }
}
