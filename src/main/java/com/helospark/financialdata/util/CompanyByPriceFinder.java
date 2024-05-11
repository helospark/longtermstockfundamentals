package com.helospark.financialdata.util;

import static com.helospark.financialdata.service.DataLoader.readFinancials;

import java.time.LocalDate;
import java.util.Set;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.Helpers;

public class CompanyByPriceFinder {

    public static void main(String[] args) {
        Set<String> symbols = DataLoader.provideAllSymbols();

        symbols.parallelStream()
                .forEach(symbol -> {
                    CompanyFinancials company = readFinancials(symbol);

                    if (!company.financials.isEmpty()) {
                        int asdIndex = Helpers.findIndexWithOrBeforeDate(company.financials, LocalDate.of(2019, 6, 15));
                        int index = Helpers.findIndexWithOrBeforeDate(company.financials, LocalDate.of(2023, 9, 22));
                        int oldIndex = Helpers.findIndexWithOrBeforeDate(company.financials, LocalDate.of(2021, 1, 15));

                        if (index != -1 && oldIndex != -1 && asdIndex != -1 &&
                                company.financials.get(asdIndex).price < 0.6 && company.financials.get(asdIndex).price > 0.1 &&
                                company.financials.get(index).price < 0.2 && company.financials.get(index).price > 0.1 &&
                                company.financials.get(oldIndex).price < 2.0 && company.financials.get(oldIndex).price > 1.20 &&
                                (company.profile.currency == null || company.profile.currency.equals("CAD"))) {
                            System.out.println(symbol + " " + company.profile.currency);
                        }
                    }
                });
    }

}
