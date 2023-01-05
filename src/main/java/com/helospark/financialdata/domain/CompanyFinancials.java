package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CompanyFinancials implements Serializable {
    public double latestPrice;
    public double latestPriceUsd;
    public List<FinancialsTtm> financials = new ArrayList<>();
    public Profile profile;
    public int dataQualityIssue;

    public CompanyFinancials() {

    }

    public CompanyFinancials(double latestPrice, double latestPriceUsd, List<FinancialsTtm> financials, Profile profile, int dataQualityIssue) {
        this.latestPrice = latestPrice;
        this.latestPriceUsd = latestPriceUsd;
        this.financials = financials;
        this.profile = profile;
        this.dataQualityIssue = dataQualityIssue;
    }

}
