package com.helospark.financialdata.management.user.repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

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
}
