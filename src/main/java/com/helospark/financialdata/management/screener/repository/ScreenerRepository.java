package com.helospark.financialdata.management.screener.repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

@Repository
public class ScreenerRepository {
    @Autowired
    DynamoDBMapper mapper;

    public void save(Screener watchlist) {
        mapper.save(watchlist);
    }

    public Optional<Screener> readScreenerByEmail(String email) {
        return Optional.ofNullable(mapper.load(Screener.class, email));
    }
}
