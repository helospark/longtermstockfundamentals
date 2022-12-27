package com.helospark.financialdata.management.user.repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

@Repository
public class ConfirmationEmailRepository {
    @Autowired
    DynamoDBMapper mapper;

    public Optional<ConfirmationEmail> getConfirmationEmail(String value) {
        return Optional.ofNullable(mapper.load(ConfirmationEmail.class, value));
    }

    public void removeConfirmationEmail(String value) {
        PersistentSignin toDelete = new PersistentSignin();
        toDelete.setKey(value);
        mapper.delete(toDelete);
    }

    public void save(ConfirmationEmail confirmationEmail) {
        mapper.save(confirmationEmail);
    }

}
