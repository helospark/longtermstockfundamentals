package com.helospark.financialdata.util;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helospark.financialdata.domain.IncomeStatement;
import com.helospark.financialdata.service.DataLoader;

public class MacroTrendStockDataDownloader {
    static Map<String, FieldMapping> incomeStatementMapping = new HashMap<>();

    static {
        incomeStatementMapping.put("Revenue", new FieldMapping("revenue"));
        incomeStatementMapping.put("Cost Of Goods Sold", new FieldMapping("costOfRevenue"));
        incomeStatementMapping.put("Gross Profit", new FieldMapping("grossProfit"));
        incomeStatementMapping.put("Research And Development Expenses", new FieldMapping("researchAndDevelopmentExpenses"));
        incomeStatementMapping.put("SG&A Expenses", new FieldMapping("sellingGeneralAndAdministrativeExpenses"));
        incomeStatementMapping.put("Operating Expenses", new FieldMapping("operatingExpenses"));
        incomeStatementMapping.put("Other Operating Income Or Expenses", new FieldMapping("otherExpenses"));
        incomeStatementMapping.put("Pre-Tax Income", new FieldMapping("incomeBeforeTax"));
        incomeStatementMapping.put("Income Taxes", new FieldMapping("incomeTaxExpense"));
        incomeStatementMapping.put("Net Income", new FieldMapping("netIncome"));
        incomeStatementMapping.put("EBITDA", new FieldMapping("ebitda"));
        incomeStatementMapping.put("Basic Shares Outstanding", new FieldMapping("weightedAverageShsOut"));
        incomeStatementMapping.put("Shares Outstanding", new FieldMapping("weightedAverageShsOutDil"));
        incomeStatementMapping.put("Basic EPS", new FieldMapping("eps", false));
        incomeStatementMapping.put("EPS - Earnings Per Share", new FieldMapping("epsdiluted", false));
        incomeStatementMapping.put("Operating Income", new FieldMapping("operatingIncome"));
    }

    public static void main(String[] args) throws Exception {
        String symbol = "GOOGL";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> entity = restTemplate.getForEntity("https://www.macrotrends.net/stocks/charts/" + symbol + "/asd/income-statement?freq=Q", String.class);

        String newUri = Arrays.stream(entity.getBody().split("\n"))
                .filter(a -> a.contains("link rel=\"canonical\""))
                .findFirst()
                .map(a -> a.replaceAll(".*?https://", "").replaceAll("income-statement.*", ""))
                .get();
        newUri = "https://" + newUri;

        String fileContent = restTemplate.getForObject(newUri + "income-statement?freq=Q", String.class);

        //        FileInputStream fis = new FileInputStream(new File("/tmp/out.html"));

        //        String fileContent = new String(fis.readAllBytes());

        String[] lines = fileContent.split("\n");
        long multiplier = 1_000_000;
        String currency = "USD";

        String jsonLine = "";
        for (var line : lines) {
            if (line.contains("originalData =")) {
                jsonLine = line.replace("var originalData =", "").replace(";", "");
                break;
            }
        }
        ObjectMapper om = new ObjectMapper();
        List<Map<String, String>> dataDeserialized = om.readValue(jsonLine, new TypeReference<List<Map<String, String>>>() {
        });

        IncomeStatement incomeStatement = readIncomeStatement("2024-03-31", dataDeserialized, multiplier);
        IncomeStatement realIncomeStatement = DataLoader.readFinancials("GOOGL").financials.get(0).incomeStatement;

        for (var field : IncomeStatement.class.getFields()) {
            Object val1 = field.get(incomeStatement);
            Object val2 = field.get(realIncomeStatement);
            System.out.println(field.getName() + "\n" + val1 + "\n" + val2 + "\n\n");
        }

    }

    private static IncomeStatement readIncomeStatement(String dateKey, List<Map<String, String>> dataDeserialized, long multiplier) throws Exception {
        var incomeStatement = new IncomeStatement();
        incomeStatement.date = LocalDate.parse(dateKey);
        incomeStatement.reportedCurrency = "USD";

        for (var element : dataDeserialized) {
            String fieldName = element.get("field_name");
            String data = element.get(dateKey);
            updateData(incomeStatement, fieldName, data, dateKey, multiplier);
        }

        return incomeStatement;
    }

    private static void updateData(IncomeStatement incomeStatement, String fieldName, String data, String dateKey, long multiplier) throws Exception {

        for (var entry : incomeStatementMapping.entrySet()) {
            if (fieldName.contains(entry.getKey())) {
                FieldMapping setterFieldName = entry.getValue();
                Field field = incomeStatement.getClass().getField(setterFieldName.field);
                field.setAccessible(true);
                var type = field.getType();

                long multiplierToUse = setterFieldName.useMultiplier ? multiplier : 1;

                double dataDouble;
                if (!data.isEmpty()) {
                    dataDouble = Double.parseDouble(data);
                } else {
                    dataDouble = 0.0;
                }
                if (type.equals(long.class)) {
                    field.set(incomeStatement, (long) (dataDouble * multiplierToUse));
                }
                if (type.equals(double.class)) {
                    field.set(incomeStatement, dataDouble * multiplierToUse);
                }
            }
        }
    }

    static class FieldMapping {
        String field;
        boolean useMultiplier = true;

        public FieldMapping(String field) {
            this.field = field;
        }

        public FieldMapping(String field, boolean useMultiplier) {
            this.field = field;
            this.useMultiplier = useMultiplier;
        }

    }

    static class MacroTrendData {
        String field_name;
    }
}
