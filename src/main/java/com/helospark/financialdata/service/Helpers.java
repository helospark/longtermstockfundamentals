package com.helospark.financialdata.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helospark.financialdata.domain.DateAware;

public class Helpers {
    private static final Logger LOGGER = LoggerFactory.getLogger(Helpers.class);

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
        if (cashFlows.size() == 0) {
            return -1;
        }
        if (ChronoUnit.DAYS.between(cashFlows.get(0).getDate(), date) > 6 * 30) {
            //LOGGER.warn("Company may be delisted already, the latest report is more than 6 month old date={}", cashFlows.get(0).getDate());
            //return -1;
        }

        for (int i = 0; i < cashFlows.size(); ++i) {
            LocalDate cashFlowDate = cashFlows.get(i).getDate();
            if (daysBetween(date, cashFlowDate) < 20) {
                return i;
            } else if (cashFlowDate.compareTo(date) < 0) {
                return i;
            }
        }
        return -1;
    }

    public static long daysBetween(LocalDate date1, LocalDate date2) {
        return Math.abs(ChronoUnit.DAYS.between(date1, date2));
    }

    public static int findIndexWithOrBeforeDateSafe(List<? extends DateAware> cashFlows, LocalDate date) {
        int value = findIndexWithOrBeforeDate(cashFlows, date);
        if (value == -1) {
            return cashFlows.size() - 1;
        } else {
            return value;
        }
    }

    static class CacheKey {
        List<? extends DateAware> cashFlows;
        LocalDate date;

        public CacheKey(List<? extends DateAware> cashFlows, LocalDate date) {
            this.cashFlows = cashFlows;
            this.date = date;
        }

        @Override
        public int hashCode() {
            return Objects.hash(cashFlows, date);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CacheKey other = (CacheKey) obj;
            return Objects.equals(cashFlows, other.cashFlows) && Objects.equals(date, other.date);
        }

    }

}
