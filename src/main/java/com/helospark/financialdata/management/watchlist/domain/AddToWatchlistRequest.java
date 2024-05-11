package com.helospark.financialdata.management.watchlist.domain;

import java.util.List;

public class AddToWatchlistRequest {
    public String symbol;
    public Double priceTarget;
    public List<String> tags;
    public String notes;
    public int ownedShares = 0;
    public CalculatorParameters calculatorParameters;
    public Moats moats;

    @Override
    public String toString() {
        return "AddToWatchlistRequest [symbol=" + symbol + ", priceTarget=" + priceTarget + ", tags=" + tags + ", notes=" + notes + ", ownedShares=" + ownedShares + ", calculatorParameters="
                + calculatorParameters + ", moats=" + moats + "]";
    }

}
