package com.helospark.financialdata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.DoubleStream;

import com.helospark.financialdata.management.screener.annotation.ScreenerElement;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.GrowthStandardDeviationCounter;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class GrowthStatisticsCreator {
    public static final int YEAR_CUTOFF = 20;

    public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, IOException, NoSuchFieldException, SecurityException {
        SymbolAtGlanceProvider provider = new SymbolAtGlanceProvider();

        Set<String> usSymbols = DataLoader.provideSymbolsFromNasdaqNyse();
        Map<String, AtGlanceData> growthStocks = new HashMap<>();

        int fifteenYearsAgo = LocalDate.now().minusYears(YEAR_CUTOFF).getYear();
        Map<String, AtGlanceData> fifteenYearOldData = provider.loadAtGlanceDataAtYear(fifteenYearsAgo).get();
        for (var symbol : usSymbols) {
            Optional<AtGlanceData> data = provider.getAtGlanceData(symbol);

            if (data.isPresent() && isDataMatch(data.get())) {

                if (fifteenYearOldData.containsKey(symbol) && oldDataFilter(fifteenYearOldData.get(symbol))) {
                    growthStocks.put(symbol, fifteenYearOldData.get(symbol));
                }
            }
        }

        for (int i = 2018; i < 2022; ++i) {
            System.out.println(i);
            Map<String, AtGlanceData> yearData = provider.loadAtGlanceDataAtYear(i).get();
            int oldYear = LocalDate.of(i, 1, 1).minusYears(YEAR_CUTOFF).getYear();
            Map<String, AtGlanceData> oldData = provider.loadAtGlanceDataAtYear(oldYear).get();
            for (var symbol : usSymbols) {
                AtGlanceData data = yearData.get(symbol);

                if (data != null && isDataMatch(data)) {

                    if (oldData.containsKey(symbol) && oldDataFilter(oldData.get(symbol))) {
                        growthStocks.put(symbol + "_" + i, oldData.get(symbol));
                    }
                }
            }
        }

        aggregateData(growthStocks);
    }

    public static boolean isDataMatch(AtGlanceData data) {
        return data.price20Gr < 0.0;
    }

    public static boolean oldDataFilter(AtGlanceData data) {
        return data.eps > 0 && data.marketCapUsd > 300;
    }

    private static void aggregateData(Map<String, AtGlanceData> growthStocks) throws IllegalArgumentException, IllegalAccessException, IOException, NoSuchFieldException, SecurityException {
        Map<String, AggregateData> aggregateData = new LinkedHashMap<>();
        for (var field : AtGlanceData.class.getDeclaredFields()) {
            if (field.getType().isPrimitive()) {
                aggregateData.put(field.getName(), new AggregateData());
            }
        }

        for (var glance : growthStocks.entrySet()) {
            for (var field : AtGlanceData.class.getDeclaredFields()) {
                if (field.getType().isPrimitive()) {
                    Object object = field.get(glance.getValue());
                    double value = convertNumberToType(object);

                    AggregateData aggregate = aggregateData.get(field.getName());

                    if (Double.isFinite(value)) {
                        aggregate.datas.add(value);
                    } else {
                        aggregate.numberOfUnknowns++;
                    }
                }
            }
        }
        String outputCsv = "";

        outputCsv += ("data,avg,clearAvg,median,min,max,standarddev,p25,p75,p90,unknown\n");

        System.out.println("data\t\t\tavg\tclearAvg\tmedian\tmin\tmax\tstandarddev\tunknown");
        for (var entry : aggregateData.entrySet()) {
            var value = entry.getValue();
            ScreenerElement screenerElement = AtGlanceData.class.getField(entry.getKey()).getAnnotation(ScreenerElement.class);
            if (screenerElement != null) {
                System.out.printf("%s\t\t\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%d\n",
                        entry.getKey(),
                        value.getAvg(),
                        value.getAvgWithoutExtremes(),
                        value.getMedian(),
                        value.getMin(),
                        value.getMax(),
                        value.getSd(),
                        value.numberOfUnknowns);
                outputCsv += (screenerElement.name() + "," +
                        value.getAvg() + "," +
                        value.getAvgWithoutExtremes() + "," +
                        value.getMedian() + "," +
                        value.getMin() + "," +
                        value.getMax() + "," +
                        value.getSd() + "," +
                        value.getP25() + "," +
                        value.getP75() + "," +
                        value.getP90() + "," +
                        value.numberOfUnknowns + "\n");
            }
        }

        FileOutputStream fos = new FileOutputStream(new File("/tmp/stats.csv"));
        fos.write(outputCsv.getBytes());
    }

    public static double convertNumberToType(Object value) {
        if (value.getClass().equals(Double.class)) {
            return (Double) value;
        } else if (value.getClass().equals(Float.class)) {
            return ((Float) value).doubleValue();
        } else if (value.getClass().equals(Byte.class)) {
            return ((Byte) value).doubleValue();
        } else if (value.getClass().equals(Integer.class)) {
            return ((Integer) value).doubleValue();
        } else if (value.getClass().equals(Long.class)) {
            return ((Long) value).doubleValue();
        } else if (value.getClass().equals(Short.class)) {
            return ((Short) value).doubleValue();
        } else {
            throw new RuntimeException("Unknown type " + value.getClass());
        }
    }

    static class AggregateData {
        public List<Double> datas = new ArrayList<>();
        public int numberOfUnknowns = 0;

        public Double getMin() {
            return doubleStream().min().orElse(-1);
        }

        public double getMax() {
            return doubleStream().max().orElse(-1);
        }

        public double getAvg() {
            return doubleStream().average().orElse(-1);
        }

        public double getMedian() {
            List<Double> medianList = medianList();

            if (medianList.size() == 0) {
                return -1.0;
            } else {
                return medianList.get(medianList.size() / 2);
            }
        }

        public double getAvgWithoutExtremes() {
            List<Double> medianList = medianList();

            int amountToRemove = medianList.size() / 5;

            List<Double> newList = new ArrayList<>();
            for (int i = amountToRemove; i < medianList.size() - amountToRemove; ++i) {
                newList.add(medianList.get(i));
            }

            return newList.stream().mapToDouble(a -> a).average().orElse(-1);
        }

        public double getSd() {
            return GrowthStandardDeviationCounter.getArraySD(medianList());
        }

        public double getP90() {
            List<Double> medianList = medianList();
            int p90 = (int) (medianList.size() * 0.9);
            if (medianList.size() == 0) {
                return -1;
            }
            return medianList.get(p90);
        }

        public double getP25() {
            List<Double> medianList = medianList();
            int p25 = (int) (medianList.size() * 0.25);

            if (medianList.size() == 0) {
                return -1;
            }

            return medianList.get(p25);
        }

        public double getP75() {
            List<Double> medianList = medianList();
            int p75 = (int) (medianList.size() * 0.75);
            if (medianList.size() == 0) {
                return -1;
            }
            return medianList.get(p75);
        }

        public List<Double> medianList() {
            List<Double> medianList = new ArrayList<>(datas);
            Collections.sort(medianList);
            return medianList;
        }

        public DoubleStream doubleStream() {
            return datas.stream().mapToDouble(a -> a);
        }
    }

}
