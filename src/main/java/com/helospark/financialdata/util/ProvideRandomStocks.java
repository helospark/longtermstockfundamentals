package com.helospark.financialdata.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class ProvideRandomStocks {

    public static void main(String[] args) {
        List<String> symbols = new ArrayList<>(DataLoader.provideSp500Symbols());

        Collections.shuffle(symbols);

        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();
        LinkedHashMap<String, AtGlanceData> data = symbolAtGlanceProvider.getSymbolCompanyNameCache();

        for (int i = 0; i < 100; ++i) {
            AtGlanceData asd = data.get(symbols.get(i));
            if (asd != null) {
                System.out.println("http://localhost:8080/stock/" + symbols.get(i) + "\t\t\t" + asd.investmentScore);
            }
        }
    }

}
