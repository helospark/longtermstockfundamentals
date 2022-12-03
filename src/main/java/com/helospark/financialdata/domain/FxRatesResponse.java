package com.helospark.financialdata.domain;

import java.util.Map;

public class FxRatesResponse {
    public boolean success;
    public Map<String, Map<String, Double>> rates;
}
