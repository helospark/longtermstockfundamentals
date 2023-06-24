package com.helospark.financialdata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.helospark.financialdata.management.screener.annotation.ScreenerElement;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class NNDataCreator {
    private static final double MISSING_DATA = -10.0;

    public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
        SymbolAtGlanceProvider provider = new SymbolAtGlanceProvider();
        List<String> columns = new ArrayList<>();
        List<List<String>> csv = new ArrayList<>();
        int atYear = 2012;
        Map<String, AtGlanceData> oldData = provider.loadAtGlanceDataAtYear(atYear, 1).get();
        Map<String, AtGlanceData> latestData = provider.getSymbolCompanyNameCache();
        Set<String> usSymbols = DataLoader.provideSymbolsFromNasdaqNyse();

        Field[] fields = AtGlanceData.class.getDeclaredFields();
        for (var field : fields) {
            ScreenerElement annotation = field.getAnnotation(ScreenerElement.class);
            if (annotation != null) {
                columns.add(field.getName());
            }
        }
        columns.add("result_bad");
        columns.add("result_negative");
        columns.add("result_underperformer");
        columns.add("result_overperformer");
        columns.add("result_good");

        for (var symbol : usSymbols) {
            var entry = oldData.get(symbol);
            if (entry == null || entry.marketCapUsd < 300 || latestData.get(symbol) == null || !Float.isFinite(latestData.get(symbol).price10Gr)) {
                continue;
            }
            List<String> csvLine = new ArrayList<>();
            csv.add(csvLine);

            for (var column : columns) {
                if (column.startsWith("result")) {
                    if (column.equals("result_bad")) {
                        AtGlanceData latestGlance = latestData.get(symbol);

                        List<Double> result = convertResult(latestGlance);
                        for (var a : result) {
                            csvLine.add(a.toString());
                        }
                    }
                } else {
                    Field field = AtGlanceData.class.getField(column);
                    double fieldValue = convertNumberToType(field.get(entry));

                    if (Double.isFinite(fieldValue)) {
                        csvLine.add(String.valueOf(fieldValue));
                    } else {
                        csvLine.add(String.valueOf(MISSING_DATA));
                    }
                }
            }

        }

        writeCsv("/tmp/nn.csv", columns, csv);

    }

    private static List<Double> convertResult(AtGlanceData latestGlance) {
        float tenYearGrowth = latestGlance.price10Gr;

        if (!Float.isFinite(tenYearGrowth)) {
            return List.of(0.0, 0.0, 0.0, 0.0, 0.0);
        } else if (tenYearGrowth < -5) {
            return List.of(1.0, 0.0, 0.0, 0.0, 0.0);
        } else if (tenYearGrowth < 0) {
            return List.of(0.0, 1.0, 0.0, 0.0, 0.0);
        } else if (tenYearGrowth < 10) {
            return List.of(0.0, 0.0, 1.0, 0.0, 0.0);
        } else if (tenYearGrowth < 15) {
            return List.of(0.0, 0.0, 0.0, 1.0, 0.0);
        } else {
            return List.of(0.0, 0.0, 0.0, 0.0, 1.0);
        }
    }

    private static void writeCsv(String string, List<String> columns, List<List<String>> csv) throws IOException {
        File file = new File(string);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            String line = createCsvLine(columns);
            fos.write(line.getBytes());

            for (var csvLine : csv) {
                String line2 = createCsvLine(csvLine);
                fos.write(line2.getBytes());
            }
        }
    }

    public static String createCsvLine(List<String> columns) {
        return columns.stream().collect(Collectors.joining(",")) + "\n";
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

}
