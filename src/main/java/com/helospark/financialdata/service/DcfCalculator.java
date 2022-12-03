package com.helospark.financialdata.service;

public class DcfCalculator {

    public static double doDcfAnalysis(double eps, double pastGrowth) {
        double startGrowth = pastGrowth * 0.8;
        double endGrowth = pastGrowth * 0.4;
        return doDcfAnalysisWithGrowth(eps, startGrowth, endGrowth);
    }

    public static double doDcfAnalysisWithGrowth(double eps, double startGrowth, double endGrowth) {
        double dcf = 0.0;
        int years = 10;
        double discount = 0.15;
        double endMultiple = endGrowth;
        if (endMultiple > 18) {
            endMultiple = 18;
        }
        if (endMultiple < 8) {
            endMultiple = 8;
        }

        for (int i = 0; i < years; ++i) {
            double currentGrowth = startGrowth - ((startGrowth - endGrowth) * i) / (years - 1);

            eps *= 1.0 + (currentGrowth / 100.0);

            dcf += (eps / Math.pow(1.0 + discount, i + 1));
        }

        dcf += ((eps * endMultiple) / Math.pow(1.0 + discount, years));

        return dcf;
    }

}
