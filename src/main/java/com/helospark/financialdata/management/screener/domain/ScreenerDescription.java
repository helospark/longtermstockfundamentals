package com.helospark.financialdata.management.screener.domain;

import com.helospark.financialdata.management.screener.annotation.AtGlanceFormat;
import com.helospark.financialdata.management.screener.annotation.ScreenerElement;

public class ScreenerDescription {
    public String readableName;
    public AtGlanceFormat format;
    public Source source;
    public ScreenerElement annotation;
    public Object data;
    public String allowedValuesJson;

    public static enum Source {
        FIELD,
        METHOD
    }
}
