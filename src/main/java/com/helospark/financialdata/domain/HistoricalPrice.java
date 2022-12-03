package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.util.List;

public class HistoricalPrice implements Serializable {
    public List<HistoricalPriceElement> historical;
}
