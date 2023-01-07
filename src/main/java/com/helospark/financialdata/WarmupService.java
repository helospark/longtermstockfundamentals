package com.helospark.financialdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class WarmupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Logger.class);
    @Autowired
    private FinancialsController financialController;

    @PostConstruct
    public void doImmediateWarmup() {
        for (int i = 0; i < 10; ++i) {
            warmupFinancialController();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        // load screener backtest here
    }

    public void warmupFinancialController() {
        try {
            financialController.get7yrFcfGrowthRateMovingAvg("INTC", 7);
            financialController.getRoic("INTC");
        } catch (Exception e) {
            LOGGER.warn("Unable to warmup", e);
        }
    }
}
