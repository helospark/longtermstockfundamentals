package com.helospark.financialdata.management.watchlist.repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

@Repository
public class WatchlistExpectationHistoryRepository {
    public static final String EMAIL_SYMBOL_SEPARATOR = " :: ";
    @Autowired
    DynamoDBMapper mapper;

    public void save(WatchlistExpectationHistory watchlist) {
        mapper.save(watchlist);
    }

    public Optional<WatchlistExpectationHistory> readWatchlistByEmailAndSymbol(String email, String symbol) {
        return Optional.ofNullable(mapper.load(WatchlistExpectationHistory.class, email + EMAIL_SYMBOL_SEPARATOR + symbol));
    }

}
