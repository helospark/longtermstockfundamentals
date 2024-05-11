package com.helospark.financialdata.management.watchlist.domain;

import org.hibernate.validator.constraints.Range;

public class Moats {
    @Range(min = 0, max = 5)
    public int networkEffect;
    @Range(min = 0, max = 5)
    public int switchingCost;
    @Range(min = 0, max = 5)
    public int economyOfScale;
    @Range(min = 0, max = 5)
    public int brand;
    @Range(min = 0, max = 5)
    public int intangibles;
    @Range(min = 0, max = 5)
    public int costAdvantage;

    @Override
    public String toString() {
        return "Moats [networkEffect=" + networkEffect + ", switchingCost=" + switchingCost + ", economyOfScale=" + economyOfScale + ", brand=" + brand + ", intangibles=" + intangibles
                + ", costAdvantage=" + costAdvantage + "]";
    }

}
