package com.helospark.financialdata.management.inspire.jobs;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.util.analyzer.HighRoicScreener;

@Component
public class HighRoicJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(HighRoicJob.class);

    @Scheduled(cron = "0 0 0 * * *")
    public void runHighRoicJob() {
        try {
            LOGGER.info("Starting high ROIC job");

            Set<String> symbols = DataLoader.provideSymbolsFromNasdaqNyse();
            new HighRoicScreener().analyze(symbols);

            LOGGER.info("Finished high ROIC job");
        } catch (Exception e) {
            LOGGER.error("Unable to run high ROIC job", e);
        }
    }
}
