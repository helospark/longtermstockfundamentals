package com.helospark.financialdata.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.helospark.financialdata.domain.SearchElement;
import com.helospark.financialdata.util.StockDataDownloader;

@Component
public class SymbolIndexProvider {
    List<String> symbols = new ArrayList<>();
    List<String> companyNames = new ArrayList<>();
    List<String> upperCompanyNames = new ArrayList<>();

    int longestCompanyName;
    int longestSymbol;

    public SymbolIndexProvider() {
        try (var file = new FileInputStream(new File(StockDataDownloader.SYMBOL_CACHE_FILE))) {
            for (var line : new String(file.readAllBytes()).split("\n")) {
                String[] elements = line.split(";");
                String symbol = elements[0].toUpperCase();

                if (symbol.length() > longestSymbol) {
                    longestSymbol = symbol.length();
                }

                symbols.add(symbol);
                if (elements.length > 1) {
                    companyNames.add(elements[1]);
                    upperCompanyNames.add(elements[1].toUpperCase());

                    if (elements[1].length() > longestCompanyName) {
                        longestCompanyName = elements[1].length();
                    }
                } else {
                    companyNames.add("");
                    upperCompanyNames.add("");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<SearchElement> getTopResult(String parts) {
        if (parts.length() > longestCompanyName || parts.isBlank()) {
            return List.of();
        }
        List<SearchElement> result = new ArrayList<>();
        String partsUppercase = parts.toUpperCase();

        List<SearchElement> bestSymbols = new ArrayList<>();
        if (parts.length() <= longestSymbol) {
            for (int i = 0; i < symbols.size(); ++i) {
                var element = symbols.get(i);
                if (element.equals(partsUppercase)) {
                    bestSymbols.add(0, new SearchElement(element, companyNames.get(i)));
                } else if (element.startsWith(partsUppercase)) {
                    bestSymbols.add(new SearchElement(element, companyNames.get(i)));
                }

            }

            for (int i = 0; i < 3 && i < bestSymbols.size(); ++i) {
                result.add(bestSymbols.get(i));
            }
        }
        if (parts.length() > 1) {
            for (int i = 0; i < companyNames.size(); ++i) {
                SearchElement asd = new SearchElement(symbols.get(i), companyNames.get(i));
                if (upperCompanyNames.get(i).contains(partsUppercase) && !result.contains(asd)) {
                    result.add(asd);
                }
                if (result.size() > 6) {
                    break;
                }
            }
        }
        return result;
    }

}
