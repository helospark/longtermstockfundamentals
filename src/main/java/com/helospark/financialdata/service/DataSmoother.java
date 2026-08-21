package com.helospark.financialdata.service;

import java.util.ArrayList;
import java.util.List;

import com.helospark.financialdata.domain.SimpleDataElement;

public class DataSmoother {

    public static List<SimpleDataElement> smoothSMA(List<SimpleDataElement> data, int windowSize) {
        if (data == null || data.isEmpty() || windowSize <= 1) {
            return new ArrayList<>(data != null ? data : List.of());
        }

        List<SimpleDataElement> smoothed = new ArrayList<>();
        int halfWindow = windowSize / 2;

        for (int i = 0; i < data.size(); i++) {
            double sum = 0.0;
            int count = 0;

            int start = Math.max(0, i - halfWindow);
            int end = Math.min(data.size() - 1, i + halfWindow);

            for (int j = start; j <= end; j++) {
                Double val = data.get(j).value;
                if (val != null && !Double.isNaN(val)) {
                    sum += val;
                    count++;
                }
            }

            Double average = (count > 0) ? (sum / count) : null;
            smoothed.add(new SimpleDataElement(data.get(i).date, average));
        }

        return smoothed;
    }
}
