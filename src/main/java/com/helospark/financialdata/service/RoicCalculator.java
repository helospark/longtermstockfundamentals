package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.FinancialsTtm;

public class RoicCalculator {

    public static Optional<Double> getAverageRoic(List<FinancialsTtm> financials, double offset) {
        int oldIndex = findIndexWithOrBeforeDate(financials, CommonConfig.NOW.minusMonths((long) (offset * 12.0)));

        if (oldIndex == -1) {
            return Optional.empty();
        }

        List<Double> roics = new ArrayList<>();
        for (int i = oldIndex; i < oldIndex + (5 * 4) && i < financials.size(); ++i) {
            double roic = financials.get(i).keyMetrics.roic;
            roics.add(roic);
        }
        Collections.sort(roics);
        return Optional.of(roics.get(roics.size() / 2));
    }

}
