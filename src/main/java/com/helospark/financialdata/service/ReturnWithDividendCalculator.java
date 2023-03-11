package com.helospark.financialdata.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.util.Strings;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.SimpleDateDataElement;

public class ReturnWithDividendCalculator {
    private static final Cache<String, List<SimpleDateDataElement>> CACHE = Caffeine.newBuilder()
            .maximumSize(20)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    public static List<SimpleDateDataElement> getPriceWithDividendsReinvested(CompanyFinancials company) {
        if (Strings.isBlank(company.profile.symbol)) {
            return gerPriceGrowthInternal(company);
        } else {
            return CACHE.get(company.profile.symbol, asd -> gerPriceGrowthInternal(company));
        }
    }

    public static List<SimpleDateDataElement> gerPriceGrowthInternal(CompanyFinancials company) {
        double shareCount = 1.0;

        List<SimpleDateDataElement> result = new ArrayList<>();
        for (int i = company.financials.size() - 1; i >= 0; --i) {
            FinancialsTtm financialsTtm = company.financials.get(i);
            double dividendPaid = (double) -financialsTtm.cashFlow.dividendsPaid / financialsTtm.incomeStatement.weightedAverageShsOut;
            double priceThen = financialsTtm.price;
            shareCount += ((shareCount * dividendPaid) / priceThen);

            double value = priceThen * shareCount;
            result.add(new SimpleDateDataElement(financialsTtm.getDate(), value));
        }
        if (company.financials.size() > 0 && company.latestPriceDate.compareTo(company.financials.get(0).getDate()) > 0) {
            double value = company.latestPrice * shareCount;
            result.add(new SimpleDateDataElement(company.latestPriceDate, value));
        }
        Collections.reverse(result);
        return result;
    }

}
