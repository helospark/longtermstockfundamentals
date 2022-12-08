package com.helospark.financialdata.util.analyzer.parameter;

import java.util.List;

public class TestParameterResult {
    public double result;
    public double benchmark;

    public double value;
    public double benchmarkValue;

    double beatPercent;

    public List<Parameter> parameters;

    public TestParameterResult(double result, double value, double benchmark, double bmValue, double beatPercent, List<Parameter> parameters) {
        this.result = result;
        this.benchmark = benchmark;
        this.parameters = parameters;
        this.value = value;
        this.benchmarkValue = bmValue;
        this.beatPercent = beatPercent;
    }

    @Override
    public String toString() {
        return "result=" + result + ", benchmark=" + benchmark + ", v=$" + (long) value + ", b=$" + (long) benchmarkValue + ", beatPerc=" + beatPercent + ", parameters=" + parameters;
    }

    public TestParameterResult copy() {
        var paramsCopy = parameters.stream().map(a -> a.copy()).toList();
        return new TestParameterResult(result, value, benchmark, benchmarkValue, beatPercent, paramsCopy);
    }

}
