package com.helospark.financialdata.domain;

import java.util.List;

public class ChartAnnotation {
    public List<ChartLine> verticalLines;

    public ChartAnnotation(List<ChartLine> verticalLines) {
        this.verticalLines = verticalLines;
    }

    public static class ChartLine {
        public double value;
        public String description;

        public ChartLine(double value, String description) {
            this.value = value;
            this.description = description;
        }

    }
}
