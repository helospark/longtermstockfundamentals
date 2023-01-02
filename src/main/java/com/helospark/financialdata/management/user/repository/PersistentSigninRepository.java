package com.helospark.financialdata.management.user.repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;

@Component
public class PersistentSigninRepository {
    @Autowired
    DynamoDBMapper mapper;

    public Optional<PersistentSignin> getPersistentSignin(String value) {
        return Optional.ofNullable(mapper.load(PersistentSignin.class, value));
    }

    public void removePersistentSigning(String value) {
        PersistentSignin toDelete = new PersistentSignin();
        toDelete.setKey(value);
        mapper.delete(toDelete);
    }

    public void save(PersistentSignin persistentSignin) {
        mapper.save(persistentSignin);
    }

    public void listAll() {
        PaginatedScanList<PersistentSignin> allElements = mapper.scan(PersistentSignin.class, new DynamoDBScanExpression());

        allElements.forEach(element -> {
            System.out.println(element);
        });
    }
}
