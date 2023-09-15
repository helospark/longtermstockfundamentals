package com.helospark.financialdata.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import com.helospark.financialdata.management.screener.annotation.ScreenerElement;
import com.helospark.financialdata.management.screener.domain.ScreenerDescription;
import com.helospark.financialdata.management.screener.domain.ScreenerDescription.Source;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

/**
 * Prints more result from the result of ParameterFinderBacktest.
 */
public class StockAtGlanceCsvPrinter {

    private static final String INPUT = "ENPH, ALTEO.BD, SWKS, TSM, INTC, QCOM, MED, MBUU, GOOGL, ULTA, VOW3.DE, META, VWCE.DE, TSCO, MAST.BD, V, BABA, UNH, EXPD, MEDP, GSP.BD, FTNT, ELV, IBP, SNA, EPAM, HUF=X, OTP.BD, QLYS";

    static Map<String, ScreenerDescription> idToDescription = new LinkedHashMap<>();

    public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException, InvocationTargetException {
        for (var field : AtGlanceData.class.getDeclaredFields()) {
            ScreenerElement screenerElement = field.getAnnotation(ScreenerElement.class);
            if (screenerElement != null) {
                field.setAccessible(true); // optimization
                String name = screenerElement.name();
                ScreenerDescription description = new ScreenerDescription();
                description.readableName = name;
                description.format = screenerElement.format();
                description.source = Source.FIELD;
                description.data = field;

                String id = screenerElement.id().equals("") ? field.getName() : screenerElement.id();

                idToDescription.put(id, description);
            }
        }
        for (var method : AtGlanceData.class.getDeclaredMethods()) {
            ScreenerElement screenerElement = method.getAnnotation(ScreenerElement.class);
            if (screenerElement != null && method.getParameterCount() == 0) {
                method.setAccessible(true); // optimization
                String name = screenerElement.name();
                ScreenerDescription description = new ScreenerDescription();
                description.readableName = name;
                description.format = screenerElement.format();
                description.source = Source.METHOD;
                description.data = method;

                String id = screenerElement.id().equals("") ? method.getName() : screenerElement.id();

                idToDescription.put(id, description);
            }
        }

        printStockSummary(INPUT);
    }

    public static void printStockSummary(String input) throws IllegalAccessException, NoSuchFieldException, IOException, InvocationTargetException {
        String[] stocks = input.split(", ");

        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();
        LinkedHashMap<String, AtGlanceData> data = symbolAtGlanceProvider.getSymbolCompanyNameCache();

        AtGlanceData.class.getFields();

        String result = "";
        result += String.format("%-15s;", "stock");
        for (var column : idToDescription.keySet()) {
            result += String.format(column + ";");
        }
        result += "companyName";
        result += "\n";

        for (var stock : stocks) {
            String ticker;
            if (stock.indexOf("(") != -1) {
                ticker = stock.substring(0, stock.indexOf("("));
            } else {
                ticker = stock;
            }
            AtGlanceData atGlance = data.get(ticker);
            if (atGlance != null) {
                result += String.format("%-15s;", stock);

                for (var column : idToDescription.entrySet()) {
                    result += String.format("%.2f;", getValueAsDouble(atGlance, column.getKey(), column.getValue()));
                }
                result += String.format("%s", atGlance.companyName);
                result += "\n";
            }
        }
        try (FileOutputStream fos = new FileOutputStream(new File("/tmp/summary.csv"))) {
            fos.write(result.getBytes());
        }
    }

    private static Object getValueAsDouble(AtGlanceData glance, String key, ScreenerDescription screenerDescriptor) throws IllegalAccessException, InvocationTargetException {
        if (screenerDescriptor.source.equals(Source.FIELD)) {
            Object value = ((Field) screenerDescriptor.data).get(glance);
            return convertNumberToType(value);
        } else if (screenerDescriptor.source.equals(Source.METHOD)) {
            Object value = ((Method) screenerDescriptor.data).invoke(glance);
            return convertNumberToType(value);
        } else {
            throw new RuntimeException("Unknown source");
        }
    }

    public static double convertNumberToType(Object value) {
        if (value == null) {
            return Double.NaN;
        }
        if (value.getClass().equals(Double.class)) {
            return (Double) value;
        } else if (value.getClass().equals(Float.class)) {
            return ((Float) value).doubleValue();
        } else if (value.getClass().equals(Byte.class)) {
            return ((Byte) value).doubleValue();
        } else if (value.getClass().equals(Integer.class)) {
            return ((Integer) value).doubleValue();
        } else if (value.getClass().equals(Short.class)) {
            return ((Short) value).doubleValue();
        } else {
            throw new RuntimeException("Unknown type");
        }
    }

    public static Double getFieldAsDouble(AtGlanceData atGlance, String column) throws IllegalAccessException, NoSuchFieldException {
        Object value = AtGlanceData.class.getField(column).get(atGlance);

        if (value.getClass().equals(Double.class)) {
            return (Double) value;
        } else if (value.getClass().equals(Float.class)) {
            return ((Float) value).doubleValue();
        } else if (value.getClass().equals(Byte.class)) {
            return ((Byte) value).doubleValue();
        } else if (value.getClass().equals(Integer.class)) {
            return ((Integer) value).doubleValue();
        } else if (value.getClass().equals(Short.class)) {
            return ((Short) value).doubleValue();
        } else {
            throw new RuntimeException("Unknown type");
        }
    }

}
