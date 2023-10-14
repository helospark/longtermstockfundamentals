package com.helospark.financialdata.domain;

public class FxSymbol {
    public String code;
    public String description;

    public FxSymbol(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public FxSymbol() {
    }

}
