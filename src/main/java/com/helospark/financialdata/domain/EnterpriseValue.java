package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class EnterpriseValue implements DateAware, Serializable {
    public LocalDate date; //2022-09-30,
    public double stockPrice; //46.65,
    public long numberOfShares; //4830826.0,
    //public long marketCapitalization; //225358032.9,
    public long minusCashAndCashEquivalents; //3583539.0,
    public long addTotalDebt; //0.0,
    //public long enterpriseValue; //221774493.9

    @Override
    public LocalDate getDate() {
        return date;
    }
}
