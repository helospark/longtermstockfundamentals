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

    public static int findIndexWithOrBeforeDate(List<? extends DateAware> elements, LocalDate date) {
        for (int i = 0; i < elements.size(); ++i) {
            LocalDate elementDate = elements.get(i).getDate();
            long daysBetween = daysBetween(date, elementDate);
            if (daysBetween == 0 || (daysBetween < 20 &&
                    (i == elements.size() - 1 || (elements.get(i + 1).getDate().compareTo(date) < 0)))) {
                return i;
            } else if (elementDate.compareTo(date) < 0) {
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
