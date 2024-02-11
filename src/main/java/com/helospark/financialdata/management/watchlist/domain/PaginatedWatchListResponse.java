package com.helospark.financialdata.management.watchlist.domain;

import java.util.ArrayList;
import java.util.List;

public class PaginatedWatchListResponse {
    public int draw;
    public int recordsTotal;
    public int recordsFiltered;

    public List<String> columns = new ArrayList<>();
    public List<List<String>> data = new ArrayList<>();
}
