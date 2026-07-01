package com.helospark.financialdata.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.domain.SimpleDateDataElement;

public class DrawDownService {

    public static List<SimpleDateDataElement> getDrawdownChart(List<HistoricalPriceElement> prices) {
        List<SimpleDateDataElement> result = new ArrayList<>();

        HistoricalPriceElement latestData = prices.get(prices.size() - 1);
        double maxPrice = latestData.close;

        result.add(new SimpleDateDataElement(latestData.date, 100.0));

        for (int i = prices.size() - 2; i >= 0; --i) {
            double value = prices.get(i).close;

            if (value > maxPrice) {
                maxPrice = value;
            }

            double drawDownPercent = (value / maxPrice) * 100.0;

            result.add(new SimpleDateDataElement(prices.get(i).date, drawDownPercent));
        }

        Collections.reverse(result);

        return result;
    }

    public static double getPercentageNearTop(List<HistoricalPriceElement> prices, LocalDate onDate, double numberOfPastYears) {
        List<SimpleDateDataElement> drawdowns = getDrawdownChart(prices);
        int index = Helpers.findIndexWithOrBeforeDate(drawdowns, onDate);
        int oldIndex = Helpers.findIndexWithOrBeforeDate(drawdowns, onDate.minusDays((int) (numberOfPastYears * 365)));

        if (index == -1 || index >= oldIndex) {
            return 0.0;
        }

        if (oldIndex == -1) {
            oldIndex = prices.size() - 1;
        }

        int nearTop = 0;
        int all = 0;

        for (int i = oldIndex; i >= index; --i) {
            all += 1;

            if (drawdowns.get(i).value > 90) {
                nearTop += 1;
            }
        }

        return ((double) nearTop / all) * 100.0;
    }

    public static Optional<Double> getLowQualityDrawdownAt(CompanyFinancials company, LocalDate actualDate) {
        var list = company.financials.stream().map(a -> new HistoricalPriceElement(a.date, a.price)).collect(Collectors.toList());

        List<SimpleDateDataElement> drawDownChart = getDrawdownChart(list);

        int index = Helpers.findIndexWithOrBeforeDate(drawDownChart, actualDate);

        if (index != -1) {
            return Optional.of(100.0 - drawDownChart.get(index).value);
        } else {
            return Optional.empty();
        }
    }

}
