package com.helospark.financialdata.management.screener.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScreenerResult {
    public boolean hasMoreResults;
    public List<String> columns = new ArrayList<>();
    public List<Map<String, String>> portfolio = new ArrayList<>();
}
