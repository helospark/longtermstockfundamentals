package com.helospark.financialdata.domain;

import java.util.Map;

public class FxSupportedSymbolsResponse {
    public boolean success;
    public Map<String, FxSymbol> symbols;
}
