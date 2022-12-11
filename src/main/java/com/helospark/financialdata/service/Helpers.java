package com.helospark.financialdata.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

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
            if (Math.abs(ChronoUnit.DAYS.between(date, cashFlowDate)) < 20) {
                return i;
            } else if (cashFlowDate.compareTo(date) < 0) {
                return i;
            }
        }
        return -1;
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
