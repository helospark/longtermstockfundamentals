package com.helospark.financialdata.management.watchlist.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class JobLastRunRepository {
    DynamoDbTable<JobLastRunData> table;

    public JobLastRunRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(
                "JobLastRunData",
                TableSchema.fromClass(JobLastRunData.class));
        ;
    }

    public void save(JobLastRunData data) {
        table.putItem(data);
    }

    public Optional<JobLastRunData> readJobLastRanByName(String jobName) {
        Key key = Key.builder()
                .partitionValue(jobName)
                .build();

        JobLastRunData result = this.table.getItem(key);

        return Optional.ofNullable(result);
    }
}
