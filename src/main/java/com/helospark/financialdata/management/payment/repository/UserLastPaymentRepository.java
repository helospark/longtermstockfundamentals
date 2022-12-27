package com.helospark.financialdata.management.payment.repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

@Repository
public class UserLastPaymentRepository {
    @Autowired
    DynamoDBMapper mapper;

    public Optional<UserLastPayment> findByEmail(String email) {
        return Optional.ofNullable(mapper.load(UserLastPayment.class, email));
    }

    public void save(UserLastPayment stripeUserMapping) {
        mapper.save(stripeUserMapping);
    }

}
