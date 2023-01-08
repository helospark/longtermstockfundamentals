package com.helospark.financialdata.management.watchlist.domain;

import java.util.List;
import java.util.Map;

public class WatchListResponse {
    public List<String> columns = List.of();
    public List<Map<String, String>> portfolio = List.of();
}
