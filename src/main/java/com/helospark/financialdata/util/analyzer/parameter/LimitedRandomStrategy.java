package com.helospark.financialdata.util.analyzer.parameter;

import java.util.Random;

public class LimitedRandomStrategy implements StepStrategy {
    private Random random = new Random();
    public int limit;
    public int count;
    public double min;
    public double max;

    public LimitedRandomStrategy(int limit, double min, double max) {
        this.count = 0;
        this.limit = limit;
        this.min = min;
        this.max = max;
    }

    @Override
    public StepStrategyResponse step(double previous) {
        double number = random.nextDouble(min, max);
        boolean ended;
        if (++count >= limit) {
            this.count = 0;
            ended = true;
        } else {
            ended = false;
        }
        return new StepStrategyResponse(ended, number);
    }

    @Override
    public StepStrategy copy() {
        return new LimitedRandomStrategy(limit, min, max);
    }

}
