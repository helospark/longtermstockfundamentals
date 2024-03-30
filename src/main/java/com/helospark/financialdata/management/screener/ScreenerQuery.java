package com.helospark.financialdata.management.screener;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public class ScreenerQuery {
    public String id;
    @NotNull
    public List<ScreenerOperation> operations;

    public List<String> exchanges;

    @Override
    public String toString() {
        return "ScreenerRequest [id=" + id + ", operations=" + operations + ", exchanges=" + exchanges + "]";
    }

}
