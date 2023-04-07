package com.helospark.financialdata.util.analyzer.parameter;

import java.util.Random;

public class RandomStrategy implements StepStrategy {
    private Random random = new Random();
    public double min;
    public double max;

    public RandomStrategy(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public StepStrategyResponse step(double previous) {
        double number = random.nextDouble(min, max);
        return new StepStrategyResponse(false, number);
    }

    @Override
    public StepStrategy copy() {
        return new RandomStrategy(min, max);
    }

}
