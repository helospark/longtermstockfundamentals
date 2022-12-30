package com.helospark.financialdata.management.payment.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

@Repository
public class StripeUserMappingRepository {
    @Autowired
    DynamoDBMapper mapper;

    public Optional<StripeUserMapping> getStripeUserMapping(String value) {
        return Optional.ofNullable(mapper.load(StripeUserMapping.class, value));
    }

    public Optional<StripeUserMapping> findStripeUserMappingByEmail(String email) {
        PaginatedScanList<StripeUserMapping> result = findAllStripeUsersWithEmail(email);

        if (result != null && result.size() > 0) {
            return Optional.of(result.get(0));
        } else {
            return Optional.empty();
        }
    }

    public PaginatedScanList<StripeUserMapping> findAllStripeUsersWithEmail(String email) {
        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":val1", new AttributeValue().withS(email));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("email = :val1")
                .withExpressionAttributeValues(eav);

        PaginatedScanList<StripeUserMapping> result = mapper.scan(StripeUserMapping.class, scanExpression);
        return result;
    }

    public void removeAllEntriesWithEmail(String email) {
        findAllStripeUsersWithEmail(email).forEach(entry -> mapper.delete(entry));
    }

    public void removeConfirmationEmail(String value) {
        StripeUserMapping toDelete = new StripeUserMapping();
        toDelete.setStripeCustomerId(value);
        mapper.delete(toDelete);
    }

    public void save(StripeUserMapping stripeUserMapping) {
        mapper.save(stripeUserMapping);
    }

}
