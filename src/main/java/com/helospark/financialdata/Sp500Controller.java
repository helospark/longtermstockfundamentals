package com.helospark.financialdata;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.helospark.financialdata.domain.EconomicPriceElement;
import com.helospark.financialdata.domain.HistoricalPriceElement;
import com.helospark.financialdata.domain.SimpleDataElement;
import com.helospark.financialdata.domain.SimpleDateDataElement;
import com.helospark.financialdata.domain.ThreeDDataElement;
import com.helospark.financialdata.service.CpiAdjustor;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;

@RestController
@RequestMapping("/sp500/data")
public class Sp500Controller {
    private static final Set<String> VALID_INDICATORS = Set.of("CPI", "15YearFixedRateMortgageAverage", "30YearFixedRateMortgageAverage",
            "unemploymentRate", "consumerSentiment", "realGDP", "GDP");

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

    @GetMapping("/price_infl_adjusted")
    public List<SimpleDataElement> getPriceInflationAdjusted() {
        var asd = StandardAndPoorPerformanceProvider.prices;

        int maxElements = 300;

        int steps = asd.size() / maxElements;

        LocalDate now = LocalDate.now();

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < asd.size(); i += steps) {
            LocalDate dateThen = asd.get(i).date;
            double priceThen = asd.get(i).close;
            double adjustedPriceThen = CpiAdjustor.adjustForInflationToOldDate(priceThen, dateThen, now);

            result.add(new SimpleDataElement(dateThen.toString(), adjustedPriceThen));
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
        int i = 0;
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

    @GetMapping("/price_growth_reinv_dividends_x_yr")
    public List<SimpleDataElement> getPriceGrowthInIntervalsWithDividends(@RequestParam(name = "year", required = false, defaultValue = "10") int years) {
        List<SimpleDataElement> asd = getPriceWithReinvestedDividends();

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

    @GetMapping("/price_for_indicator")
    public List<SimpleDataElement> getPriceCpi(@RequestParam("indicator") String indicator) {
        if (!VALID_INDICATORS.contains(indicator)) {
            throw new RuntimeException("Invalid indicator");
        }

        var asd = StandardAndPoorPerformanceProvider.prices;

        List<EconomicPriceElement> cpi = DataLoader.loadEconomicFile(indicator);

        int maxElements = 300;
        int step = cpi.size() / maxElements;
        if (step == 0) {
            step = 1;
        }

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < cpi.size(); i += step) {
            var cpiElement = cpi.get(i);
            LocalDate date = cpiElement.date;
            int index = Helpers.findIndexWithOrBeforeDate(asd, date);
            if (index != -1) {
                double priceThen = asd.get(index).close;
                result.add(new SimpleDataElement(date.toString(), priceThen));
            } else {
                result.add(new SimpleDataElement(date.toString(), null));
            }
        }
        return result;
    }

    @GetMapping("/indicator")
    public List<SimpleDataElement> getIndicator(@RequestParam("indicator") String indicator) {
        if (!VALID_INDICATORS.contains(indicator)) {
            throw new RuntimeException("Invalid indicator");
        }

        List<EconomicPriceElement> unemployment = DataLoader.loadEconomicFile(indicator);

        int maxElements = 300;
        int step = unemployment.size() / maxElements;
        if (step == 0) {
            step = 1;
        }

        List<SimpleDataElement> result = new ArrayList<>();
        for (int i = 0; i < unemployment.size(); i += step) {
            var cpiElement = unemployment.get(i);
            result.add(new SimpleDataElement(cpiElement.date.toString(), cpiElement.value));
        }
        return result;
    }

    @GetMapping("/xyr_shiller_return")
    public List<ThreeDDataElement> getXYearShillerReturn(@RequestParam(name = "year", required = false, defaultValue = "10") int years) throws IOException {
        return createBubbleChart(years, loadLttFile("/info/sp500_shiller_pe.json"), 50.0);
    }

    @GetMapping("/xyr_pe_return")
    public List<ThreeDDataElement> getXYearPeReturn(@RequestParam(name = "year", required = false, defaultValue = "10") int years) throws IOException {
        return createBubbleChart(years, loadLttFile("/info/sp500_pe.json"), 50.0);
    }

    @GetMapping("/spgdpratio_return")
    public List<ThreeDDataElement> getSpGdpRatioReturn(@RequestParam(name = "year", required = false, defaultValue = "10") int years) throws IOException {
        return createBubbleChart(years, loadLttFile("/info/sp_to_gdp_ratio.json"), 50.0);
    }

    @GetMapping("/buffet_indicator_return")
    public List<ThreeDDataElement> getBuffetIndicator(@RequestParam(name = "year", required = false, defaultValue = "10") int years) throws IOException {
        return createBubbleChart(years, loadLttFile("/info/buffet_indicator.json"), 150.0);
    }

    @GetMapping("/interestrate_return")
    public List<ThreeDDataElement> getInterestRateReturns(@RequestParam(name = "year", required = false, defaultValue = "10") int years) throws IOException {
        return createBubbleChart(years, loadFpFile("/info/federalFunds.json"), 30.0);
    }

    @GetMapping("/inflatation_return")
    public List<ThreeDDataElement> getInflationReturns(@RequestParam(name = "year", required = false, defaultValue = "10") int years) throws IOException {
        return createBubbleChart(years, cpiToInflation(loadFpFile("/info/CPI.json")), 30.0);
    }

    public List<ThreeDDataElement> createBubbleChart(int years, List<SimpleDateDataElement> dataElements, double maxValue) throws IOException, StreamReadException, DatabindException {
        List<SimpleDataElement> spPrices = getPriceWithReinvestedDividends();

        LocalDate lastDate = dataElements.get(0).date;

        List<ThreeDDataElement> result = new ArrayList<>();

        while (lastDate.compareTo(LocalDate.of(1900, 1, 1)) > 0) {
            LocalDate offsetDate = lastDate.minusYears(years);

            int oldShillerIndex = Helpers.findIndexWithOrBeforeDate(dataElements, offsetDate);
            int newShillerIndex = Helpers.findIndexWithOrBeforeDate(dataElements, lastDate);
            if (oldShillerIndex == -1 || oldShillerIndex > dataElements.size() || newShillerIndex == -1 || newShillerIndex > dataElements.size()) {
                break;
            }
            int oldSpIndex = Helpers.findIndexWithOrBeforeDate(spPrices, dataElements.get(oldShillerIndex).date);
            int newSpIndex = Helpers.findIndexWithOrBeforeDate(spPrices, dataElements.get(newShillerIndex).date);

            if (oldSpIndex == -1 || newSpIndex == -1) {
                break;
            }

            double oldShillerValue = dataElements.get(oldShillerIndex).value;
            double oldSpValue = spPrices.get(oldSpIndex).value;
            double newSpValue = spPrices.get(newSpIndex).value;

            if (oldShillerValue <= maxValue) {

                Optional<Double> growth = GrowthCalculator.calculateAnnualGrowth(oldSpValue, offsetDate, newSpValue, lastDate);

                if (growth.isPresent()) {
                    result.add(new ThreeDDataElement(oldShillerValue, growth.get(), 10, dataElements.get(oldShillerIndex).date + ": " + String.format("%.2f", growth.get()) + "%"));
                }
            }
            LocalDate newDate = lastDate.minusMonths(1);

            if (lastDate.equals(newDate)) {
                if (newShillerIndex + 1 >= dataElements.size()) {
                    break;
                }
                newDate = dataElements.get(newShillerIndex + 1).date;
            }

            lastDate = newDate;
        }

        return result;
    }

    public List<SimpleDateDataElement> loadLttFile(String file) throws IOException, StreamReadException, DatabindException {
        ObjectMapper om = new ObjectMapper();
        String[][] shillerRawData = om.readValue(new File(CommonConfig.BASE_FOLDER + file), String[][].class);
        List<SimpleDateDataElement> dataElements = new ArrayList<>();
        for (int i = shillerRawData.length - 1; i >= 0; --i) {
            LocalDate date = LocalDate.parse(shillerRawData[i][0].replaceAll("T.*", ""));
            double value = Double.parseDouble(shillerRawData[i][1]);
            SimpleDateDataElement element = new SimpleDateDataElement(date, value);

            dataElements.add(element);
        }
        return dataElements;
    }

    public List<SimpleDateDataElement> loadFpFile(String file) throws IOException, StreamReadException, DatabindException {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JSR310Module());
        SimpleDateDataElement[] shillerRawData = om.readValue(new File(CommonConfig.BASE_FOLDER + file), SimpleDateDataElement[].class);

        return Arrays.asList(shillerRawData);
    }

    private List<SimpleDateDataElement> cpiToInflation(List<SimpleDateDataElement> loadFpFile) {
        List<SimpleDateDataElement> result = new ArrayList<>();

        int i = 0;
        while (i < loadFpFile.size() - 13) {
            double currentCpi = loadFpFile.get(i).value;
            double previousCpi = loadFpFile.get(i + 12).value;

            double inflation = ((currentCpi - previousCpi) / previousCpi) * 100.0;

            result.add(new SimpleDateDataElement(loadFpFile.get(i).date, inflation));

            ++i;
        }

        return result;
    }

}
