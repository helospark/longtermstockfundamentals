package com.helospark.financialdata.management.watchlist.domain;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class AddToWatchlistExpectationHistoryRequest {
    @NotNull
    public String symbol;
    @NotEmpty
    @NotNull
    public List<String> dates;
    @NotEmpty
    @NotNull
    public List<Double> revenue;
    @NotEmpty
    @NotNull
    public List<Double> eps;
    @NotEmpty
    @NotNull
    public List<Double> margin;
    @NotEmpty
    @NotNull
    public List<Double> shareCount;

    @Override
    public String toString() {
        return "AddToWatchlistExpectationHistoryRequest [symbol=" + symbol + ", dates=" + dates + ", revenue=" + revenue + ", eps=" + eps + ", margin=" + margin + ", shareCount=" + shareCount + "]";
    }

}
