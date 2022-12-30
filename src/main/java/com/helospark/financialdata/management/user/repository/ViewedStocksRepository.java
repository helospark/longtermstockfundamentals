package com.helospark.financialdata.management.user.repository;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;

@Component
public class ViewedStocksRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewedStocksRepository.class);
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

    public void removeAll() {
        PaginatedScanList<ViewedStocks> allElements = mapper.scan(ViewedStocks.class, new DynamoDBScanExpression());

        allElements.forEach(element -> {
            LOGGER.debug("User '{}' had stocks '{}'", element.getEmail(), element.getStocks());
            clearViewedStocks(element.getEmail());
        });
    }

}
