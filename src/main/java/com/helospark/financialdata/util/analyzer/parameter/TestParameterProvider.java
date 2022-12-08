package com.helospark.financialdata.util.analyzer.parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestParameterProvider {
    private List<Parameter> parameters = new ArrayList<>();
    public List<TestParameterResult> results = new ArrayList<>();

    public void registerParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    public Double getValue(String name) {
        for (var parameter : parameters) {
            if (parameter.name.equals(name)) {
                return parameter.number;
            }
        }
        return null;
    }

    public boolean step() {
        if (parameters.size() == 0) {
            return true;
        }

        boolean maxed;
        int index = parameters.size() - 1;
        do {
            maxed = parameters.get(index).step();
            if (maxed) {
                --index;
                if (index < 0) {
                    return true;
                }
            } else {
                return false;
            }
        } while (maxed);
        return true;
    }

    public void addResult(double resultPercent, double value, double benchmarkResultPercent, double benchmarkValue, double d) {
        List<Parameter> paramCopy = parameters.stream()
                .map(a -> a.copy())
                .collect(Collectors.toList());

        results.add(new TestParameterResult(resultPercent, value, benchmarkResultPercent, benchmarkValue, d, paramCopy));
    }

    public void printResult() {
        if (results.size() == 0) {
            return;
        }
        Collections.sort(results, (a, b) -> Double.compare(a.result, b.result));

        for (var result : results) {
            System.out.println(result);
        }

        double meanResult = results.get(results.size() / 2).result;
        double avgResult = results.stream().mapToDouble(a -> a.result).average().getAsDouble();

        Collections.sort(results, (a, b) -> Double.compare(a.benchmark, b.benchmark));
        double meanBenchmark = results.get(results.size() / 2).benchmark;
        double avgBenchmark = results.stream().mapToDouble(a -> a.benchmark).average().getAsDouble();

        double sum = results.stream().mapToDouble(a -> a.value).sum();
        double benchmarkSum = results.stream().mapToDouble(a -> a.benchmarkValue).sum();

        double max = results.stream().mapToDouble(a -> a.result).max().getAsDouble();
        double min = results.stream().mapToDouble(a -> a.result).min().getAsDouble();

        double bmMax = results.stream().mapToDouble(a -> a.benchmark).max().getAsDouble();
        double bmMin = results.stream().mapToDouble(a -> a.benchmark).min().getAsDouble();

        System.out.println();
        System.out.println("mean = " + meanResult + "\tbenchmark=" + meanBenchmark);
        System.out.println("avg  = " + avgResult + "\tbenchmark=" + avgBenchmark);
        System.out.println("max  = " + max + "\tbenchmark=" + bmMax);
        System.out.println("min  = " + min + "\tbenchmark=" + bmMin);
        System.out.println("sum  = " + (int) sum + "\tbenchmark=" + (int) benchmarkSum);
    }

    public static TestParameterProvider createFromList(List<TestParameterProvider> list) {
        var result = new TestParameterProvider();
        result.results = list.stream().flatMap(a -> a.results.stream()).collect(Collectors.toList());
        return result;
    }

    public TestParameterProvider copy() {
        TestParameterProvider result = new TestParameterProvider();
        result.parameters = parameters.stream().map(a -> a.copy()).toList();
        result.results = new ArrayList<>(results.stream().map(a -> a.copy()).toList());
        return result;
    }
}
