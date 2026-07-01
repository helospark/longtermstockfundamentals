package com.helospark.financialdata;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.helospark.financialdata.util.spconstituents.Sp500ConstituentsProvider;

public class SP500ConstituentsProviderTest {

    @Test
    public void test() {
        Sp500ConstituentsProvider asd = new Sp500ConstituentsProvider();

        List<String> result = asd.getConstituentsForDate(LocalDate.of(2010, 1, 1));
        //        asd.exportOptimizedConstituentsZip(CommonConfig.BASE_FOLDER + "/info/sp500_historical_constituents_small.csv.zip");

        Assertions.assertTrue(result.get(0).equals("A"));
    }

}
