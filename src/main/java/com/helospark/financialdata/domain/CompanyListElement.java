package com.helospark.financialdata.domain;

import java.io.Serializable;

public class CompanyListElement implements Serializable {
    String symbol;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

}
