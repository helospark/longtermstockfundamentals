package com.helospark.financialdata.util.analyzer.parameter;

public class Parameter {
    public String name;

    public Double number;

    public StepStrategy stepStrategy;

    public Parameter(String name, Double number, StepStrategy stepStrategy) {
        this.name = name;
        this.number = number;
        this.stepStrategy = stepStrategy;
    }

    public Parameter copy() {
        return new Parameter(name, number, stepStrategy.copy());
    }

    public boolean step() {
        var result = stepStrategy.step(number);
        this.number = result.updatedNumber;
        return result.finished;
    }

    @Override
    public String toString() {
        return name + "=" + number + "";
    }

}
