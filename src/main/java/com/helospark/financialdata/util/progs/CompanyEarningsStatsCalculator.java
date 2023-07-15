package com.helospark.financialdata.util.progs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.service.exchanges.ExchangeRegion;
import com.helospark.financialdata.service.exchanges.Exchanges;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class CompanyEarningsStatsCalculator {

    public static void main(String[] args) {
        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();
        LinkedHashMap<String, AtGlanceData> data = symbolAtGlanceProvider.getSymbolCompanyNameCache();

        System.out.println("10 year stats:");
        calculateStats(data, entry -> entry.price10Gr);
        System.out.println("15 year stats:");
        calculateStats(data, entry -> entry.price15Gr);
        System.out.println("20 year stats:");
        calculateStats(data, entry -> entry.price20Gr);
    }

    public static void calculateStats(LinkedHashMap<String, AtGlanceData> data, Function<AtGlanceData, Float> mappings) {
        TreeMap<Float, String> map = new TreeMap<Float, String>();
        for (var symbol : DataLoader.provideSymbolsIn(Exchanges.getExchangesByRegion(ExchangeRegion.US))) {
            AtGlanceData atGlanceData = data.get(symbol);
            if (atGlanceData != null) {
                map.put(mappings.apply(atGlanceData), symbol);
            }
        }
        List<Double> returns = map.keySet()
                .stream()
                .filter(a -> a != null)
                .filter(a -> !a.isNaN())
                .filter(a -> Float.isFinite(a))
                .filter(a -> Math.abs(a) < 100.0)
                .map(a -> a.doubleValue())
                .collect(Collectors.toList());
        double avg10Return = returns.stream()
                .mapToDouble(a -> a)
                .average()
                .getAsDouble();
        double medianReturn = returns.get(returns.size() / 2);

        int i = 0;
        for (i = 0; i < returns.size(); ++i) {
            if (returns.get(i) > 10.0) {
                break;
            }
        }

        System.out.println("Avg return: " + avg10Return + "; median: " + medianReturn);
        System.out.println("Overperforming percent: " + ((returns.size() - i) * 100.0 / returns.size()));
        System.out.println("p90: " + getTop(returns, 90));
        System.out.println("p75: " + getTop(returns, 75));
        System.out.println("p50: " + getTop(returns, 50));
        System.out.println("p30: " + getTop(returns, 30));
        System.out.println("p20: " + getTop(returns, 20));
        System.out.println("p10: " + getTop(returns, 10));
        System.out.println("p1: " + getTop(returns, 1));
        System.out.println("p0.1: " + getTop(returns, 0.1));
        System.out.println();
    }

    public static Double getTop(List<Double> returns, double percent) {
        return returns.get((int) (returns.size() * (1.0 - (percent / 100.0))));
    }

}
