package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CompanyFinancials implements Serializable {
    public double latestPrice;
    public List<FinancialsTtm> financials = new ArrayList<>();

    public CompanyFinancials() {

    }

    public CompanyFinancials(double latestPrice, List<FinancialsTtm> financials) {
        this.latestPrice = latestPrice;
        this.financials = financials;
    }

}
