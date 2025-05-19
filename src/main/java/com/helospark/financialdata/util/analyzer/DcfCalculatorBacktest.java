package com.helospark.financialdata.util.analyzer;

import java.util.Optional;

import com.helospark.financialdata.management.watchlist.domain.CalculatorParameters;
import com.helospark.financialdata.service.DcfCalculator;

public class DcfCalculatorBacktest {

    public static void main(String[] args) {
        DcfCalculator dcf = new DcfCalculator();

        CalculatorParameters param = new CalculatorParameters();
        param.discount = 8.0;
        param.endGrowth = 5.0;
        param.startGrowth = 5.0;
        param.endMargin = 100.0;
        param.startMargin = 100.0;
        param.startPayout = 100.0;
        param.endPayout = 100.0;
        param.endShChange = 0.0;
        param.startShChange = 0.0;
        param.endMultiple = 35.0;

        Optional<Double> result = dcf.doDcf(100, 1, param);

        System.out.println(result);
    }

}
