package com.helospark.financialdata.management.watchlist.domain.datatables;

import java.util.List;

public class DataTableRequest {
    public int draw;
    public List<Column> columns;
    public List<Order> order;
    public int start;
    public int length;
    public Search search;

}
