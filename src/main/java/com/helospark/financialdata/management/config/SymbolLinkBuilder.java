package com.helospark.financialdata.management.config;

public class SymbolLinkBuilder {

    public static String createSymbolLink(String ticker) {
        return "<a href=\"/stock/" + ticker + "\">" + ticker + "</a>";
    }

}
