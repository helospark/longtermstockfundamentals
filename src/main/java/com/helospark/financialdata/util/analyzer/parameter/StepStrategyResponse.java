package com.helospark.financialdata.util.analyzer.parameter;

public class StepStrategyResponse {
    public boolean finished;
    public double updatedNumber;

    public StepStrategyResponse(boolean finished, double updatedNumber) {
        this.finished = finished;
        this.updatedNumber = updatedNumber;
    }

}
