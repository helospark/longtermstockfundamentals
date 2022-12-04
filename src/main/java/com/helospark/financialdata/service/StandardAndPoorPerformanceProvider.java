package com.helospark.financialdata.service;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.HistoricalPriceElement;

public class StandardAndPoorPerformanceProvider {
    static List<HistoricalPriceElement> prices = new ArrayList<>();

    static {
        prices = DataLoader.loadHistoricalFile(new File(CommonConfig.BASE_FOLDER + "/info/s&p500_price.json"));
    }

    public static double getGrowth(int yearsAgo) {
        int oldIndex = Helpers.findIndexWithOrBeforeDate(prices, LocalDate.now().minusYears(yearsAgo));
        if (oldIndex >= prices.size() || oldIndex == -1) {
            oldIndex = prices.size() - 1;
        }

        HistoricalPriceElement latestPrice = prices.get(0);
        HistoricalPriceElement oldPrice = prices.get(oldIndex);

        return GrowthCalculator.calculateGrowth(latestPrice.close, oldPrice.close, yearsAgo);
    }
}