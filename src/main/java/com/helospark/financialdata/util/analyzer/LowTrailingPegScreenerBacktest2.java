package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.AltmanZCalculator.calculateAltmanZScore;
import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;
import static com.helospark.financialdata.service.GrowthAnalyzer.isStableGrowth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.StandardAndPoorPerformanceProvider;
import com.helospark.financialdata.service.TrailingPegCalculator;
import com.helospark.financialdata.util.analyzer.parameter.IncrementStepStrategy;
import com.helospark.financialdata.util.analyzer.parameter.Parameter;
import com.helospark.financialdata.util.analyzer.parameter.TestParameterProvider;

public class LowTrailingPegScreenerBacktest2 implements StockScreeners {

    private static final double INVEST_PER_STOCK = 1000.0;

    @Override
    public void analyze(Set<String> symbols) {
        TestParameterProvider param = new TestParameterProvider();
        param.registerParameter(new Parameter("pe", 20.0, new IncrementStepStrategy(20.0, 30.0, 1.0)));
        param.registerParameter(new Parameter("peg", 0.4, new IncrementStepStrategy(0.4, 1.2, 0.1)));
        param.registerParameter(new Parameter("py", 3.0, new IncrementStepStrategy(3.0, 6.0, 1.0)));
        param.registerParameter(new Parameter("alt", 2.0, new IncrementStepStrategy(2.0, 5.0, 0.2)));
        boolean finished = false;

        List<TestParameterProvider> providerList = new ArrayList<>();
        Queue<TestParameterProvider> elements = new ConcurrentLinkedQueue<>();
        do {
            providerList.add(param.copy());
            finished = param.step();
        } while (!finished);
        elements.addAll(providerList);
        System.out.println("steps: " + providerList.size());

        ExecutorService tpe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
            futures.add(CompletableFuture.runAsync(() -> process(symbols, elements), tpe));
        }
        futures.stream().map(a -> a.join()).collect(Collectors.toList());

        TestParameterProvider resultProvider = TestParameterProvider.createFromList(providerList);

        resultProvider.printResult();

        tpe.shutdownNow();
    }

    private void process(Set<String> symbols, Queue<TestParameterProvider> providerQueue) {
        while (true) {
            TestParameterProvider param = providerQueue.poll();
            if (param == null) {
                return;
            }
            double growthSum = 0.0;
            double benchmarkSum = 0.0;
            int count = 0;
            for (int yearsAgo = 5; yearsAgo <= 28; ++yearsAgo) {
                //                System.out.println("symbol\t(Growth1, Growth2, Growth3)\t\tDCF\tPE\tUpside%\tfcfUpside%");
                for (var symbol : symbols) {
                    CompanyFinancials company = readFinancials(symbol);
                    var financials = company.financials;
                    //                    System.out.println(symbol);
                    if (financials.isEmpty()) {
                        continue;
                    }
                    //                    if (symbol.equals("CMP")) {
                    //                        break;
                    //                    }

                    int latestElement = yearsAgo * 4;

                    if (financials.size() > latestElement + 1) {

                        Optional<Double> trailingPeg = TrailingPegCalculator.calculateTrailingPeg(company, yearsAgo * 4);
                        Optional<Double> trailingPeg2 = TrailingPegCalculator.calculateTrailingPeg(company, yearsAgo * 4 + 1);
                        Optional<Double> trailingPeg3 = TrailingPegCalculator.calculateTrailingPeg(company, yearsAgo * 4 + 2);

                        double latestPriceThen = financials.get(latestElement).price;

                        int profitableYear = (int) Math.round(param.getValue("py"));
                        boolean continouslyProfitable = isProfitableEveryYearSince(financials, profitableYear + yearsAgo, yearsAgo);
                        boolean stableGrowth = isStableGrowth(financials, profitableYear + yearsAgo, yearsAgo);
                        double altmanZ = financials.size() > latestElement ? calculateAltmanZScore(financials.get(latestElement), latestPriceThen) : 0.0;
                        //                    System.out.println(latestPriceThen + " " + continouslyProfitable + " " + stableGrowth + " " + altmanZ);

                        if (trailingPeg.isPresent() && trailingPeg2.isPresent() && trailingPeg3.isPresent() &&
                                stableGrowth &&
                                continouslyProfitable &&
                                altmanZ > param.getValue("alt") &&
                                financials.get(latestElement).incomeStatementTtm.eps > 0.0) {

                            double currentPe = latestPriceThen / financials.get(latestElement).incomeStatementTtm.eps;

                            double pegCutoff = param.getValue("peg");
                            double peCutoff = param.getValue("pe");
                            if (trailingPeg.get() < pegCutoff && trailingPeg2.get() < pegCutoff && trailingPeg3.get() < pegCutoff && currentPe > peCutoff) {

                                int i = -1;
                                /*
                                for (i = latestElement - 1; i >= 0; --i) {
                                if (TrailingPegCalculator.calculateTrailingPeg(company, i).orElse(-1.0) > 2.0) {
                                System.out.print(" b(" + company.financials.get(i).price + ") ");
                                // break;
                                }
                                System.out.print(TrailingPegCalculator.calculateTrailingPeg(company, i).orElse(-1.0) + " ");
                                }
                                System.out.println();*/

                                double sellPrice = i > -1 ? company.financials.get(i).price : company.latestPrice;
                                double growthRatio = sellPrice / latestPriceThen;
                                growthSum += (growthRatio * INVEST_PER_STOCK);
                                benchmarkSum += INVEST_PER_STOCK
                                        * (StandardAndPoorPerformanceProvider.getLatestPrice() / StandardAndPoorPerformanceProvider.getPriceAt(financials.get(latestElement).getDate()));
                                ++count;

                                //                                System.out.println(symbol + " " + (growthSum));

                                //                            double growthTillSell = (growthRatio - 1.0) * 100.0;

                                //                                                            System.out.printf("%s\t(%.1f, %.1f, %.1f, %.1f)\t%.1f%% (%.1f -> %.1f)\t\t%s | %s\n", symbol,
                                //                                                                    trailingPeg.get(), trailingPeg2.get(), trailingPeg3.get(), currentPe,
                                //                                                                    growthTillSell, latestPriceThen, sellPrice, company.profile.companyName, company.profile.industry);
                            }
                        }
                    }
                }
                //                double benchmark = StandardAndPoorPerformanceProvider.getGrowth(yearsAgo);
                //            System.out.println("Have " + (growthSum) + " from " + invested + " (" + (increase * 100.0) + "%, " + (annual * 100.0) + "%) invested sp500=" + benchmark + "\t"
                //                    + LocalDate.now().minusYears(yearsAgo).getYear());
                //            System.out.println();
            }
            int invested = (int) (count * INVEST_PER_STOCK);
            double increase = (growthSum / invested - 1.0);
            double annual = Math.pow(growthSum / invested, (1.0 / 20.0)) - 1.0;
            if (Double.isFinite(annual)) {
                double benchmarkResultSumPerc = (Math.pow((benchmarkSum / invested), 1.0 / 20.0) - 1.0) * 100.0;
                param.addResult(annual * 100.0, growthSum, benchmarkResultSumPerc, benchmarkSum, 0);
            }
        }
    }

}
