package com.helospark.financialdata.util.analyzer.parameter;

import java.util.Random;

public class TimedRandomStrategy implements StepStrategy {
    private Random random = new Random();
    public long endTime;
    public int time;
    public double min;
    public double max;

    public TimedRandomStrategy(int timeInSeconds, double min, double max) {
        this.endTime = System.currentTimeMillis() + timeInSeconds * 1000L;
        this.min = min;
        this.max = max;
    }

    @Override
    public StepStrategyResponse step(double previous) {
        double number = random.nextDouble(min, max);
        boolean ended;
        if (System.currentTimeMillis() > endTime) {
            this.endTime = System.currentTimeMillis() + time;
            ended = true;
        } else {
            ended = false;
        }
        return new StepStrategyResponse(ended, number);
    }

    @Override
    public StepStrategy copy() {
        return new TimedRandomStrategy(time, min, max);
    }

}
