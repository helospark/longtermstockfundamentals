package com.helospark.financialdata.management.screener.annotation;

import java.util.function.Function;

public enum AtGlanceFormat {
    SIMPLE_NUMBER(data -> String.format("%.2f", data)),
    PERCENT(data -> String.format("%.2f %%", data)),
    PERCENT_0_TO_1(data -> String.format("%.2f %%", data * 100.0));

    Function<Double, String> formatter;

    AtGlanceFormat(Function<Double, String> formatter) {
        this.formatter = formatter;
    }

    public String format(Double data) {
        return formatter.apply(data);
    }

}
