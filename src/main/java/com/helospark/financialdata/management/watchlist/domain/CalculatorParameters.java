package com.helospark.financialdata.management.watchlist.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class CalculatorParameters {
    public Double startMargin;
    public Double endMargin;

    public Double startGrowth;
    public Double endGrowth;

    public Double startShChange;
    public Double endShChange;

    public Double discount;
    public Double endMultiple;

    public Double startPayout;
    public Double endPayout;

    public String type;

    @Override
    public String toString() {
        return "CalculatorParameters [startMargin=" + startMargin + ", endMargin=" + endMargin + ", startGrowth=" + startGrowth + ", endGrowth=" + endGrowth + ", startShChange=" + startShChange
                + ", endShChange=" + endShChange + ", discount=" + discount + ", endMultiple=" + endMultiple + ", startPayout=" + startPayout + ", endPayout=" + endPayout + ", type=" + type + "]";
    }

    public static CalculatorParameters deepClone(CalculatorParameters old) {
        CalculatorParameters result = new CalculatorParameters();

        result.startMargin = old.startMargin;
        result.endMargin = old.endMargin;
        result.startGrowth = old.startGrowth;
        result.endGrowth = old.endGrowth;
        result.startShChange = old.startShChange;
        result.endShChange = old.endShChange;
        result.discount = old.discount;
        result.endMultiple = old.endMultiple;
        result.startPayout = old.startPayout;
        result.endPayout = old.endPayout;
        result.type = old.type;

        return result;
    }

    public Double getStartMargin() {
        return startMargin;
    }

    public void setStartMargin(Double startMargin) {
        this.startMargin = startMargin;
    }

    public Double getEndMargin() {
        return endMargin;
    }

    public void setEndMargin(Double endMargin) {
        this.endMargin = endMargin;
    }

    public Double getStartGrowth() {
        return startGrowth;
    }

    public void setStartGrowth(Double startGrowth) {
        this.startGrowth = startGrowth;
    }

    public Double getEndGrowth() {
        return endGrowth;
    }

    public void setEndGrowth(Double endGrowth) {
        this.endGrowth = endGrowth;
    }

    public Double getStartShChange() {
        return startShChange;
    }

    public void setStartShChange(Double startShChange) {
        this.startShChange = startShChange;
    }

    public Double getEndShChange() {
        return endShChange;
    }

    public void setEndShChange(Double endShChange) {
        this.endShChange = endShChange;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getEndMultiple() {
        return endMultiple;
    }

    public void setEndMultiple(Double endMultiple) {
        this.endMultiple = endMultiple;
    }

    public Double getStartPayout() {
        return startPayout;
    }

    public void setStartPayout(Double startPayout) {
        this.startPayout = startPayout;
    }

    public Double getEndPayout() {
        return endPayout;
    }

    public void setEndPayout(Double endPayout) {
        this.endPayout = endPayout;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
