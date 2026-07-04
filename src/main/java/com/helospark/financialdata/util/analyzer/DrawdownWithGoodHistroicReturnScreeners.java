package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.Helpers;
import com.helospark.financialdata.service.RoicCalculator;

public class DrawdownWithGoodHistroicReturnScreeners {

    public static void main(String[] args) {
        Set<String> symbols = DataLoader.provideSymbolsFromNasdaqNyse();

        new DrawdownWithGoodHistroicReturnScreeners().analyze(symbols);
    }

    static class ResultSet {
        public List<Double> longTermReturns = new ArrayList<>();
        public List<Double> totalReturns = new ArrayList<>();

        public ResultSet(List<Double> longTermReturns2, List<Double> totalReturns2) {
            this.longTermReturns = longTermReturns2;
            this.totalReturns = totalReturns2;
        }
    }

    public void analyze(Set<String> symbols) {
        LocalDate now = LocalDate.now();

        int[][] types = new int[][] {
                new int[] { -30, -20, 15, 15 },
                new int[] { -20, -20, 15, 15 },
                new int[] { -30, -30, 15, 15 },
                new int[] { -20, -20, 20, 20 },
                new int[] { -50, -30, 15, 15 },
                new int[] { -40, -35, 0, 7 },
        };
        List<ResultSet> resultSet = new ArrayList<>();

        for (int[] arr : types) {
            List<Double> longTermReturns = new ArrayList<>();
            List<Double> totalReturns = new ArrayList<>();
            for (var symbol : symbols) {
                CompanyFinancials company = readFinancials(symbol);
                var financials = company.financials;

                if (financials.isEmpty()) {
                    continue;
                }

                int begin = financials.get(financials.size() - 1).date.getYear() + 5;
                int end = financials.get(0).getDate().getYear();
                for (int i = begin; i <= end; ++i) {
                    LocalDate localDateAt = LocalDate.of(i, 1, 1);
                    int index = Helpers.findIndexWithOrBeforeDate(financials, localDateAt);
                    if (index == -1) {
                        continue;
                    }
                    double oneYearReturn = calculateReturnMonthAgo(company, localDateAt, 1 * 12);
                    double twoYearReturn = calculateReturnMonthAgo(company, localDateAt, 2 * 12);
                    double fiveYearReturn = calculateReturnMonthAgo(company, localDateAt, 5 * 12);
                    double tenYearReturn = calculateReturnMonthAgo(company, localDateAt, 10 * 12);
                    double altman = AltmanZCalculator.calculateAltmanZScore(financials.get(index), financials.get(index).price);
                    double roic = RoicCalculator.calculateRoic(financials.get(index)) * 100.0;

                    boolean poorShortTermReturn = oneYearReturn < arr[0] && twoYearReturn < arr[1];
                    boolean goodLongTermReturn = fiveYearReturn > arr[2] || tenYearReturn > arr[3];
                    boolean highQuality = altman > 2 && roic > 15;

                    boolean large = financials.get(index).incomeStatementTtm.revenue > 100_000_000;

                    if (poorShortTermReturn && goodLongTermReturn && highQuality && large) {
                        int monthsBetween = (int) Math.abs(ChronoUnit.MONTHS.between(localDateAt, now));
                        double longTermReturnTillNow = calculateReturnMonthAgo(company, now, monthsBetween);

                        System.out.printf("%s: %s had poor short term return (%.2f%%, %.2f%%) with long term return (%.2f%%, %.2f%%) then delivered %.2f%% in %d years\n",
                                localDateAt.toString(),
                                symbol,
                                oneYearReturn, twoYearReturn,
                                fiveYearReturn, tenYearReturn,
                                longTermReturnTillNow,
                                monthsBetween / 12);

                        if (monthsBetween > 5 * 12) {
                            longTermReturns.add(longTermReturnTillNow);
                        }
                        totalReturns.add(Math.pow(1.0 + (longTermReturnTillNow / 100.0), monthsBetween / 12.0));
                    }

                }
            }
            resultSet.add(new ResultSet(longTermReturns, totalReturns));
        }

        for (int i = 0; i < types.length; ++i) {
            System.out.println(Arrays.toString(types[i]));

            System.out.println("CAGR");
            printStatistics(resultSet.get(i).longTermReturns, "%");

            System.out.println("total returns");
            printStatistics(resultSet.get(i).totalReturns, "x");

            System.out.println();
            System.out.println();
        }

    }

    private double calculateReturnMonthAgo(CompanyFinancials data, LocalDate now, int monthAgo) {
        return calculateReturnMonthAgo(data, now, monthAgo, true);
    }

    public double calculateReturnMonthAgo(CompanyFinancials data, LocalDate nowDate, int monthAgo, boolean annualized) {
        LocalDate oldDate = nowDate.minusMonths(monthAgo);

        return GrowthCalculator.getPriceGrowthWithReinvestedDividendsGrowth(data, ChronoUnit.MONTHS.between(oldDate, CommonConfig.NOW) / 12.0, ChronoUnit.MONTHS.between(nowDate, CommonConfig.NOW) / 12.0).orElse(Double.NaN);
    }

    public static void printStatistics(List<Double> longTermReturns, String format) {
        if (longTermReturns == null || longTermReturns.isEmpty()) {
            System.out.println("The list is empty or null.");
            return;
        }

        List<Double> sorted = new ArrayList<>(longTermReturns);
        Collections.sort(sorted);
        int size = sorted.size();

        double sum = 0;
        for (double val : sorted) {
            sum += val;
        }
        double avg = sum / size;

        double p25 = getPercentile(sorted, 25);
        double median = getPercentile(sorted, 50);
        double p75 = getPercentile(sorted, 75);
        double p90 = getPercentile(sorted, 90);
        double max = sorted.get(size - 1);

        System.out.println("--- Long-Term Return Statistics ---");
        System.out.printf("Average (Mean): %.2f%s%n", avg, format);
        System.out.printf("10th Percentile: %.2f%s%n", getPercentile(sorted, 10), format);
        System.out.printf("25th Percentile: %.2f%s%n", p25, format);
        System.out.printf("Median (p50):    %.2f%s%n", median, format);
        System.out.printf("75th Percentile: %.2f%s%n", p75, format);
        System.out.printf("90th Percentile: %.2f%s%n", p90, format);
        System.out.printf("95th Percentile: %.2f%s%n", getPercentile(sorted, 95), format);
        System.out.printf("98th Percentile: %.2f%s%n", getPercentile(sorted, 98), format);
        System.out.printf("Maximum value:   %.2f%s%n", max, format);
        //        System.out.println(sorted);
        //
        //        System.out.println("---------------------");
        //        System.out.println();
    }

    private static double getPercentile(List<Double> sortedList, double percentile) {
        int size = sortedList.size();
        if (size == 1)
            return sortedList.get(0);

        double index = (percentile / 100.0) * (size - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sortedList.get(lower);
        }

        double weight = index - lower;
        return sortedList.get(lower) * (1 - weight) + sortedList.get(upper) * weight;
    }
}
