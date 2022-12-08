package com.helospark.financialdata.util.analyzer.parameter;

public interface StepStrategy {

    StepStrategyResponse step(double previous);

    StepStrategy copy();

}
