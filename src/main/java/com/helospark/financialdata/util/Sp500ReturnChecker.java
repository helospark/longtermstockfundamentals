package com.helospark.financialdata.util;

import static com.helospark.financialdata.service.StandardAndPoorPerformanceProvider.prices;

import java.time.LocalDate;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.Helpers;

public class Sp500ReturnChecker {

    public static void main(String[] args) {

        for (int i = 0; i < 100 * 12; ++i) {
            double yearsAgo = i / 12.0;
            LocalDate date = CommonConfig.NOW.minusMonths((long) (yearsAgo * 12.0));
            int oldIndex = Helpers.findIndexWithOrBeforeDate(prices, date);
            if (oldIndex >= prices.size() || oldIndex == -1) {
                break;
            }

            HistoricalPriceElement latestPrice = prices.get(0);
            HistoricalPriceElement oldPrice = prices.get(oldIndex);

            double growth = GrowthCalculator.calculateGrowth(latestPrice.close, oldPrice.close, yearsAgo);
            System.out.println(date + "," + growth);
        }
    }

}
