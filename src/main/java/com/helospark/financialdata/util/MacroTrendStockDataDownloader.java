package com.helospark.financialdata.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helospark.financialdata.domain.BalanceSheet;
import com.helospark.financialdata.domain.CashFlow;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.IncomeStatement;
import com.helospark.financialdata.service.DataLoader;

public class MacroTrendStockDataDownloader {
    static Map<String, FieldMapping> incomeStatementMapping = new HashMap<>();
    static Map<String, FieldMapping> balanceSheetMapping = new HashMap<>();
    static Map<String, FieldMapping> cashFlowStatementMapping = new HashMap<>();
    static RestTemplate restTemplate = new RestTemplate();

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

        balanceSheetMapping.put("Receivables", new FieldMapping("netReceivables"));
        balanceSheetMapping.put("Inventory", new FieldMapping("inventory"));
        //        balanceSheetMapping.put("Pre-Paid Expenses", new FieldMapping("TODO"));
        balanceSheetMapping.put("Other Current Assets", new FieldMapping("otherCurrentAssets"));
        balanceSheetMapping.put("Total Current Assets", new FieldMapping("totalCurrentAssets"));
        balanceSheetMapping.put("Property, Plant, And Equipment", new FieldMapping("propertyPlantEquipmentNet"));
        balanceSheetMapping.put("Long-Term Investments", new FieldMapping("longTermInvestments"));
        balanceSheetMapping.put("Goodwill And Intangible Assets", new FieldMapping("goodwillAndIntangibleAssets"));
        balanceSheetMapping.put("Other Long-Term Assets", new FieldMapping("otherNonCurrentAssets"));
        balanceSheetMapping.put("Total Long-Term Assets", new FieldMapping("totalNonCurrentAssets"));
        balanceSheetMapping.put("Total Assets", new FieldMapping("totalAssets"));
        balanceSheetMapping.put("Total Current Liabilities", new FieldMapping("totalCurrentLiabilities"));
        balanceSheetMapping.put("Long Term Debt", new FieldMapping("longTermDebt"));
        balanceSheetMapping.put("Other Non-Current Liabilities", new FieldMapping("otherNonCurrentLiabilities"));
        balanceSheetMapping.put("Total Long Term Liabilities", new FieldMapping("totalNonCurrentLiabilities"));
        balanceSheetMapping.put("Total Liabilities", new FieldMapping("totalLiabilities"));
        balanceSheetMapping.put("Common Stock Net", new FieldMapping("commonStock"));
        balanceSheetMapping.put("Retained Earnings (Accumulated Deficit)", new FieldMapping("retainedEarnings"));
        balanceSheetMapping.put("Comprehensive Income", new FieldMapping("accumulatedOtherComprehensiveIncomeLoss"));
        balanceSheetMapping.put("Other Share Holders Equity", new FieldMapping("othertotalStockholdersEquity"));
        balanceSheetMapping.put("Share Holder Equity", new FieldMapping("totalStockholdersEquity"));
        balanceSheetMapping.put("Total Liabilities And Share Holders Equity", new FieldMapping("totalLiabilitiesAndTotalEquity"));
        balanceSheetMapping.put("Cash On Hand", new FieldMapping("cashAndCashEquivalents"));

        cashFlowStatementMapping.put("Net Income/Loss", new FieldMapping("netIncome"));
        cashFlowStatementMapping.put("Total Depreciation And Amortization - Cash Flow", new FieldMapping("depreciationAndAmortization"));
        //        cashFlowStatementMapping.put("Other Non-Cash Items", new FieldMapping("otherNonCashItems"));
        cashFlowStatementMapping.put("Total Non-Cash Items", new FieldMapping("otherNonCashItems")); // No perfect match, but this is the most logical choice for a total.
        cashFlowStatementMapping.put("Change In Accounts Receivable", new FieldMapping("accountsReceivables"));
        cashFlowStatementMapping.put("Change In Inventories", new FieldMapping("inventory"));
        cashFlowStatementMapping.put("Change In Accounts Payable", new FieldMapping("accountsPayables"));
        cashFlowStatementMapping.put("Change In Assets/Liabilities", new FieldMapping("otherWorkingCapital")); // This is the closest match for general changes
        cashFlowStatementMapping.put("Total Change In Assets/Liabilities", new FieldMapping("changeInWorkingCapital"));
        cashFlowStatementMapping.put("Cash Flow From Operating Activities", new FieldMapping("netCashProvidedByOperatingActivities"));
        cashFlowStatementMapping.put("Net Change In Property, Plant, And Equipment", new FieldMapping("investmentsInPropertyPlantAndEquipment"));
        //        cashFlowStatementMapping.put("Net Change In Intangible Assets", new FieldMapping("acquisitionsNet")); // No perfect match, but acquisitions often include intangible assets.
        cashFlowStatementMapping.put("Net Acquisitions/Divestitures", new FieldMapping("acquisitionsNet"));
        cashFlowStatementMapping.put("Net Change In Short-term Investments", new FieldMapping("purchasesOfInvestments")); // Closest match, as changes are a result of purchases/sales.
        cashFlowStatementMapping.put("Net Change In Long-Term Investments", new FieldMapping("salesMaturitiesOfInvestments")); // Closest match, as changes are a result of purchases/sales.
        cashFlowStatementMapping.put("Net Change In Investments - Total", new FieldMapping("otherInvestingActivites")); // No specific field for a total change in investments, so other investing activities is a reasonable catch-all.
        cashFlowStatementMapping.put("Investing Activities - Other", new FieldMapping("otherInvestingActivites"));
        cashFlowStatementMapping.put("Cash Flow From Investing Activities", new FieldMapping("netCashUsedForInvestingActivites"));
        cashFlowStatementMapping.put("Net Long-Term Debt", new FieldMapping("debtRepayment")); // Net is debt issued minus debt repaid, but debtRepayment is the most specific field.
        cashFlowStatementMapping.put("Net Current Debt", new FieldMapping("debtRepayment")); // Same as above, no specific field for short-term debt changes.
        cashFlowStatementMapping.put("Debt Issuance/Retirement Net - Total", new FieldMapping("debtRepayment"));
        //        cashFlowStatementMapping.put("Net Common Equity Issued/Repurchased", new FieldMapping("commonStockIssued")); // Net change is a combination, but this is the closest single field.
        cashFlowStatementMapping.put("Net Total Equity Issued/Repurchased", new FieldMapping("commonStockRepurchased"));
        cashFlowStatementMapping.put("Total Common And Preferred Stock Dividends Paid", new FieldMapping("dividendsPaid"));
        cashFlowStatementMapping.put("Financial Activities - Other", new FieldMapping("otherFinancingActivites"));
        cashFlowStatementMapping.put("Cash Flow From Financial Activities", new FieldMapping("netCashUsedProvidedByFinancingActivities"));
        cashFlowStatementMapping.put("Net Cash Flow", new FieldMapping("netChangeInCash"));
        cashFlowStatementMapping.put("Stock-Based Compensation", new FieldMapping("stockBasedCompensation"));
        cashFlowStatementMapping.put("Common Stock Dividends Paid", new FieldMapping("dividendsPaid"));
    }

    public static void main(String[] args) throws Exception {
        String date = "2025-06-30";
        String symbol = "GOOGL";
        IncomeStatement incomeStatement = readIncomeStatementFull(date, symbol);
        BalanceSheet balanceSheet = readBalanceSheetFull(date, symbol);
        CashFlow cashFlowStatement = readCashFlowStatementFull(date, symbol);

        FinancialsTtm financialsTtm = DataLoader.readFinancials("GOOGL").financials.get(0);
        IncomeStatement realIncomeStatement = financialsTtm.incomeStatement;

        for (var field : IncomeStatement.class.getFields()) {
            Object val1 = field.get(incomeStatement);
            Object val2 = field.get(realIncomeStatement);
            System.out.println(field.getName() + "\n" + val1 + "\n" + val2 + "\n\n");
        }

        System.out.println();
        System.out.println("--------------------");
        System.out.println();

        BalanceSheet realBalanceSheet = financialsTtm.balanceSheet;

        for (var field : BalanceSheet.class.getFields()) {
            Object val1 = field.get(balanceSheet);
            Object val2 = field.get(realBalanceSheet);
            System.out.println(field.getName() + "\n" + val1 + "\n" + val2 + "\n\n");
        }

        System.out.println();
        System.out.println("--------------------");
        System.out.println();

        CashFlow realCashFlow = financialsTtm.cashFlow;

        for (var field : CashFlow.class.getFields()) {
            Object val1 = field.get(cashFlowStatement);
            Object val2 = field.get(realCashFlow);
            System.out.println(field.getName() + "\n" + val1 + "\n" + val2 + "\n\n");
        }
    }

    public static IncomeStatement readIncomeStatementFull(String date, String symbol) throws IOException, FileNotFoundException, JsonProcessingException, JsonMappingException, Exception {
        long multiplier = 1_000_000;
        String currency = "USD";

        String fileName = "/tmp/income-statement-" + symbol + ".json";
        String fileContent = "";
        File file = new File(fileName);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                fileContent = new String(fis.readAllBytes());
            }
        } else {
            String newUri = getBaseUrl(symbol);
            fileContent = restTemplate.getForObject(newUri + "income-statement?freq=Q", String.class);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileContent.getBytes());
            }
        }

        List<Map<String, String>> dataDeserialized = parseOutData(fileContent);

        IncomeStatement incomeStatement = readIncomeStatement(date, dataDeserialized, multiplier);
        return incomeStatement;
    }

    public static BalanceSheet readBalanceSheetFull(String date, String symbol) throws IOException, FileNotFoundException, JsonProcessingException, JsonMappingException, Exception {
        long multiplier = 1_000_000;
        String currency = "USD";

        String fileName = "/tmp/balance-sheet-" + symbol + ".json";
        String fileContent = "";
        File file = new File(fileName);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                fileContent = new String(fis.readAllBytes());
            }
        } else {
            String newUri = getBaseUrl(symbol);
            fileContent = restTemplate.getForObject(newUri + "balance-sheet?freq=Q", String.class);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileContent.getBytes());
            }
        }

        List<Map<String, String>> dataDeserialized = parseOutData(fileContent);

        BalanceSheet balanceSheet = readBalanceSheet(date, dataDeserialized, multiplier);
        return balanceSheet;
    }

    public static CashFlow readCashFlowStatementFull(String date, String symbol) throws IOException, FileNotFoundException, JsonProcessingException, JsonMappingException, Exception {
        long multiplier = 1_000_000;
        String currency = "USD";

        String fileName = "/tmp/cash-flow-statement-" + symbol + ".json";
        String fileContent = "";
        File file = new File(fileName);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                fileContent = new String(fis.readAllBytes());
            }
        } else {
            String newUri = getBaseUrl(symbol);
            fileContent = restTemplate.getForObject(newUri + "cash-flow-statement?freq=Q", String.class);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileContent.getBytes());
            }
        }

        List<Map<String, String>> dataDeserialized = parseOutData(fileContent);

        CashFlow cashFlow = readCashFlowStatement(date, dataDeserialized, multiplier);
        return cashFlow;
    }

    public static String getBaseUrl(String symbol) {
        File cacheFile = new File("/tmp/macrotrends-baseurl-" + symbol);

        if (cacheFile.exists()) {
            try (FileInputStream fis = new FileInputStream(cacheFile)) {
                return new String(fis.readAllBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            ResponseEntity<String> entity = restTemplate.getForEntity("https://www.macrotrends.net/stocks/charts/" + symbol + "/" + symbol + "/income-statement?freq=Q", String.class);

            String newUri = Arrays.stream(entity.getBody().split("\n"))
                    .filter(a -> a.contains("link rel=\"canonical\""))
                    .findFirst()
                    .map(a -> a.replaceAll(".*?https://", "").replaceAll("income-statement.*", ""))
                    .get();
            newUri = "https://" + newUri;

            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                fos.write(newUri.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return newUri;
        }
        throw new RuntimeException("Cannot get baseuri");
    }

    public static List<Map<String, String>> parseOutData(String fileContent) throws JsonProcessingException, JsonMappingException {
        String[] lines = fileContent.split("\n");
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
        return dataDeserialized;
    }

    private static BalanceSheet readBalanceSheet(String dateKey, List<Map<String, String>> dataDeserialized, long multiplier) throws Exception {
        var balanceSheet = new BalanceSheet();
        balanceSheet.date = LocalDate.parse(dateKey);
        balanceSheet.reportedCurrency = "USD";

        for (var element : dataDeserialized) {
            String fieldName = element.get("field_name");
            String data = element.get(dateKey);
            updateData(balanceSheet, balanceSheetMapping, fieldName, data, dateKey, multiplier);
        }

        return balanceSheet;
    }

    private static IncomeStatement readIncomeStatement(String dateKey, List<Map<String, String>> dataDeserialized, long multiplier) throws Exception {
        var incomeStatement = new IncomeStatement();
        incomeStatement.date = LocalDate.parse(dateKey);
        incomeStatement.reportedCurrency = "USD";

        for (var element : dataDeserialized) {
            String fieldName = element.get("field_name");
            String data = element.get(dateKey);
            updateData(incomeStatement, incomeStatementMapping, fieldName, data, dateKey, multiplier);
        }

        return incomeStatement;
    }

    private static CashFlow readCashFlowStatement(String dateKey, List<Map<String, String>> dataDeserialized, long multiplier) throws Exception {
        var cashFlow = new CashFlow();
        cashFlow.date = LocalDate.parse(dateKey);
        cashFlow.reportedCurrency = "USD";

        for (var element : dataDeserialized) {
            String fieldName = element.get("field_name");
            String data = element.get(dateKey);
            updateData(cashFlow, cashFlowStatementMapping, fieldName, data, dateKey, multiplier);
        }

        return cashFlow;
    }

    private static void updateData(Object incomeStatement, Map<String, FieldMapping> mapping, String fieldName, String data, String dateKey, long multiplier) throws Exception {

        for (var entry : mapping.entrySet()) {
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
