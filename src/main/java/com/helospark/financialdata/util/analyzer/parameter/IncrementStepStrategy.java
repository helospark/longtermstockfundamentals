package com.helospark.financialdata.util.analyzer.parameter;

public class IncrementStepStrategy implements StepStrategy {
    double min;
    double max;
    double step;

    public IncrementStepStrategy(double min, double max, double step) {
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public StepStrategyResponse step(double previous) {
        double number = previous + this.step;
        boolean ended;
        if (number > this.max) {
            number = this.min;
            ended = true;
        } else {
            ended = false;
        }
        return new StepStrategyResponse(ended, number);
    }

    @Override
    public StepStrategy copy() {
        return new IncrementStepStrategy(min, max, step);
    }

}
