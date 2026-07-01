package com.helospark.financialdata.management.screener.annotation;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.helospark.financialdata.management.screener.strategy.ScreenerColumnListProvider;

public enum AtGlanceFormat {
    SIMPLE_NUMBER(data -> String.format("%,.2f", data)),
    PERCENT(data -> String.format("%,.2f %%", data)),
    PERCENT_0_TO_1(data -> String.format("%,.2f %%", data * 100.0)),
    MILLION_DOLLAR(data -> String.format("%,.2f", data)),
    STRING(data -> String.valueOf(data)),
    LIST_PROVIDER(data -> String.valueOf(data));

    private static final String UNKNOWN_CATEGORY = "Unknown";
    Function<Double, String> formatter;

    AtGlanceFormat(Function<Double, String> formatter) {
        this.formatter = formatter;
    }

    public String format(Double data, ScreenerElement annotation) {
        if (this == LIST_PROVIDER) {
            Map<String, Map<String, Integer>> elements = ScreenerColumnListProvider.provideValues();
            Map<String, Integer> map = elements.get(annotation.listProvider());

            if (map == null) {
                return UNKNOWN_CATEGORY;
            } else {
                Map<Integer, String> inverseMap = map.entrySet().stream().collect(Collectors.toMap(entry -> entry.getValue(), entry -> entry.getKey()));
                String result = inverseMap.get((int) Math.round(data));
                if (result == null) {
                    return UNKNOWN_CATEGORY;
                } else {
                    return result;
                }
            }
        }

        return formatter.apply(data);
    }

}
