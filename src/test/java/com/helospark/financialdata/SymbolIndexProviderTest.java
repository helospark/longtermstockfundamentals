package com.helospark.financialdata;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.helospark.financialdata.domain.SearchElement;
import com.helospark.financialdata.service.SymbolIndexProvider;

public class SymbolIndexProviderTest {
    static SymbolIndexProvider underTest;

    @BeforeAll
    public static void setUp() {
        underTest = new SymbolIndexProvider();
    }

    @Test
    public void testSymbols() {

        List<SearchElement> result = underTest.getTopResult("AAP");

        Assertions.assertEquals("AAP", result.get(0).symbol);
        Assertions.assertEquals("AAPL", result.get(1).symbol);
    }

    @Test
    public void performance() {
        int iteration = 3000;
        Random random = new Random();
        long start = System.currentTimeMillis();
        for (int i = 0; i < iteration; ++i) {
            String randomString = RandomStringUtils.randomAlphabetic(random.nextInt(1, 10));
            underTest.getTopResult(randomString);
        }
        long end = System.currentTimeMillis();

        double iterationPerSec = iteration / ((end - start) / 1000.0);

        System.out.println("Iteration/sec = " + iterationPerSec);
    }

}
