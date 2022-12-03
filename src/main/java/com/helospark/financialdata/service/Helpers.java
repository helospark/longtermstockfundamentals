package com.helospark.financialdata.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.helospark.financialdata.domain.DateAware;

public class Helpers {

    public static double min(double... asd) {
        double min = asd[0];
        for (int i = 1; i < asd.length; ++i) {
            if (asd[i] < min) {
                min = asd[i];
            }
        }
        return min;
    }

    public static int findIndexWithOrBeforeDate(List<? extends DateAware> cashFlows, LocalDate date) {
        for (int i = 0; i < cashFlows.size(); ++i) {
            LocalDate cashFlowDate = cashFlows.get(i).getDate();
            if (ChronoUnit.DAYS.between(date, cashFlowDate) < 20) {
                return i;
            } else if (cashFlowDate.compareTo(date.minusDays(20)) < 0) {
                return i;
            }
        }
        return -1;
    }

}
