package com.helospark.financialdata.management.config;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public class EnhancedSchemaCache {

    public static <T> TableSchema<T> getSchema(Class<T> entityClass) {
        return TableSchema.fromClass(entityClass);
    }
}
