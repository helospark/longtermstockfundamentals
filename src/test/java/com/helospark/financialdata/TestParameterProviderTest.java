package com.helospark.financialdata;

import org.junit.jupiter.api.Test;

import com.helospark.financialdata.util.analyzer.parameter.IncrementStepStrategy;
import com.helospark.financialdata.util.analyzer.parameter.Parameter;
import com.helospark.financialdata.util.analyzer.parameter.TestParameterProvider;
import com.helospark.financialdata.util.analyzer.parameter.TimedRandomStrategy;

public class TestParameterProviderTest {

    @Test
    public void test() {
        TestParameterProvider provider = new TestParameterProvider();
        provider.registerParameter(new Parameter("a", 1.0, new IncrementStepStrategy(1.0, 2.1, 1.0)));
        provider.registerParameter(new Parameter("b", 1.0, new IncrementStepStrategy(1.0, 2.1, 1.0)));
        provider.registerParameter(new Parameter("c", 1.0, new IncrementStepStrategy(1.0, 2.1, 1.0)));
        provider.registerParameter(new Parameter("d", 1.0, new IncrementStepStrategy(1.0, 2.1, 1.0)));

        boolean finished = false;

        do {
            System.out.println("a=" + provider.getValue("a") + " b=" + provider.getValue("b") + " c=" + provider.getValue("c") + " d=" + provider.getValue("d"));
            finished = provider.step();
        } while (!finished);
    }

    @Test
    public void testRandom() {
        TestParameterProvider provider = new TestParameterProvider();
        provider.registerParameter(new Parameter("a", 1.0, new TimedRandomStrategy(3, 1, 100)));
        provider.registerParameter(new Parameter("b", 1.0, new TimedRandomStrategy(1, 111, 222)));

        boolean finished = false;

        do {
            System.out.println("a=" + provider.getValue("a") + " b=" + provider.getValue("b"));
            finished = provider.step();
            noExceptionSleep();
        } while (!finished);
    }

    private void noExceptionSleep() {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {

        }
    }
}
