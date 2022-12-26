package com.helospark.financialdata.management.user.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public class FreeStockRepository {

    public List<String> getFreeSockList() {
        return List.of(
                "AAPL",
                "INTC",
                "NVDA",
                "TSLA",
                "VOW3.DE");
    }

}
