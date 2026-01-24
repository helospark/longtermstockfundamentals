package com.helospark.financialdata.util.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.SmoothnessCalculator;

public class SmoothnessTest {

    public static void main(String[] args) {
        Set<String> symbols = DataLoader.provideSp500Symbols();

        List<StringDoublePair> result = new ArrayList<>();
        for (String symbol : symbols) {
            result.add(new StringDoublePair(symbol, calculateSmoothness(symbol)));
        }
        Collections.sort(result, (a, b) -> Double.compare(b.value, a.value));

        for (var entry : result) {
            System.out.printf("%s\t%.2f\n", entry.symbol, entry.value);
        }
    }

    public static double calculateSmoothness(String symbol) {
        CompanyFinancials company = DataLoader.readFinancials(symbol);

        return SmoothnessCalculator.calculateSmoothnessOfFcf(company);

    }

    static class StringDoublePair {
        String symbol;
        double value;

        public StringDoublePair(String symbol, double value) {
            this.symbol = symbol;
            this.value = value;
        }

    }

}
