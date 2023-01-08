package com.helospark.financialdata.management.watchlist.repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

@Repository
public class WatchlistRepository {
    @Autowired
    DynamoDBMapper mapper;

    public void save(Watchlist watchlist) {
        mapper.save(watchlist);
    }

    public Optional<Watchlist> readWatchlistByEmail(String email) {
        return Optional.ofNullable(mapper.load(Watchlist.class, email));
    }
}
