package com.helospark.financialdata.management.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.user.repository.ViewedStocksRepository;

@Component
public class ClearViewCountJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClearViewCountJob.class);
    @Autowired
    private ViewedStocksRepository viewedStocksRepository;
    @Autowired
    private ViewedStocksService viewedStocksService;

    @Scheduled(cron = "0 0 0 1 * *") // TODO: what if server is not running at this time
    public void clearViewCount() {
        LOGGER.info("Removing all viewed stocks");
        viewedStocksRepository.removeAll();
        viewedStocksService.clearCache();
        LOGGER.info("Viewed stocks removed");
    }
}
