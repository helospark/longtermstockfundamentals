package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuxilaryInformation implements DateAware, Serializable {
    @JsonProperty("d")
    public LocalDate date; //2022-09-30,
    @JsonProperty("ib")
    public int insiderBoughtShares;
    @JsonProperty("is")
    public int insiderSoldShares;

    @JsonProperty("sb")
    public int senateBoughtDollar;
    @JsonProperty("ss")
    public int senateSoldDollar;

    @JsonProperty("es")
    public int earnSurprisePercent;

    @JsonProperty("ec")
    public int employeeCount;

    @Override
    public LocalDate getDate() {
        return date;
    }
}
