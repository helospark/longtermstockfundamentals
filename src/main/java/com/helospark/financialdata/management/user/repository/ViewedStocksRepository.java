package com.helospark.financialdata.management.user.repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

@Component
public class ViewedStocksRepository {
    @Autowired
    DynamoDBMapper mapper;

    public Optional<ViewedStocks> getViewedStocks(String value) {
        return Optional.ofNullable(mapper.load(ViewedStocks.class, value));
    }

    public void clearViewedStocks(String email) {
        ViewedStocks toDelete = new ViewedStocks();
        toDelete.setEmail(email);
        mapper.delete(toDelete);
    }

    public void save(ViewedStocks viewedStocks) {
        mapper.save(viewedStocks);
    }
}
