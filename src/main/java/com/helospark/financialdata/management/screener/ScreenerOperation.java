package com.helospark.financialdata.management.screener;

public class ScreenerOperation {
    public String id;
    public String operation;
    public Double number1;
    public Double number2;

    @Override
    public String toString() {
        return "ScreenerOperation [id=" + id + ", operation='" + operation + "', number1=" + number1 + ", number2=" + number2 + "]";
    }

}
