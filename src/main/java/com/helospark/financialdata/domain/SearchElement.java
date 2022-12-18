package com.helospark.financialdata.domain;

import java.util.Objects;

public class SearchElement {
    public String symbol;
    public String name;

    public SearchElement(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
    }

    @Override
    public String toString() {
        return "SearchElement [symbol=" + symbol + ", name=" + name + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SearchElement other = (SearchElement) obj;
        return Objects.equals(symbol, other.symbol);
    }

}
