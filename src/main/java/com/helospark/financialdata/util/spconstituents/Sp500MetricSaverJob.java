package com.helospark.financialdata.util.spconstituents;

import static com.helospark.financialdata.CommonConfig.BASE_FOLDER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helospark.financialdata.management.watchlist.repository.JobLastRunData;
import com.helospark.financialdata.management.watchlist.repository.JobLastRunRepository;

@Component
public class Sp500MetricSaverJob {
    public static final File FUNDAMENTALS_FILE = new File(BASE_FOLDER + "/info/sp500_fundamentals.json");
    private static final String JOB_NAME = "SP500_metric";
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    Sp500MetricCalculator calculator;
    @Autowired
    private JobLastRunRepository jobLastRunRepository;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.DAYS)
    public void saveMetrics() {
        LocalDate currentDate = LocalDate.now();
        Optional<JobLastRunData> result = jobLastRunRepository.readJobLastRanByName(JOB_NAME);

        if (result.isEmpty() || jobRanMoreThanAMonthAgo(currentDate, result.get().getDate())) {
            runJob(currentDate);
        }
    }

    public void runJob(LocalDate currentDate) {
        try {
            GeneralCompanyMetrics metric = calculator.calculateMetrics();
            FileOutputStream fos = new FileOutputStream(FUNDAMENTALS_FILE);
            objectMapper.writeValue(fos, metric);

            jobLastRunRepository.save(new JobLastRunData(JOB_NAME, currentDate));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public GeneralCompanyMetrics loadCachedMetric() {
        try {
            FileInputStream fis = new FileInputStream(FUNDAMENTALS_FILE);

            return objectMapper.readValue(fis, GeneralCompanyMetrics.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean jobRanMoreThanAMonthAgo(LocalDate currentDate, LocalDate lastRanDate) {
        return ChronoUnit.MONTHS.between(lastRanDate, currentDate) >= 1;
    }

}
