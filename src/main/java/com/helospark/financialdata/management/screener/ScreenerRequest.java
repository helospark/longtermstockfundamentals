package com.helospark.financialdata.management.screener;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public class ScreenerRequest {
    @NotNull
    public List<ScreenerOperation> operations;

    public List<String> exchanges;

    public String lastItem;
    public String prevItem;

    public LocalDate onDate;

    @Override
    public String toString() {
        return "ScreenerRequest [operations=" + operations + ", exchanges=" + exchanges + ", lastItem=" + lastItem + ", prevItem=" + prevItem + ", onDate=" + onDate + "]";
    }

}
