package com.helospark.financialdata;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.domain.SimpleDataElement;
import com.helospark.financialdata.service.CpiAdjustor;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;

@RestController
@RequestMapping("/sp500/data")
public class Sp500Controller {

    @GetMapping("/price")
    public List<SimpleDataElement> getPrice() {
        var asd = StandardAndPoorPerformanceProvider.prices;

        int maxElements = 300;

        int steps = asd.size() / maxElements;

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < asd.size(); i += steps) {
            result.add(new SimpleDataElement(asd.get(i).date.toString(), asd.get(i).close));
        }
        return result;
    }

    @GetMapping("/price_with_reinvested_dividends")
    public List<SimpleDataElement> getPriceWithReinvestedDividends() {
        var asd = StandardAndPoorPerformanceProvider.prices;

        int maxElements = 300;

        int steps = asd.size() / maxElements;

        List<SimpleDataElement> result = new ArrayList<>();
        double shareCount = 1.0;
        int lastYear = asd.get(asd.size() - 1).date.getYear();
        for (int i = asd.size() - 1; i >= 0; i -= steps) {
            LocalDate dateThen = asd.get(i).date;
            double priceThen = asd.get(i).close;

            int newYear = dateThen.getYear();
            if (lastYear != newYear) {
                Double dividendPaid = StandardAndPoorPerformanceProvider.getDividendsPaidInYear(newYear);

                shareCount += dividendPaid / StandardAndPoorPerformanceProvider.getPriceAt(LocalDate.of(newYear, 1, 1));
                lastYear = newYear;
            }
            double value = shareCount * priceThen;
            result.add(new SimpleDataElement(dateThen.toString(), value));
        }
        Collections.reverse(result);
        return result;
    }

    @GetMapping("/price_reinv_dividends_infl_adjusted")
    public List<SimpleDataElement> getPriceWithReinvestedDividendsInflactionAdjusted() {
        List<SimpleDataElement> asd = getPriceWithReinvestedDividends();
        LocalDate now = LocalDate.now();

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < asd.size(); i++) {
            SimpleDataElement currentElement = asd.get(i);
            double priceThen = currentElement.value;
            double adjustedPriceThen = CpiAdjustor.adjustForInflationToOldDate(priceThen, LocalDate.parse(currentElement.date), now);

            result.add(new SimpleDataElement(currentElement.date, adjustedPriceThen));
        }

        return result;
    }

    @GetMapping("/price_growth")
    public List<SimpleDataElement> getPriceGrowth() {
        List<HistoricalPriceElement> asd = StandardAndPoorPerformanceProvider.prices;
        LocalDate now = LocalDate.now();

        double latestPrice = StandardAndPoorPerformanceProvider.getLatestPrice();

        List<SimpleDataElement> result = new ArrayList<>();
        int i = 4;
        while (true) {
            double yearsAgo = (i / 4.0);
            LocalDate date = now.minusMonths((int) (yearsAgo * 12.0));

            int index = Helpers.findIndexWithOrBeforeDate(asd, date);

            if (index == -1) {
                break;
            }

            HistoricalPriceElement actualElement = asd.get(index);
            double priceThen = actualElement.close;

            double growth = GrowthCalculator.calculateGrowth(latestPrice, priceThen, yearsAgo);

            result.add(new SimpleDataElement(actualElement.date.toString(), growth));

            i += 2;
        }

        return result;
    }

    @GetMapping("/price_growth_reinv_dividends")
    public List<SimpleDataElement> getPriceGrowthWithReinvestedDividends() {
        List<SimpleDataElement> asd = getPriceWithReinvestedDividends();
        LocalDate now = LocalDate.now();

        double latestPrice = asd.get(0).value;

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < asd.size(); i += 1) {
            double yearsAgo = Math.abs(ChronoUnit.DAYS.between(now, LocalDate.parse(asd.get(i).date)) / 365.0);

            double priceThen = asd.get(i).value;

            double growth = GrowthCalculator.calculateGrowth(latestPrice, priceThen, yearsAgo);

            result.add(new SimpleDataElement(asd.get(i).date, growth));
        }

        return result;
    }

    @GetMapping("/price_growth_reinv_dividends_infl_adjust")
    public List<SimpleDataElement> getPriceGrowthWithReinvestedDividendsInflatationAdjust() {
        List<SimpleDataElement> asd = getPriceWithReinvestedDividendsInflactionAdjusted();
        LocalDate now = LocalDate.now();

        double latestPrice = asd.get(0).value;

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < asd.size(); i += 1) {
            LocalDate dateThen = LocalDate.parse(asd.get(i).date);
            double yearsAgo = Math.abs(ChronoUnit.DAYS.between(now, dateThen) / 365.0);

            double priceThen = asd.get(i).value;

            double growth = GrowthCalculator.calculateGrowth(latestPrice, priceThen, yearsAgo);

            result.add(new SimpleDataElement(asd.get(i).date, growth));
        }

        return result;
    }

    @GetMapping("/price_growth_intervals")
    public List<SimpleDataElement> getPriceGrowthInIntervals(@RequestParam(name = "year", required = false, defaultValue = "10") int years) {
        List<HistoricalPriceElement> asd = StandardAndPoorPerformanceProvider.prices;
        LocalDate now = LocalDate.now();

        List<SimpleDataElement> result = new ArrayList<>();
        int i = 4;
        while (true) {
            double yearsAgo = (i / 4.0);
            LocalDate date = now.minusMonths((int) (yearsAgo * 12.0));
            LocalDate offsetDate = date.minusMonths((int) (years * 12.0));

            int nowIndex = Helpers.findIndexWithOrBeforeDate(asd, date);
            int oldIndex = Helpers.findIndexWithOrBeforeDate(asd, offsetDate);

            if (nowIndex == -1 || oldIndex == -1) {
                break;
            }

            HistoricalPriceElement newElement = asd.get(nowIndex);
            double priceNow = newElement.close;
            HistoricalPriceElement oldElement = asd.get(oldIndex);
            double priceThen = oldElement.close;

            double growth = GrowthCalculator.calculateGrowth(priceNow, priceThen, years);

            result.add(new SimpleDataElement(newElement.date.toString(), growth));

            i += 2;
        }

        return result;
    }

    @GetMapping("/price_growth_x_yrs_intervals_divs_infl_adjusted")
    public List<SimpleDataElement> getPriceGrowthInIntervalsWithDividendsInflAdjusted(@RequestParam(name = "year", required = false, defaultValue = "10") int years) {
        List<SimpleDataElement> asd = getPriceWithReinvestedDividendsInflactionAdjusted();

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < asd.size(); i++) {
            SimpleDataElement currentElement = asd.get(i);
            LocalDate date = LocalDate.parse(currentElement.date);
            LocalDate offsetDate = date.minusMonths((int) (years * 12.0));

            int oldIndex = Helpers.findIndexWithOrBeforeDate(asd, offsetDate);

            if (oldIndex == -1) {
                break;
            }

            Double priceNow = currentElement.value;
            Double priceThen = asd.get(oldIndex).value;

            if (priceThen == null || priceNow == null) {
                break;
            }

            double growth = GrowthCalculator.calculateGrowth(priceNow, priceThen, years);

            result.add(new SimpleDataElement(currentElement.date, growth));
        }

        return result;
    }
}
