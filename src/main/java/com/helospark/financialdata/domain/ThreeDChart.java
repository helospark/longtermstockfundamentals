package com.helospark.financialdata.domain;

import java.util.List;

public class ThreeDChart {
    public List<ThreeDDataElement> data;
    public ChartAnnotation annotations = new ChartAnnotation(List.of());

    public ThreeDChart(List<ThreeDDataElement> data) {
        this.data = data;
    }

    public ThreeDChart(List<ThreeDDataElement> data, ChartAnnotation annotations) {
        this.data = data;
        this.annotations = annotations;
    }

}
