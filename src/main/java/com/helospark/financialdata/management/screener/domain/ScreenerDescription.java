package com.helospark.financialdata.management.screener.domain;

import com.helospark.financialdata.management.screener.annotation.AtGlanceFormat;

public class ScreenerDescription {
    public String readableName;
    public AtGlanceFormat format;
    public Source source;
    public Object data;

    public static enum Source {
        FIELD,
        METHOD
    }
}
