package com.helospark.financialdata.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;

import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.StockDataDownloader.DownloadDateData;

import jakarta.annotation.PostConstruct;

//@Component
public class SlowScraper implements Runnable {
    private static final Random RANDOM = new Random();
    @Autowired
    SymbolAtGlanceProvider symbolAtGlanceProvider;

    @PostConstruct
    public void init() {
        Thread.ofVirtual().start(this);
    }

    @Override
    public void run() {
        List<String> symbols = new ArrayList<>(DataLoader.provideUsSymbols());
        Collections.shuffle(symbols);
        Map<String, DownloadDateData> symbolToDates = StockDataDownloader.loadDateData();
        int index = 0;
        while (true) {
            try {
                sleep(RANDOM.nextLong(2000, 3300));

                String symbol = null;
                for (; index < symbols.size(); ++index) {
                    symbol = symbols.get(index);

                    DownloadDateData downloadData = symbolToDates.get(symbol);

                    if (downloadData == null || (notDelisted(downloadData) && isExpectedReportDateAfterToday(downloadData))) {
                        break;
                    }
                }
                if (symbol != null && index < symbols.size()) {
                    System.out.println("Downloading the following symbol now: '" + symbol + "'");
                    StockDataDownloader.downloadMultiStockYahoo(List.of(symbol), symbolAtGlanceProvider);
                }
                if (index > symbols.size()) {
                    index = 0;
                } else {
                    ++index;
                }
            } catch (Exception e) {
                e.printStackTrace();
                ++index;
            }
        }
    }

    private boolean notDelisted(DownloadDateData downloadData) {
        LocalDate now = LocalDate.now();
        LocalDate lastDate = downloadData.lastReportDate;
        return Math.abs(ChronoUnit.YEARS.between(now, lastDate)) < 2;
    }

    public boolean isExpectedReportDateAfterToday(DownloadDateData downloadData) {
        LocalDate now = LocalDate.now();
        LocalDate expectedReportDate = downloadData.lastReportDate.plusDays(downloadData.previousReportPeriod);
        return now.isAfter(expectedReportDate);
    }

    public void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
