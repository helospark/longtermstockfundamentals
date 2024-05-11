package com.helospark.financialdata.util.spconstituents;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.BalanceSheet;
import com.helospark.financialdata.domain.CashFlow;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.IncomeStatement;
import com.helospark.financialdata.domain.Profile;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.InvestmentScoreCalculator;
import com.helospark.financialdata.service.RatioCalculator;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

@Component
public class Sp500MetricCalculator {
    @Autowired
    private Sp500ConstituentsProvider constituentsProvider;
    @Autowired
    private SymbolAtGlanceProvider symbolIndexProvider;

    public GeneralCompanyMetrics calculateMetrics() {
        List<Sp500WeightedConstituent> constituents = constituentsProvider.getConstituents();

        Map<String, CompanyFinancials> localCache = new HashMap<>();
        for (var cons : constituents) {
            Optional<AtGlanceData> atGlanceOpt = symbolIndexProvider.getAtGlanceData(cons.symbol);

            if (atGlanceOpt.isEmpty()) {
                System.out.println(cons.symbol + " is not present");
                continue;
            }

            CompanyFinancials financials = DataLoader.readFinancials(cons.symbol);

            localCache.put(cons.symbol, financials);
        }

        List<FinancialsTtm> financials = new ArrayList<>();
        for (int i = 0; i < 6 * 4; ++i) {
            financials.add(convertToTtm(constituents, localCache, i));
        }

        FinancialsTtm latestData = financials.get(0);
        CompanyFinancials company = new CompanyFinancials(latestData.price, latestData.price, latestData.price, latestData.date, financials, new Profile(), 0);

        GeneralCompanyMetrics result = new GeneralCompanyMetrics();

        result.roic = RoicCalculator.calculateRoic(latestData) * 100.0;
        result.roe = RoicCalculator.calculateROE(latestData) * 100.0;
        result.fcfRoic = RoicCalculator.calculateFcfRoic(latestData) * 100.0;
        result.altman = AltmanZCalculator.calculateAltmanZScore(latestData, latestData.price);
        result.grossMargin = RatioCalculator.calculateGrossProfitMargin(latestData) * 100.0;
        result.opMargin = RatioCalculator.calculateOperatingMargin(latestData) * 100.0;
        result.netMargin = RatioCalculator.calculateNetMargin(latestData) * 100.0;
        result.d2e = RatioCalculator.calculateDebtToEquityRatio(latestData);
        result.pe = RatioCalculator.calculatePriceToEarningsRatio(latestData);
        result.pfcf = latestData.price / (latestData.cashFlowTtm.freeCashFlow / latestData.incomeStatementTtm.weightedAverageShsOut);
        result.price = latestData.price;
        result.eps = latestData.incomeStatementTtm.eps;
        result.revGrowth = GrowthCalculator.getRevenueGrowthInInterval(financials, 5, 0.0).orElse(0.0);
        result.epsGrowth = GrowthCalculator.getEpsGrowthInInterval(financials, 5, 0.0).orElse(0.0);
        result.shareCountGrowth = GrowthCalculator.getShareCountGrowthInInterval(financials, 5, 0.0).orElse(0.0);
        result.investScore = InvestmentScoreCalculator.calculate(company, 0).orElse(0.0);

        result.oneYrGrowth = GrowthCalculator.getPriceGrowthInInterval(financials, 1, 0).get();
        result.twoYrGrowth = GrowthCalculator.getPriceGrowthInInterval(financials, 2, 0).get();
        result.threeYrGrowth = GrowthCalculator.getPriceGrowthInInterval(financials, 3, 0).get();
        result.fiveYrGrowth = GrowthCalculator.getPriceGrowthInInterval(financials, 5, 0).get();

        return result;
    }

    public FinancialsTtm convertToTtm(List<Sp500WeightedConstituent> constituents, Map<String, CompanyFinancials> localCache, int index) {
        Map<String, Double> mergedIncomeStatementData = new HashMap<>();
        Map<String, Double> mergedBalanceSheetData = new HashMap<>();
        Map<String, Double> mergedCashflowStatementData = new HashMap<>();

        double price = 0.0;
        double weights = 0.0;
        LocalDate date = null;

        for (var cons : constituents) {
            CompanyFinancials financials = localCache.get(cons.symbol);

            if (financials == null || financials.financials.size() <= index) {
                System.out.println(cons.symbol + " has empty financials");
                continue;
            }
            FinancialsTtm currentElement = financials.financials.get(index);
            date = currentElement.date;

            IncomeStatement incomeStatement = currentElement.incomeStatementTtm;
            BalanceSheet balanceSheet = currentElement.balanceSheet;
            CashFlow cashflowStatement = currentElement.cashFlowTtm;

            calculateFor(mergedIncomeStatementData, cons, incomeStatement);
            calculateFor(mergedBalanceSheetData, cons, balanceSheet);
            calculateFor(mergedCashflowStatementData, cons, cashflowStatement);

            price += currentElement.priceUsd * cons.weight;
            weights += cons.weight;

        }

        IncomeStatement incomeStatement = convertTo(mergedIncomeStatementData, IncomeStatement.class);
        BalanceSheet balanceSheet = convertTo(mergedBalanceSheetData, BalanceSheet.class);
        CashFlow cashflowStatement = convertTo(mergedCashflowStatementData, CashFlow.class);

        incomeStatement.weightedAverageShsOut *= 0.5844; // divisor adjustment
        incomeStatement.weightedAverageShsOutDil *= 0.5844;
        incomeStatement.eps = incomeStatement.netIncome / incomeStatement.weightedAverageShsOut;

        FinancialsTtm ttm = new FinancialsTtm();
        ttm.balanceSheet = balanceSheet;
        ttm.incomeStatement = incomeStatement;
        ttm.incomeStatementTtm = incomeStatement;
        ttm.cashFlowTtm = cashflowStatement;
        ttm.cashFlow = cashflowStatement;
        ttm.price = price;
        ttm.priceTradingCurrency = price;
        ttm.priceUsd = price;
        ttm.date = date;
        return ttm;
    }

    private <T> T convertTo(Map<String, Double> mergedIncomeStatementData, Class<T> class1) {
        try {
            T newInstance = class1.newInstance();

            for (var entry : mergedIncomeStatementData.entrySet()) {
                Field field = class1.getField(entry.getKey());
                Class<?> type = field.getType();
                double value = entry.getValue();

                if (long.class.equals(type)) {
                    field.set(newInstance, (long) value);
                } else if (double.class.equals(type)) {
                    field.set(newInstance, value);
                }

            }

            return newInstance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void calculateFor(Map<String, Double> mergedData, Sp500WeightedConstituent cons, Object incomeStatement) {
        try {
            for (var field : incomeStatement.getClass().getDeclaredFields()) {
                String name = field.getName();
                double value = 0.0;
                boolean found = false;
                if (field.getType().equals(long.class)) {
                    value = (long) field.get(incomeStatement);
                    found = true;
                } else if (field.getType().equals(double.class)) {
                    value = (double) field.get(incomeStatement);
                    found = true;
                }

                if (found && Double.isFinite(value)) {
                    double prevValue = mergedData.getOrDefault(name, 0.0);
                    prevValue += value * cons.weight;
                    mergedData.put(name, prevValue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
