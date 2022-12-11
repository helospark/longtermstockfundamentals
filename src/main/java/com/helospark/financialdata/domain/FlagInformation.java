package com.helospark.financialdata.domain;

public class FlagInformation {
    public FlagType type;
    public String text;

    public FlagInformation() {

    }

    public FlagInformation(FlagType type, String text) {
        this.type = type;
        this.text = text;
    }

}
