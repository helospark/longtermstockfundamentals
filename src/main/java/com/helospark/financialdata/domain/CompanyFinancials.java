package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CompanyFinancials implements Serializable {
    public double latestPrice;
    public List<FinancialsTtm> financials = new ArrayList<>();
    public Profile profile;

    public CompanyFinancials() {

    }

    public CompanyFinancials(double latestPrice, List<FinancialsTtm> financials, Profile profile) {
        this.latestPrice = latestPrice;
        this.financials = financials;
        this.profile = profile;
    }

}
