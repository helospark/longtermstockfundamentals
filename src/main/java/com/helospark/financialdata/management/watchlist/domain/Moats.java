package com.helospark.financialdata.management.watchlist.domain;

import org.hibernate.validator.constraints.Range;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
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

    public int getNetworkEffect() {
        return networkEffect;
    }

    public void setNetworkEffect(int networkEffect) {
        this.networkEffect = networkEffect;
    }

    public int getSwitchingCost() {
        return switchingCost;
    }

    public void setSwitchingCost(int switchingCost) {
        this.switchingCost = switchingCost;
    }

    public int getEconomyOfScale() {
        return economyOfScale;
    }

    public void setEconomyOfScale(int economyOfScale) {
        this.economyOfScale = economyOfScale;
    }

    public int getBrand() {
        return brand;
    }

    public void setBrand(int brand) {
        this.brand = brand;
    }

    public int getIntangibles() {
        return intangibles;
    }

    public void setIntangibles(int intangibles) {
        this.intangibles = intangibles;
    }

    public int getCostAdvantage() {
        return costAdvantage;
    }

    public void setCostAdvantage(int costAdvantage) {
        this.costAdvantage = costAdvantage;
    }

}
