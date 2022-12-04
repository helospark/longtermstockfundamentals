package com.helospark.financialdata.util;

import java.util.Set;

import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.util.analyzer.LowTrailingPegScreenerBacktest;

public class CompanyScreener {

    public static void main(String[] args) {
        Set<String> symbols = DataLoader.provideSymbolsFromNasdaqNyse();

        new LowTrailingPegScreenerBacktest().analyze(symbols);
    }

}
